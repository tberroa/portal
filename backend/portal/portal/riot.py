import json
import string
from boto.sqs.connection import SQSConnection
from boto.sqs.message import RawMessage
from cassiopeia import baseriotapi
from django.http import HttpResponse
from django.views.decorators.http import require_POST
from portal.errors import INVALID_REQUEST_FORMAT
from portal.keys import AWS_ACCESS_KEY_ID
from portal.keys import AWS_SECRET_ACCESS_KEY
from portal.keys import RIOT_API_KEY

QUEUE = "TEAM_BUILDER_DRAFT_RANKED_5x5"
SEASON = "SEASON2016"
baseriotapi.set_api_key(RIOT_API_KEY)
baseriotapi.set_rate_limits((270, 10), (16200, 600))


def format_key(key):
    # filter to remove punctuation
    translator = str.maketrans({key: None for key in string.punctuation})

    # also remove white spaces and make lowercase
    return key.translate(translator).replace(" ", "").lower()


def riot_request(region, args):
    # set region
    baseriotapi.set_region(region)

    # extract arguments
    request = args.get("request")
    key = args.get("key")
    match_id = args.get("match_id")
    summoner_id = args.get("summoner_id")
    summoner_ids = args.get("summoner_ids")

    # make request
    if request == 1:
        riot_response = baseriotapi.get_summoners_by_name(format_key(key))
    elif request == 2:
        riot_response = baseriotapi.get_match_list(summoner_id, 0, 0, 0, 0, 0, QUEUE, SEASON)
    elif request == 3:
        riot_response = baseriotapi.get_match(match_id)
    elif request == 4:
        riot_response = baseriotapi.get_leagues_by_summoner(summoner_ids)
    elif request == 5:
        riot_response = baseriotapi.get_ranked_stats(summoner_id, SEASON)
    elif request == 6:
        riot_response = baseriotapi.get_summoner_runes(summoner_id)
    else:
        riot_response = None

    # return response
    return riot_response


@require_POST
def update(request):
    # extract data
    data = json.loads(request.body.decode('utf-8'))
    region = data.get("region")
    keys = data.get("keys")

    # ensure the data is valid
    if None in (region, keys):
        return HttpResponse(INVALID_REQUEST_FORMAT)

    # update the summoners
    conn = SQSConnection(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
    queue = conn.get_queue("portal")
    message = RawMessage()
    message.set_body(json.dumps({"region": region, "keys": keys}))
    queue.write(message)

    # successful return
    return HttpResponse("success")
