import boto3, json
from pymongo import MongoClient
from datetime import datetime,timedelta
import argparse
from os import path
import sys
from mongodb_utils import *

PATCH_NAME="001_close_expired_enrollments"

ht_api_secret_name = "dev/ht-api"

parser = argparse.ArgumentParser(description='Close all orphan enrollments')
parser.add_argument('--secret_name', dest='ht_api_secret_name',
                   default=ht_api_secret_name,
                    help='AWS HT API secret name, defaults to ' + ht_api_secret_name)
parser.add_argument('--profile_name', dest='profile_name',
                   default="default",
                   help='If there are multiple environvents in .aws/credentials set the desired profile name')
parser.add_argument('--ca_bundle', dest='ca_bundle_path',
                   default="/Users/ayusov/Work/secret/rds-combined-ca-bundle.pem",
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

# Note.  Borrowed from restart_mn.py. Should be moved to common module.
def get_start_date(el):
    if "reminderStartDate" in el:
        return el["reminderStartDate"] if el["reminderStartDate"] else el["txStartDate"]
    else:
        return el["txStartDate"]

def is_expired(e):
    sd = get_start_date(e)
    c = int(e["cycles"])
    dc = int(e["daysInCycle"])
    return sd + timedelta(days=c*dc) < datetime.now()

print("Querying mongoDB", mongo_db, "database")

db = mc[mongo_db]
enr = db["enrollments"]
ee = [e for e in enr.find({"status" : "ACTIVE", "cycles":{"$gt":0}})]
print("There are", len(ee), "ACTIVE enrollments")
eexp = [e["_id"] for e in ee if is_expired(e)]
print("There are", len(eexp), "expired ACTIVE enrollments")

modified = 0
for eid in eexp:
    if args.run:
        print("Setting to COMPLETED", eid)
        upd = enr.update_one({"_id":eid}, {"$set":{"status":"COMPLETED"}})
        modified = modified + upd.modified_count 
    else:
        print("Dry run, not changing", eid)

if args.run:
    log_db_patch(db, PATCH_NAME, """Modified %d enrollment records out of %d expired enrollment records.
Total number of ACTIVE enrollments in the database is %d
""" % (modified, len(eexp), len(ee)))

