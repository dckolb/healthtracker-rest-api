import boto3, json
from pymongo import MongoClient
import argparse
from os import path
import sys
from mongodb_utils import *
import pandas as pd

ht_api_secret_name = "dev/ht-api"

parser = argparse.ArgumentParser(description="Nine o'clock checkins report")
common_cli_args(parser)

parser.add_argument('--clinic_id', dest='clinic_id',
                   default=12,
                   help='Clinic ID')

args = parser.parse_args()
ht_api_secret_name = args.ht_api_secret_name
aws_profile = args.profile_name
ca_bundle_path = args.ca_bundle_path
clinic_id = args.clinic_id

print("Using AWS profile", aws_profile, "and HT API key", ht_api_secret_name, ", CA bundle path", ca_bundle_path)

if not path.exists(ca_bundle_path):
    print("CA bundle file", ca_bundle_path, "not found")
    sys.exit(1)

# Some clinic ID:
#  MNOC  "Minnesota Oncology"           291
#  RMCC  "Rocky Mountain Cancer Center" 279
#  VOA   "Virginia Oncology Associates" 307

if clinic_id.isdigit():
    clinic_id = int(clinic_id)
    print("Running report for clinic_id", clinic_id)
else:
    print("clinic_id must be integer:", clinic_id)

# Provider and location collected with:
#echo 'select clinic_id,id,name from clinic_individual_provider_ties' | mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD -D $DB_NAME -A -B > clinic_provider_mappings.tsv
#echo 'select clinic_id, id, label from clinic_locations' | mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD -D $DB_NAME -A -B > clinic_locations.tsv
providers_file = "clinic_provider_mappings.tsv"
locations_file = "clinic_locations.tsv"

if not path.exists(providers_file):
    print("Provider names file", providers_file, "not found")
    sys.exit(1)

if not path.exists(locations_file):
    print("Location names file", locations_file, "not found")
    sys.exit(1)

providers_df = pd.read_table(providers_file)
locations_df = pd.read_table(locations_file)
    
mongo_uri, mongo_db = get_db_name(ht_api_secret_name, aws_profile, ca_bundle_path)

print("Opening session with mongoDB", mongo_uri)

mc = MongoClient(mongo_uri, ssl=True, ssl_ca_certs=ca_bundle_path)

print("Querying mongoDB", mongo_db, "database")

db = mc[mongo_db]
enr = db["enrollments"]

reminder_at_9 = {"$match":{"reminderTime":"09:00","status":"ACTIVE","clinicId":int(clinic_id)}}
#reminder_at_9 = {"$match":{"reminderTime":"09:00","status":"ACTIVE"}}
join_stat = {"$lookup":{"from":"ht_status", "localField":"_id", "foreignField":"_id", "as":"ht_status"}}
flatten_stat = {"$unwind":"$ht_status"}
rep_fields = {"$project":{"_id":0,
#                          "clinicId":1,
                          "mrn":"$ht_status.patientInfo.mrn",
                          "firstName":"$ht_status.patientInfo.firstName",
                          "lastName":"$ht_status.patientInfo.lastName",
                          "locationId":1,
                          "providerId":1,
                          "enrollmentDate":"$createdDate",
                          "startDate":"$txStartDate"
}}

ee = enr.aggregate([reminder_at_9, join_stat, flatten_stat, rep_fields])
res_df = pd.DataFrame()
for e in ee:
    if "providerId" in e:
        pf = (providers_df.clinic_id == clinic_id) & (providers_df.id == int(e["providerId"]))
        if any(pf):
            e["Provider"] = providers_df[pf]["name"].values[0]
        del e["providerId"]
    if "locationId" in e:
        lf = (locations_df.clinic_id == clinic_id) & (locations_df.id == int(e["locationId"]))
        if any(lf):
            e["Location"] = locations_df[lf]["label"].values[0]
        del e["locationId"]
    res_df = res_df.append(e, ignore_index=True)
#    print(e)

# print(res_df)
res_df.to_csv('report_'+str(clinic_id)+'.csv', index=False) 

print("Total number of enrollments with 9 o'clock reminders", res_df.shape[0])


