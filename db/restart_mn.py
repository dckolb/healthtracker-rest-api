import boto3, json
from pymongo import MongoClient, DESCENDING
from datetime import datetime,timedelta
import argparse
from os import path
import math
import sys
from mongodb_utils import *

PATCH_NAME="004_restart_next_cycle"

ht_api_secret_name = "dev/ht-api"

parser = argparse.ArgumentParser(description='restart auto-ended cycles')
parser.add_argument('--secret_name', dest='ht_api_secret_name',
                    default=ht_api_secret_name,
                    help='AWS HT API secret name, defaults to ' + ht_api_secret_name)
parser.add_argument('--profile_name', dest='profile_name',
                    default="default",
                    help='If there are multiple environvents in .aws/credentials set the desired profile name')
parser.add_argument('--ca_bundle', dest='ca_bundle_path',
                    default="/Users/rmarckel/rds-certs/2019/rds-combined-ca-bundle.pem",
                    help='CA bundle file path')
parser.add_argument('--run', dest='run',
                    default=False, const=True, type=bool, nargs="?",
                    help='Say --run if you want to make DB changes, otherwise no changes will be made')

args = parser.parse_args()
ht_api_secret_name = args.ht_api_secret_name
aws_profile = args.profile_name
ca_bundle_path = args.ca_bundle_path

print("Using AWS profile", aws_profile, "and HT API key", ht_api_secret_name, ", CA bundle path", ca_bundle_path)
if args.run:
    print("Making actual DB changes")
else:
    print("This is a dry run")

if not path.exists(ca_bundle_path):
    print("CA bundle file", ca_bundle_path, "not found")
    sys.exit(1)

mongo_uri, mongo_db = get_db_name(ht_api_secret_name, aws_profile, ca_bundle_path)

print("Opening session with mongoDB", mongo_uri)

mc = MongoClient(mongo_uri, ssl=True, ssl_ca_certs=ca_bundle_path)

def get_start_date(el):
    if "reminderStartDate" in el:
        return el["reminderStartDate"] if el["reminderStartDate"] else el["txStartDate"]
    else:
        return el["txStartDate"]

def cycle_number_for_date(start_date, cycle_length, date_to_check):
    return math.ceil((date_to_check - start_date).days/cycle_length)

def is_different_cycle(en, checkin):
    dc = int(en["daysInCycle"])
    scheduleDate = checkin["scheduleDate"]
    return cycle_number_for_date(get_start_date(en), dc, datetime.now()) > cycle_number_for_date(get_start_date(en), dc, scheduleDate)


print("Querying mongoDB", mongo_db, "database")

db = mc[mongo_db]
ht_status = db["ht_status"]
htStatuses = [e for e in ht_status.find({ "endCurrentCycle": True})]
print("There are", len(htStatuses), "auto ended status")

#eeids = [e["_id"] for e in htStatuses]

modified = 0
enr = db["enrollments"]
ci = db["checkins"]
actions = []
for hts in htStatuses:
    #print("get enrollment")
    firstName = hts["patientInfo"]["firstName"]
    lastName = hts["patientInfo"]["lastName"]

    eid = hts["_id"]
    enrollment = enr.find_one({"_id": eid})
    patientId = enrollment["patientId"]
    clinicId = enrollment["clinicId"]
    eStatus = enrollment["status"]
    lastCheckInDate = None
    migrated = False
    if enrollment["status"] == "ACTIVE":
        print("get last  symptom checkin for id ", eid)
        #print("enrollment is ", enrollment[0])
        lastCheckIns = [c for c in ci.find({"enrollmentId": str(eid), "checkInType": "SYMPTOM"}).\
            sort("scheduleDate", DESCENDING).\
            limit(1)]
        if len(lastCheckIns) == 0:
            print("no checkins for ", eid)
            continue
        lastCheckInDate = lastCheckIns[0]["scheduleDate"]
        #print("enrollment id", eid, "found last symptom checkin", lastCheckIns[0]["scheduleDate"])
        if is_different_cycle(enrollment, lastCheckIns[0]):
            print("will update status for", eid, "and patient ID", enrollment["patientId"], )
            modified = modified + 1
            migrated = True
            if args.run:
                print("turning off endCurrentCycle for", eid)
                upd = ht_status.update_one({"_id": eid}, {"$set":{"endCurrentCycle": False}})
        else :
            print("skipping update for enrollment", eid)
    else:
        print("enrollment", eid, "is not active", enrollment["status"])
    actions.append("%(eid)s,%(clinicId)s,%(patientId)s,%(firstName)s,%(lastName)s,%(eStatus)s,%(lastCheckInDate)s,%(migrated)s" % locals())


if args.run:
    log_db_patch(db, PATCH_NAME, """Modified %d htstatus records.
    """ % (modified))

print(modified, "ht status records ")

print("id,clinic_id,patient_id,first,last,status,last_check_in,updated")
for a in actions:
    print(a)


