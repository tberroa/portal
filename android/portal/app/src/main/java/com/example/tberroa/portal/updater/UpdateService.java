package com.example.tberroa.portal.updater;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.example.tberroa.portal.data.LocalDB;
import com.example.tberroa.portal.models.matchlist.MatchReference;
import com.example.tberroa.portal.data.Params;
import com.example.tberroa.portal.data.RiotAPI;
import com.example.tberroa.portal.models.match.MatchDetail;
import com.example.tberroa.portal.models.matchlist.MatchList;
import com.example.tberroa.portal.models.summoner.SummonerDto;
import com.example.tberroa.portal.network.NetworkUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class UpdateService extends Service {

    /*  2 states:
        0, service is ready to run.
        1, service is taking a break. (state is switched to 0 at end of break) */

    private final UpdateJobInfo updateJobInfo = new UpdateJobInfo();
    private RiotAPI riotAPI;
    private boolean kill = false;
    private Timer timer;

    @Override
    public void onCreate() {
        // initialize riot api
        riotAPI = new RiotAPI(this);
        // initialize timer
        timer = new Timer();

        // initialize timer task to be done periodically
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // make timer task run in background via new thread
                new Thread(new Runnable() {
                    public void run() {
                        updateJob();
                    }
                }).start();
            }
        };

        // after an immediate initial run, the task will run every 10 minutes
        timer.schedule(timerTask, 0, 1000 * 60 * 5);
    }

    private void updateJob() {
        int condition = checkConditions();

        switch (condition) {
            case 100: // code 100, internet is not available
                Log.d(Params.TAG_DEBUG, "@UpdateService: network not available. update job not able to run");
                break;
            case 200: // code 200, update job is already running
                Log.d(Params.TAG_DEBUG, "@UpdateService: update job is already running");
                break;
            case 300: // code 300, update job is good to go
                // let the system know the update job is running
                updateJobInfo.setRunning(this, true);
                Log.d(Params.TAG_DEBUG, "@UpdateService: update job is running");

                if (kill) {
                    updateJobInfo.setRunning(this, false);
                    Log.d(Params.TAG_DEBUG, "@UpdateService: update job was killed");
                    return;
                }

                // get player profiles map
                Map<String, PlayerUpdateProfile> profilesMap = updateJobInfo.getProfiles(this);

                if (kill) {
                    updateJobInfo.setRunning(this, false);
                    Log.d(Params.TAG_DEBUG, "@UpdateService: update job was killed");
                    return;
                }

                // save any new summoners
                saveNewSummoners(profilesMap);

                if (kill) {
                    updateJobInfo.setRunning(this, false);
                    Log.d(Params.TAG_DEBUG, "@UpdateService: update job was killed");
                    return;
                }

                // get any new matches
                List<List<MatchReference>> newMatchesByQueue = getNewMatchReferences(profilesMap);

                if (kill) {
                    updateJobInfo.setRunning(this, false);
                    Log.d(Params.TAG_DEBUG, "@UpdateService: update job was killed");
                    return;
                }

                // save the details of any new matches, update job finishes within the saveNewMatchDetails method
                saveNewMatchDetails(newMatchesByQueue);
                break;
        }
    }

    private void saveNewSummoners(Map<String, PlayerUpdateProfile> profilesMap) {
        // initialize list to store summoner names
        List<String> summonerNames = new ArrayList<>();

        // look through the profile map for new players
        for (Map.Entry<String, PlayerUpdateProfile> profile : profilesMap.entrySet()) {

            if (kill) return;

            if (profile.getValue().newPlayer) {
                // add the name of any new players to the list of names to query for
                summonerNames.add(profile.getKey());

                // update their status
                profile.getValue().newPlayer = false;
            }
        }
        Log.d(Params.TAG_DEBUG, "@UpdateService: number of new players is " + Integer.toString(summonerNames.size()));

        if (kill) return;

        // if new players were found, get and save their summoner dto then save the profile map
        if (!summonerNames.isEmpty()) {

            if (kill) return;

            Map<String, SummonerDto> summoners = riotAPI.getSummonersByName(summonerNames);
            if (summoners != null) {
                for (Map.Entry<String, SummonerDto> summoner : summoners.entrySet()) {

                    if (kill) return;

                    summoner.getValue().save();
                }
            }
            new UpdateJobInfo().setProfiles(this, profilesMap);
        }
    }

    private List<List<MatchReference>> getNewMatchReferences(Map<String, PlayerUpdateProfile> profilesMap) {
        // initialize return value
        List<List<MatchReference>> returnedMatchesByQueue = new ArrayList<>();
        returnedMatchesByQueue.add(new ArrayList<MatchReference>()); // dynamic queue
        returnedMatchesByQueue.add(new ArrayList<MatchReference>()); // solo queue
        returnedMatchesByQueue.add(new ArrayList<MatchReference>()); // 5's
        returnedMatchesByQueue.add(new ArrayList<MatchReference>()); // 3's

        // get summoner dto's
        List<SummonerDto> summoners = new ArrayList<>();
        LocalDB localDB = new LocalDB();
        for (Map.Entry<String, PlayerUpdateProfile> entry : profilesMap.entrySet()) {

            if (kill) return null;

            summoners.add(localDB.getSummonerByName(entry.getKey()));
        }
        Log.d(Params.TAG_DEBUG, "@UpdateService: size of summoners is " + Integer.toString(summoners.size()));

        // initialize search parameters
        Map<String, String> parameters = new HashMap<>();
        parameters.put("seasons", Params.SEASON_2016);

        // for each summoner, look for new matches
        for (SummonerDto summoner : summoners) {

            if (kill) return null;

            // query riot api for the new match list
            MatchList newMatchList = riotAPI.getMatchList(summoner.id, parameters);

            if (kill) return null;

            // load up the old match list
            MatchList oldMatchList = localDB.getMatchList(summoner.id);

            if (newMatchList != null && (oldMatchList == null || oldMatchList.totalGames < newMatchList.totalGames)) {

                if (kill) return null;

                // get the amount of new games
                int amountOfNewGames;
                if (oldMatchList == null) {
                    amountOfNewGames = newMatchList.endIndex;
                } else {
                    amountOfNewGames = newMatchList.totalGames - oldMatchList.totalGames;
                }

                if (kill) return null;

                // get the list of new matches
                List<MatchReference> newMatches = newMatchList.matches.subList(0, amountOfNewGames);

                if (kill) return null;

                // parse the matches and separate by queue type
                List<List<MatchReference>> newMatchesByQueue = divideMatches(newMatches);

                // trim each list to only hold the most recent matches
                for (int i = 0; i < newMatchesByQueue.size(); i++) {
                    if (kill) return null;

                    List<MatchReference> trimmedList = onlyMostRecent(newMatchesByQueue.get(i));
                    newMatchesByQueue.set(i, trimmedList);
                }

                if (kill) return null;

                // include the new match references in the returned value
                for (int i = 0; i < newMatchesByQueue.size(); i++) {
                    if (kill) return null;

                    returnedMatchesByQueue.get(i).addAll(newMatchesByQueue.get(i));
                }

                if (kill) return null;

                // save the new match list
                newMatchList.summonerId = summoner.id;
                newMatchList.cascadeSave();
            }
        }

        return returnedMatchesByQueue;
    }

    private void saveNewMatchDetails(final List<List<MatchReference>> newMatches) {
        // create new thread to handle getting the match detail. this thread utilizes sleep
        new Thread(new Runnable() {
            public void run() {
                // sleep for 5 seconds before beginning in case user has many friends to getMatchList for
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Log.d(Params.TAG_EXCEPTIONS, e.getMessage());
                }

                for (List<MatchReference> matches : newMatches) {
                    for (MatchReference match : matches) {

                        // check if this service was killed before every request
                        if (kill) {
                            updateJobInfo.setRunning(UpdateService.this, false);
                            Log.d(Params.TAG_DEBUG, "@UpdateService: update job was killed");
                            return;
                        }

                        // query the riot api for the match details
                        MatchDetail matchDetail = riotAPI.getMatchDetail(match.matchId);
                        if (matchDetail != null) {
                            matchDetail.cascadeSave();
                        }

                        // sleep for 1 second before next request
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Log.d(Params.TAG_EXCEPTIONS, e.getMessage());
                        }

                    }
                }

                // let the system know the update job is done running
                updateJobInfo.setRunning(UpdateService.this, false);
                sendBroadcast(new Intent().setAction(Params.UPDATE_COMPLETE));
                Log.d(Params.TAG_DEBUG, "@UpdateService: update job is done running");
            }
        }).start();
    }

    private int checkConditions() {

        // code 100, internet is not available
        if (!NetworkUtil.isInternetAvailable(this)) {
            return 100;
        }

        // code 200, update job is already running
        if (updateJobInfo.isRunning(this)) {
            return 200;
        }

        // code 300, update job is good to go
        return 300;
    }

    private List<List<MatchReference>> divideMatches(List<MatchReference> matches) {
        // initialize return value
        List<List<MatchReference>> matchesByQueue = new ArrayList<>();
        matchesByQueue.add(new ArrayList<MatchReference>()); // dynamic queue
        matchesByQueue.add(new ArrayList<MatchReference>()); // solo queue
        matchesByQueue.add(new ArrayList<MatchReference>()); // 3's

        // begin parsing
        for (MatchReference match : matches) {
            switch (match.queue) {
                case Params.DYNAMIC_QUEUE:
                    matchesByQueue.get(0).add(match);
                    break;
                case Params.SOLO_QUEUE:
                    matchesByQueue.get(1).add(match);
                    break;
                case Params.TEAM_3:
                    matchesByQueue.get(3).add(match);
                    break;
            }
        }

        return matchesByQueue;
    }

    private List<MatchReference> onlyMostRecent(List<MatchReference> matches) {
        if (matches.size() > Params.MAX_MATCHES) {
            return matches.subList(0, Params.MAX_MATCHES);
        } else {
            return matches;
        }
    }

    @Override
    public void onDestroy() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        kill = true;
        updateJobInfo.setRunning(this, false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}