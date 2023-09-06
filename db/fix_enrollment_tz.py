import requests, argparse
import sys

parser = argparse.ArgumentParser(description='Update enrollment notification reminder timezone')
parser.add_argument('--ht_api_url', dest='ht_url',
                   default="http://ht-api/ht-api/",
                    help='HT REST API Url')
parser.add_argument('--enrollment_id', dest='eid',
                   help='Enrollment ID to update')
parser.add_argument('--auth_token', dest='auth_token',
                   help='Auth token if needed')
parser.add_argument('--tz', dest='tz',
                   default="America/Chicago",
                   help='New timezone to use')

args = parser.parse_args()

auth_header = {"Authorization":"Bearer "+args.auth_token} if args.auth_token != None else {}

def get_enrollment(ht_url, eid):
    return requests.get(ht_url+"/enrollments/" + str(eid), headers=auth_header)

def update_enrollment(ht_url, eid, e):
    return requests.put(ht_url+"/enrollments/" + str(eid), json=e, headers=auth_header)

print("Getting enrollment", args.eid, "from", args.ht_url)
r1 = get_enrollment(args.ht_url, args.eid)

if r1.status_code != 200:
    print("Error getting enrollment:", r1.text)
    sys.exit(1)

e = r1.json()
# Drop evaluated JSON fields, these can not be posted
for prop in ["currentCycleNumber", "nextCycleStartDate", "currentCycleStartDate", "startDate"]:
    if prop in e:
        del e[prop]

print("Setting new timezone to", args.tz)
e["reminderTimeZone"] = args.tz
r2 = update_enrollment(args.ht_url, args.eid, e)
if r2.status_code != 200:
    print("Error saving enrollment:", r2.text)
else:
    print("Update call success")
