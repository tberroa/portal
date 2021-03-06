package com.quadrastats.screens.stats.recent;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TabLayout.TabLayoutOnPageChangeListener;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.quadrastats.R;
import com.quadrastats.data.Constants;
import com.quadrastats.data.LocalDB;
import com.quadrastats.data.UserData;
import com.quadrastats.models.datadragon.Champion;
import com.quadrastats.models.stats.MatchStats;
import com.quadrastats.models.summoner.Summoner;
import com.quadrastats.screens.ScreenUtil;
import com.quadrastats.screens.stats.BaseStatsActivity;
import com.quadrastats.screens.stats.CreateLegendPackage;
import com.quadrastats.screens.stats.StatsUtil;
import com.quadrastats.screens.stats.WinRatesDialog;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecentActivity extends BaseStatsActivity implements RecentAsync {

    private int legendIconSide;

    public void displayData(List<MatchStats> matchStatsList) {
        populateActivity(matchStatsList, 0, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent);

        // initialize the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.rg_activity_title);
        toolbar.inflateMenu(R.menu.recent_menu);

        // initialize tab layout with four tabs
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setVisibility(View.GONE);
        for (int i = 0; i < 4; i++) {
            tabLayout.addTab(tabLayout.newTab());
        }

        // set default legend icon dimension
        legendIconSide = ScreenUtil.dpToPx(this, 50);

        // initialize legend
        LinearLayout legendLayout = (LinearLayout) findViewById(R.id.legend_layout);
        legendLayout.setVisibility(View.GONE);

        // set swipe refresh layout listeners
        SwipeRefreshLayout dataSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.data_swipe_layout);
        dataSwipeLayout.setOnRefreshListener(this);
        SwipeRefreshLayout messageSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.message_swipe_layout);
        messageSwipeLayout.setOnRefreshListener(this);

        // initialize view
        ViewInitialization viewInitialization = new ViewInitialization();
        viewInitialization.delegateRecent = this;
        viewInitialization.execute(1);
    }

    @Override
    public void onRefresh() {
        RequestMatchStats requestMatchStats = new RequestMatchStats();
        requestMatchStats.delegateRecent = this;
        requestMatchStats.execute(1);
    }

    private void populateActivity(List<MatchStats> matchStatsList, long championId, String position) {
        // update the toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(new MenuListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                super.onMenuItemClick(item);
                switch (item.getItemId()) {
                    case R.id.filter:
                        new FilterDialog().show();
                        break;
                    case R.id.win_rates:
                        new WinRatesDialog(RecentActivity.this, matchStatsList, null, staticRiotData).show();
                        break;
                }
                return true;
            }
        });

        // Before the data can be presented, it needs to be organized. All the data will be put into one map object.
        // The map key is the summoner name and the value is a list of of lists where each list is a list of data
        // points corresponding to one stat chart.
        // Example: Key: Frosiph | Value: list[0] = List<csAtTen>, list[1] = List<csDiffAtTen>, etc.

        // create list of chart titles
        ArrayList<String> titles = new ArrayList<>();
        titles.add(getResources().getString(R.string.rg_cs_at_ten));
        titles.add(getResources().getString(R.string.rg_cs_diff_at_ten));
        titles.add(getResources().getString(R.string.rg_cs_per_min));
        titles.add(getResources().getString(R.string.rg_gold_per_min));
        titles.add(getResources().getString(R.string.rg_first_blood));
        titles.add(getResources().getString(R.string.rg_kills));
        titles.add(getResources().getString(R.string.rg_dmg_per_min));
        titles.add(getResources().getString(R.string.rg_killing_spree));
        titles.add(getResources().getString(R.string.rg_multi_kills));
        titles.add(getResources().getString(R.string.rg_first_tower));
        titles.add(getResources().getString(R.string.rg_assists));
        titles.add(getResources().getString(R.string.rg_kda));
        titles.add(getResources().getString(R.string.rg_kill_participation));
        titles.add(getResources().getString(R.string.rg_dmg_taken_per_death));
        titles.add(getResources().getString(R.string.rg_wards_bought));
        titles.add(getResources().getString(R.string.rg_wards_placed));
        titles.add(getResources().getString(R.string.rg_wards_killed));

        // clear set of summoner names
        Set<String> names = new LinkedHashSet<>();

        // clear the map which holds the data for each summoner
        Map<String, List<List<Number>>> aggregateData = new LinkedHashMap<>();

        // populate the map
        for (MatchStats matchStats : matchStatsList) {
            String summoner = matchStats.summoner_name;
            names.add(summoner);

            // initialize the stat lists
            List<List<Number>> summonerData = aggregateData.get(summoner);
            if (summonerData == null) {
                summonerData = new ArrayList<>();
                for (int i = 0; i < titles.size(); i++) {
                    summonerData.add(new ArrayList<>());
                }
            }

            if (matchStats.cs_at_ten != null) {
                summonerData.get(0).add(matchStats.cs_at_ten);
            }
            if (matchStats.cs_diff_at_ten != null) {
                summonerData.get(1).add(matchStats.cs_diff_at_ten);
            }
            if (matchStats.cs_per_min != null) {
                summonerData.get(2).add(matchStats.cs_per_min);
            }
            if (matchStats.gold_per_min != null) {
                summonerData.get(3).add(matchStats.gold_per_min);
            }
            if ((matchStats.first_blood_assist != null) && (matchStats.first_blood_kill != null)) {
                if (matchStats.first_blood_assist || matchStats.first_blood_kill) {
                    summonerData.get(4).add(100);
                } else {
                    summonerData.get(4).add(0);
                }
            }
            if (matchStats.kills != null) {
                summonerData.get(5).add(matchStats.kills);
            }
            if (matchStats.dmg_per_min != null) {
                summonerData.get(6).add(matchStats.dmg_per_min);
            }
            if (matchStats.largest_killing_spree != null) {
                summonerData.get(7).add(matchStats.largest_killing_spree);
            }
            if (matchStats.largest_multi_kill != null) {
                summonerData.get(8).add(matchStats.largest_multi_kill);
            }
            if ((matchStats.first_tower_assist != null) && (matchStats.first_tower_kill != null)) {
                if (matchStats.first_tower_assist || matchStats.first_tower_kill) {
                    summonerData.get(9).add(100);
                } else {
                    summonerData.get(9).add(0);
                }
            }
            if (matchStats.assists != null) {
                summonerData.get(10).add(matchStats.assists);
            }
            if (matchStats.kda != null) {
                summonerData.get(11).add(matchStats.kda);
            }
            if (matchStats.kill_participation != null) {
                summonerData.get(12).add(matchStats.kill_participation);
            }
            if ((matchStats.total_damage_taken != null) && (matchStats.deaths != null)) {
                if (matchStats.deaths != 0) {
                    summonerData.get(13).add(matchStats.total_damage_taken / matchStats.deaths);
                } else {
                    summonerData.get(13).add(matchStats.total_damage_taken);
                }
            }
            if (matchStats.vision_wards_bought_in_game != null) {
                summonerData.get(14).add(matchStats.vision_wards_bought_in_game);
            }
            if (matchStats.wards_placed != null) {
                summonerData.get(15).add(matchStats.wards_placed);
            }
            if (matchStats.wards_killed != null) {
                summonerData.get(16).add(matchStats.wards_killed);
            }

            aggregateData.put(summoner, summonerData);
        }

        // create the legend
        LinearLayout legendLayout = (LinearLayout) findViewById(R.id.legend_layout);
        CreateLegendPackage createLegendPackage = new CreateLegendPackage();
        createLegendPackage.championId = championId;
        createLegendPackage.context = this;
        createLegendPackage.iconSide = legendIconSide;
        createLegendPackage.names = names;
        createLegendPackage.position = position;
        createLegendPackage.staticRiotData = staticRiotData;
        createLegendPackage.view = legendLayout;
        createLegendPackage.viewWidth = ScreenUtil.screenWidth(this);
        StatsUtil.createLegend(createLegendPackage);

        // display the legend
        legendLayout.setVisibility(View.VISIBLE);
        legendLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // copy the set of summoner names
                Set<String> selectedNames = new LinkedHashSet<>(names);

                // copy the map of data
                Map<String, List<List<Number>>> selectedData = new LinkedHashMap<>(aggregateData);

                // construct package required by dialog
                GoButtonPackageSSD goButtonPackageSSD = new GoButtonPackageSSD();
                goButtonPackageSSD.selectedNames = selectedNames;
                goButtonPackageSSD.selectedData = selectedData;
                goButtonPackageSSD.titles = titles;
                goButtonPackageSSD.championId = championId;
                goButtonPackageSSD.position = position;

                // display dialog
                new SelectSummonersDialog(goButtonPackageSSD).show();
            }
        });

        // update adapter
        updateAdapter(titles, aggregateData);
    }

    private void updateAdapter(ArrayList<String> titles, Map<String, List<List<Number>>> aggregateData) {
        // initialize tab layout and view pager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);

        // set the view pager adapter
        FragmentManager fM = getSupportFragmentManager();
        viewPager.setAdapter(new PageAdapter(fM, tabLayout.getTabCount(), titles, aggregateData));

        // set page change listener so user won't invoke refresh layout while changing views
        viewPager.addOnPageChangeListener(new TabLayoutOnPageChangeListener(tabLayout) {
            @Override
            public void onPageScrollStateChanged(int state) {
                SwipeRefreshLayout dataSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.data_swipe_layout);
                dataSwipeLayout.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
            }
        });

        // integrate tab layout and view pager
        tabLayout.setupWithViewPager(viewPager);

        // set tab icons
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            @SuppressLint("InflateParams")
            View view = getLayoutInflater().inflate(R.layout.header_tab, null);
            switch (i) {
                case 0:
                    if (tab != null) {
                        ImageView icon = (ImageView) view.findViewById(R.id.tab_icon);
                        icon.setImageResource(R.drawable.ic_tab_income);
                        tab.setCustomView(view);
                    }
                    break;
                case 1:
                    if (tab != null) {
                        ImageView icon = (ImageView) view.findViewById(R.id.tab_icon);
                        icon.setImageResource(R.drawable.ic_tab_offense);
                        tab.setCustomView(view);
                    }
                    break;
                case 2:
                    if (tab != null) {
                        ImageView icon = (ImageView) view.findViewById(R.id.tab_icon);
                        icon.setImageResource(R.drawable.ic_tab_utility);
                        tab.setCustomView(view);
                    }
                    break;
                case 3:
                    if (tab != null) {
                        ImageView icon = (ImageView) view.findViewById(R.id.tab_icon);
                        icon.setImageResource(R.drawable.ic_tab_vision);
                        tab.setCustomView(view);
                    }
                    break;
                default:
                    break;
            }
        }
        tabLayout.setVisibility(View.VISIBLE);
    }

    private class ChampionIcon {

        final Champion champion;
        ImageView check;
        boolean isSelected;

        ChampionIcon(Champion champion) {
            this.champion = champion;
            isSelected = false;
        }
    }

    private class FilterAdapter extends Adapter<FilterAdapter.ChampionViewHolder> {

        final List<ChampionIcon> championIcons;
        private final int side;

        FilterAdapter(List<ChampionIcon> championIcons, int side) {
            this.championIcons = championIcons;
            this.side = side;
        }

        @Override
        public int getItemCount() {
            return championIcons.size();
        }

        @Override
        public void onBindViewHolder(ChampionViewHolder viewHolder, int i) {
            // load champion icon into view
            ChampionIcon icon = championIcons.get(i);
            icon.check = viewHolder.champIconCheck;
            String key = icon.champion.key;
            String url = StatsUtil.championIconURL(staticRiotData.version, key);
            Picasso.with(RecentActivity.this).load(url).resize(side, side)
                    .placeholder(R.drawable.ic_placeholder).into(viewHolder.champIconView);

            // set check
            if (icon.isSelected) {
                viewHolder.champIconCheck.setVisibility(View.VISIBLE);
            } else {
                viewHolder.champIconCheck.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public ChampionViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            Context context = viewGroup.getContext();
            View view = LayoutInflater.from(context).inflate(R.layout.element_champion_icon, viewGroup, false);
            return new ChampionViewHolder(view);
        }

        class ChampionViewHolder extends ViewHolder {

            final ImageView champIconCheck;
            final ImageView champIconView;

            ChampionViewHolder(View itemView) {
                super(itemView);

                // initialize views
                champIconView = (ImageView) itemView.findViewById(R.id.champ_icon_view);
                champIconCheck = (ImageView) itemView.findViewById(R.id.champ_icon_check);
                champIconView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (int i = 0; i < championIcons.size(); i++) {
                            if (i != getLayoutPosition()) {
                                championIcons.get(i).isSelected = false;
                                if (championIcons.get(i).check != null) {
                                    championIcons.get(i).check.setVisibility(View.INVISIBLE);
                                }
                            }
                        }
                        ChampionIcon icon = championIcons.get(getLayoutPosition());
                        icon.isSelected = !icon.isSelected;
                        if (icon.isSelected) {
                            champIconCheck.setVisibility(View.VISIBLE);
                        } else {
                            champIconCheck.setVisibility(View.INVISIBLE);
                        }
                    }
                });

                // resize views according to screen
                champIconView.getLayoutParams().width = side;
                champIconView.getLayoutParams().height = side;
                champIconView.setLayoutParams(champIconView.getLayoutParams());
                champIconCheck.getLayoutParams().width = side;
                champIconCheck.getLayoutParams().height = side;
                champIconCheck.setLayoutParams(champIconCheck.getLayoutParams());
            }
        }
    }

    private class FilterDialog extends Dialog {

        FilterDialog() {
            super(RecentActivity.this, R.style.AppTheme_Dialog);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_data_filter_r);
            setCancelable(true);

            // resize dialog
            int dialogWidth = (Constants.UI_DIALOG_WIDTH * ScreenUtil.screenWidth(RecentActivity.this)) / 100;
            int dialogHeight = (Constants.UI_DIALOG_HEIGHT * ScreenUtil.screenHeight(RecentActivity.this)) / 100;
            getWindow().setLayout(dialogWidth, dialogHeight);

            // initialize champion icon dimensions
            int champIconsPerRow = 4;
            int champIconSide = dialogWidth / champIconsPerRow;

            // set legend icon dimensions based off the champion icon dimensions
            legendIconSide = champIconSide / 2;

            // set position icon dimensions based off the champion icon dimensions
            FrameLayout iconLayout = (FrameLayout) findViewById(R.id.pos_icon_layout);
            iconLayout.getLayoutParams().height = (70 * champIconSide) / 100;
            iconLayout.setLayoutParams(iconLayout.getLayoutParams());

            // create the recycler view adapter data set
            List<ChampionIcon> championIcons = new ArrayList<>();
            for (Champion champion : staticRiotData.championsList) {
                championIcons.add(new ChampionIcon(champion));
            }

            // initialize the recycler view
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
            FilterAdapter adapter = new FilterAdapter(championIcons, champIconSide);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new GridLayoutManager(RecentActivity.this, champIconsPerRow));

            // initialize the role checks
            ImageView topCheck = (ImageView) findViewById(R.id.top_check);
            ImageView jungleCheck = (ImageView) findViewById(R.id.jungle_check);
            ImageView midCheck = (ImageView) findViewById(R.id.mid_check);
            ImageView botCheck = (ImageView) findViewById(R.id.bot_check);
            ImageView supportCheck = (ImageView) findViewById(R.id.support_check);
            topCheck.setVisibility(View.INVISIBLE);
            jungleCheck.setVisibility(View.INVISIBLE);
            midCheck.setVisibility(View.INVISIBLE);
            botCheck.setVisibility(View.INVISIBLE);
            supportCheck.setVisibility(View.INVISIBLE);

            // initialize the position images
            ImageView topIcon = (ImageView) findViewById(R.id.top_view);
            ImageView jungleIcon = (ImageView) findViewById(R.id.jungle_view);
            ImageView midIcon = (ImageView) findViewById(R.id.mid_view);
            ImageView botIcon = (ImageView) findViewById(R.id.bot_view);
            ImageView supportIcon = (ImageView) findViewById(R.id.support_view);

            // set listeners to make them behave like radio buttons
            topIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    jungleCheck.setVisibility(View.INVISIBLE);
                    midCheck.setVisibility(View.INVISIBLE);
                    botCheck.setVisibility(View.INVISIBLE);
                    supportCheck.setVisibility(View.INVISIBLE);
                    if (topCheck.getVisibility() == View.INVISIBLE) {
                        topCheck.setVisibility(View.VISIBLE);
                    } else {
                        topCheck.setVisibility(View.INVISIBLE);
                    }
                }
            });
            jungleIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    topCheck.setVisibility(View.INVISIBLE);
                    midCheck.setVisibility(View.INVISIBLE);
                    botCheck.setVisibility(View.INVISIBLE);
                    supportCheck.setVisibility(View.INVISIBLE);
                    if (jungleCheck.getVisibility() == View.INVISIBLE) {
                        jungleCheck.setVisibility(View.VISIBLE);
                    } else {
                        jungleCheck.setVisibility(View.INVISIBLE);
                    }
                }
            });
            midIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    topCheck.setVisibility(View.INVISIBLE);
                    jungleCheck.setVisibility(View.INVISIBLE);
                    botCheck.setVisibility(View.INVISIBLE);
                    supportCheck.setVisibility(View.INVISIBLE);
                    if (midCheck.getVisibility() == View.INVISIBLE) {
                        midCheck.setVisibility(View.VISIBLE);
                    } else {
                        midCheck.setVisibility(View.INVISIBLE);
                    }
                }
            });
            botIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    topCheck.setVisibility(View.INVISIBLE);
                    jungleCheck.setVisibility(View.INVISIBLE);
                    midCheck.setVisibility(View.INVISIBLE);
                    supportCheck.setVisibility(View.INVISIBLE);
                    if (botCheck.getVisibility() == View.INVISIBLE) {
                        botCheck.setVisibility(View.VISIBLE);
                    } else {
                        botCheck.setVisibility(View.INVISIBLE);
                    }
                }
            });
            supportIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    topCheck.setVisibility(View.INVISIBLE);
                    jungleCheck.setVisibility(View.INVISIBLE);
                    midCheck.setVisibility(View.INVISIBLE);
                    botCheck.setVisibility(View.INVISIBLE);
                    if (supportCheck.getVisibility() == View.INVISIBLE) {
                        supportCheck.setVisibility(View.VISIBLE);
                    } else {
                        supportCheck.setVisibility(View.INVISIBLE);
                    }
                }
            });

            // initialize the go button
            Button goButton = (Button) findViewById(R.id.go_button);
            goButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // get the selected champion id
                    long championId = 0;
                    boolean foundSelected = false;
                    for (ChampionIcon icon : adapter.championIcons) {
                        if (icon.isSelected) {
                            if (!foundSelected) {
                                championId = icon.champion.id;
                                foundSelected = true;
                            } else {
                                String message = getString(R.string.err_select_only_one);
                                Toast.makeText(RecentActivity.this, message, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }

                    // get the selected position
                    String lane;
                    String role;
                    if (topCheck.getVisibility() == View.VISIBLE) {
                        lane = "TOP";
                        role = null;
                    } else if (jungleCheck.getVisibility() == View.VISIBLE) {
                        lane = "JUNGLE";
                        role = null;
                    } else if (midCheck.getVisibility() == View.VISIBLE) {
                        lane = "MIDDLE";
                        role = null;
                    } else if (botCheck.getVisibility() == View.VISIBLE) {
                        lane = "BOTTOM";
                        role = "DUO_CARRY";
                    } else if (supportCheck.getVisibility() == View.VISIBLE) {
                        lane = "BOTTOM";
                        role = "DUO_SUPPORT";
                    } else {
                        lane = null;
                        role = null;
                    }

                    // construct the package to send to the async task
                    GoButtonPackageFD goButtonPackage = new GoButtonPackageFD();
                    goButtonPackage.dialog = FilterDialog.this;
                    goButtonPackage.championId = championId;
                    goButtonPackage.lane = lane;
                    goButtonPackage.role = role;

                    // execute the go button function
                    new FilterDialogGoButton().execute(goButtonPackage);
                }
            });
        }
    }

    private class FilterDialogGoButton extends AsyncTask<GoButtonPackageFD, Void, List<MatchStats>> {

        long championId;
        FilterDialog dialog;
        String position;

        @Override
        protected List<MatchStats> doInBackground(GoButtonPackageFD... params) {
            LocalDB localDB = new LocalDB();
            UserData userData = new UserData();

            // extract objects
            dialog = params[0].dialog;
            championId = params[0].championId;
            String lane = params[0].lane;
            String role = params[0].role;

            // determine position
            String top = Constants.POS_TOP;
            String jungle = Constants.POS_JUNGLE;
            String mid = Constants.POS_MID;
            if (params[0].lane != null) {
                if (lane.equals(top) || lane.equals(jungle) || lane.equals(mid)) {
                    position = lane;
                } else {
                    position = role;
                }
            } else {
                position = null;
            }

            // get user
            Summoner user = localDB.summoner(userData.getId(RecentActivity.this));

            // construct list of keys
            List<String> keys = new ArrayList<>(Arrays.asList((user.key + "," + user.friends).split(",")));

            return localDB.matchStatsList(keys, championId, lane, role);
        }

        @Override
        protected void onPostExecute(List<MatchStats> matchStatsList) {
            // display
            if (!matchStatsList.isEmpty()) {
                populateActivity(matchStatsList, championId, position);
                dialog.dismiss();
            } else {
                Toast.makeText(RecentActivity.this, R.string.err_no_data, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class GoButtonPackageFD {

        long championId;
        FilterDialog dialog;
        String lane;
        String role;
    }

    private class GoButtonPackageSSD {

        long championId;
        String position;
        Map<String, List<List<Number>>> selectedData;
        Set<String> selectedNames;
        ArrayList<String> titles;
    }

    private class SSDName {

        final String name;
        boolean isChecked;

        SSDName(String name) {
            this.name = name;
        }
    }

    private class SSDViewAdapter extends Adapter<SSDViewAdapter.SSDViewHolder> {

        final List<SSDName> names;

        SSDViewAdapter(List<SSDName> names) {
            this.names = names;
        }

        @Override
        public int getItemCount() {
            return names.size();
        }

        @Override
        public void onBindViewHolder(SSDViewHolder viewHolder, int i) {
            viewHolder.checkbox.setText(names.get(i).name);
            viewHolder.checkbox.setChecked(names.get(i).isChecked);
        }

        @Override
        public SSDViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            Context context = viewGroup.getContext();
            View v = LayoutInflater.from(context).inflate(R.layout.view_select_summoners, viewGroup, false);
            return new SSDViewHolder(v);
        }

        class SSDViewHolder extends ViewHolder {

            final CheckBox checkbox;

            SSDViewHolder(View itemView) {
                super(itemView);
                checkbox = (CheckBox) itemView.findViewById(R.id.summoner_checkbox);
                checkbox.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        names.get(getAdapterPosition()).isChecked = !names.get(getAdapterPosition()).isChecked;
                    }
                });
            }
        }
    }

    private class SelectSummonersDialog extends Dialog {

        private final long championId;
        private final String position;
        private final Map<String, List<List<Number>>> selectedData;
        private final Set<String> selectedNames;
        private final ArrayList<String> titles;

        SelectSummonersDialog(GoButtonPackageSSD goButtonPackageSSD) {
            super(RecentActivity.this, R.style.AppTheme_Dialog);
            selectedNames = goButtonPackageSSD.selectedNames;
            selectedData = goButtonPackageSSD.selectedData;
            titles = goButtonPackageSSD.titles;
            championId = goButtonPackageSSD.championId;
            position = goButtonPackageSSD.position;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.dialog_select_summoners);
            setCancelable(true);

            // construct list of SSDNames
            List<SSDName> names = new ArrayList<>();
            for (String selectedName : selectedNames) {
                names.add(new SSDName(selectedName));
            }

            // initialize recycler view
            RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
            SSDViewAdapter adapter = new SSDViewAdapter(names);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(RecentActivity.this));

            // initialize the go button
            Button goButton = (Button) findViewById(R.id.go_button);
            goButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // make sure at least one was selected
                    boolean minimumSatisfied = false;
                    for (SSDName name : adapter.names) {
                        if (name.isChecked) {
                            minimumSatisfied = true;
                            break;
                        }
                    }

                    if (minimumSatisfied) {
                        // go through and remove those that were not checked
                        for (SSDName name : adapter.names) {
                            if (!name.isChecked) {
                                selectedNames.remove(name.name);
                                selectedData.remove(name.name);
                            }
                        }

                        // update the views
                        CreateLegendPackage createLegendPackage = new CreateLegendPackage();
                        createLegendPackage.championId = championId;
                        createLegendPackage.context = RecentActivity.this;
                        createLegendPackage.iconSide = legendIconSide;
                        createLegendPackage.names = selectedNames;
                        createLegendPackage.position = position;
                        createLegendPackage.staticRiotData = staticRiotData;
                        createLegendPackage.view = RecentActivity.this.findViewById(R.id.legend_layout);
                        createLegendPackage.viewWidth = ScreenUtil.screenWidth(RecentActivity.this);
                        StatsUtil.createLegend(createLegendPackage);
                        updateAdapter(titles, selectedData);
                        dismiss();
                    } else {
                        Toast.makeText(RecentActivity.this, R.string.err_must_select_one, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
