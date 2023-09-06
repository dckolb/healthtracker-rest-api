import boto3, json
from pymongo import MongoClient, DESCENDING
from datetime import datetime,timedelta
import argparse
from os import path
import math
import sys
from mongodb_utils import *

ht_api_secret_name = "acceptance/ht-api"

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

print("Querying mongoDB", mongo_db, "database")

db = mc[mongo_db]
enr = db["enrollments"]
ci = db["checkins"]
ht_status = db["ht_status"]
ht_events = db["ht_events"]

auto_enrollments_ids = [e["_id"] for e in enr.find({ "clinicId": { '$in': [4,5,6]}, "patientId":{'$lte':39}, "medication": "Test"},{"_id":1})]
   
if args.run:
    total_checkins = 0
    
    x = ci.delete_many({"enrollmentId":{"$in": [str(i) for i in auto_enrollments_ids]}  })
    print(x.deleted_count, " checkins deleted.")
    x = ht_status.delete_many({ "_id": { "$in": auto_enrollments_ids}  })
    print(x.deleted_count, " statuses deleted.")
    x = ht_events.delete_many({"enrollmentId":{"$in": [str(i) for i in auto_enrollments_ids]}  })
    print(x.deleted_count, " events deleted.")
    x = enr.delete_many({ "_id": { "$in": auto_enrollments_ids}  })
    print(x.deleted_count, " enrollments deleted.")
else:
    auto_statuses = [e for e in ht_status.find({ "_id": { "$in": auto_enrollments_ids}  })]
    # get list of checkin ID lists
    auto_checkins  = [c["_id"] for c in ci.find({"enrollmentId":{"$in":[str(i) for i in auto_enrollments_ids]}},{"_id":1})]
    auto_events    = [c["_id"] for c in ht_events.find({"enrollmentId":{"$in":[str(i) for i in auto_enrollments_ids]}},{"_id":1})]

    print("There are", len(auto_enrollments_ids), "auto enrollments")
    print("There are", len(auto_statuses), "auto statuses")
    print("There are", len(auto_checkins), "auto checkins")
    print("There are", len(auto_events), "auto events")

