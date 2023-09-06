import boto3, json
from pymongo import MongoClient
from bson.objectid import ObjectId
from datetime import datetime,timedelta
import argparse
from os import path
import sys
from mongodb_utils import *

PATCH_NAME="002_update_last_patient_check_in_date"

ht_api_secret_name = "dev/ht-api"

parser = argparse.ArgumentParser(description='Update last patient check in date')
parser.add_argument('--secret_name', dest='ht_api_secret_name',
                   default=ht_api_secret_name,
                    help='AWS HT API secret name, defaults to ' + ht_api_secret_name)
parser.add_argument('--profile_name', dest='profile_name',
                   default="default",
                   help='If there are multiple environments in .aws/credentials set the desired profile name')
parser.add_argument('--ca_bundle', dest='ca_bundle_path',
                   default="/Users/akarwande/rds-combined-ca-bundle.pem",
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

print("Querying mongoDB", mongo_db, "database")

db = mc[mongo_db]
enr = db["enrollments"]
ht_status = db["ht_status"]
checkins = db["checkins"]
print("There are", enr.estimated_document_count(), "enrollments")
print("There are", ht_status.estimated_document_count(), "ht_status")
print("There are", checkins.estimated_document_count(), "checkins")

modified = 0
ee_count = 0
ee = checkins.aggregate([
    {"$match": {"status":"COMPLETED"}},
    {"$group":{"_id":"$enrollmentId", "dates":{"$addToSet":"$updatedDate"}}}
    ])
for e in ee:
    ee_count = ee_count + 1
    eid = e["_id"]
    print("for enrollment id", eid, "found", len(e["dates"]), "completed checkins")

    last_date = max(e["dates"])
    update_values = {"lastPatientCheckInAt": last_date}

    if args.run:
        print("Updating HT status =", eid, "lastPatientCheckInAt date to ", last_date)
        upd = ht_status.update_one({"_id":ObjectId(eid)}, {"$set": update_values})
        modified = modified + upd.modified_count
    else:
        print("update values", update_values)
        print("Dry run, not changing", eid, "lastPatientCheckInAt date to", last_date)
        modified += 1

if args.run:
    print(modified, "ht_status updated.")
    log_db_patch(db, PATCH_NAME, """Modified %d ht_status records lastPatientCheckInAt dates.
Total number of ht_status records for updating in the database is %d
""" % (modified, ee_count))
else:
    print("Dry run : ", modified, "ht_status would have been updated.")
