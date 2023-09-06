import boto3, json
from pymongo import MongoClient
from datetime import datetime,timedelta
import argparse
from os import path
import sys
from mongodb_utils import *

PATCH_NAME="005_set_closed_ht_status"

ht_api_secret_name = "dev/ht-api"

parser = argparse.ArgumentParser(description='Set ht_status to closed if not already')
common_cli_args(parser)
parser.add_argument('--only_latest', dest='latest',
                    default=False, const=True, type=bool, nargs="?",
                    help='Choose only enrollments touched in the last 14 days')

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

print("Querying mongoDB", mongo_db, "database")

db = mc[mongo_db]
enr = db["enrollments"]
enr_query = {"status" : {"$in":["COMPLETED","STOPPED"]}}
if args.latest:
    print("Getting only enrollments updated in the last 14 days")
    # See CustomEnrollmentRepositoryImpl.java:187.
    enr_query["updatedDate"] = {"$gt":datetime.today() + timedelta(days=-14)}
ee = [e["_id"] for e in enr.find(enr_query,{"_id":1})]
print("There are", len(ee), "Closed or stopped enrollments")
hts = db["ht_status"]
htids = [h["_id"] for h in hts.find({"_id":{"$in":ee},"category":{"$ne":"COMPLETED"}},{"_id":1})]
print("There are", len(htids), "ht_status records not completed")

modified = 0
for eid in htids:
    ht = hts.find_one({"_id":eid})
    cat = ht["category"] if "category" in ht else "N/A"
    cid = ht["clinicId"] if "clinicId" in ht else "N/A"
    pname = ht["patientInfo"]["firstName"] + " " +ht["patientInfo"]["lastName"] if "patientInfo" in ht else "N/A"
    if args.run:
        print("Setting ht_status to COMPLETED", eid, "from", cat, "clinicId", cid, "name", pname)
        upd = hts.update_one({"_id":eid}, {"$set":{"category":"COMPLETED"}})
        modified = modified + upd.modified_count 
    else:
        print("Dry run, not changing", eid, "category", cat, "clinicId", cid, "name", pname)

if args.run:
    log_db_patch(db, PATCH_NAME, """Modified %d ht_status records out of %d closed enrollments.
""" % (modified, len(htids)))

