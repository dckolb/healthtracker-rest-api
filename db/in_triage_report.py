import boto3, json
from pymongo import MongoClient, ASCENDING
import argparse
from os import path
import sys, time
from mongodb_utils import *
import pandas as pd

ht_api_secret_name = "dev/ht-api"

parser = argparse.ArgumentParser(description="In triage enrollments report")
common_cli_args(parser)

args = parser.parse_args()
ht_api_secret_name = args.ht_api_secret_name
aws_profile = args.profile_name
ca_bundle_path = args.ca_bundle_path

print("Using AWS profile", aws_profile, "and HT API key", ht_api_secret_name, ", CA bundle path", ca_bundle_path)

if not path.exists(ca_bundle_path):
    print("CA bundle file", ca_bundle_path, "not found")
    sys.exit(1)

# Clinics and location collected with:
#echo 'select name, short_name, clinic_id, brand_id from clinic_brands' | mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD -D $DB_NAME -A -B > clinics.tsv
#echo 'select clinic_id, id, label from clinic_locations' | mysql -h $DB_HOST -P $DB_PORT -u $DB_USERNAME -p$DB_PASSWORD -D $DB_NAME -A -B > clinic_locations.tsv
clinics_file = "clinics.tsv"
locations_file = "clinic_locations.tsv"

if not path.exists(clinics_file):
    print("Clinic names file", clinics_file, "not found")
    sys.exit(1)

if not path.exists(locations_file):
    print("Location names file", locations_file, "not found")
    sys.exit(1)

clinics_df = pd.read_table(clinics_file)
locations_df = pd.read_table(locations_file)
    
print("Opening session with mongoDB")

mongo_uri, mongo_db = get_db_name(ht_api_secret_name, aws_profile, ca_bundle_path)
mc = MongoClient(mongo_uri, ssl=True, ssl_ca_certs=ca_bundle_path)
db = mc[mongo_db]

ht_status = db["ht_status"]
ht_events = db["ht_events"]
enr = db["enrollments"]

print("Querying mongoDB", mongo_db, "database")

test_cids = [12,61,129,167,226,251,302,306]

tr_hts = [i for i in ht_status.find({"category": "TRIAGE", "clinicId":{"$nin":test_cids}},)]

tr_eids = [str(i["_id"]) for i in tr_hts]

tr_events = [list(ht_events.find({"enrollmentId":i}).sort([("date", ASCENDING)])) for i in tr_eids]

enr_status = [enr.find_one({"_id":hts["_id"]},{"status":1}) for hts in tr_hts]

print("Processing data")

dt_now = datetime.utcnow()
tr_df = pd.DataFrame()

for ei in range(len(tr_events)):
    pi = tr_hts[ei]["patientInfo"]
    clinic_id = int(tr_hts[ei]["clinicId"])
    tr_info = {"enrollmentId":tr_eids[ei], "mrn":pi["mrn"], "patientId":pi["_id"], "clinicId":clinic_id}
    tr_info["status"] = enr_status[ei]["status"]
    if "locationId" in tr_hts[ei]:
       location_id = int(tr_hts[ei]["locationId"])
       tr_info["locationId"] = location_id
    tt_events = [i for i in tr_events[ei] if i["type"] == "TRIAGE_TICKET_CREATED"]
    if len(tt_events) > 0:
       last_tt_event = tt_events[-1]
       tr_info["ttCreated"] = last_tt_event["date"]
       tr_info["timeDiff"] = dt_now - last_tt_event["date"]
    tr_df = tr_df.append(tr_info, ignore_index=True)

tr_df = tr_df.merge(locations_df[ ["id", "label"]  ], left_on='locationId', right_on='id', how='left')
tr_df = tr_df.drop("id", 1).rename(columns={"label":"Location"})
tr_df = tr_df.merge(clinics_df[ ["clinic_id", "name"]  ], left_on='clinicId', right_on='clinic_id', how='left')
tr_df = tr_df.drop("clinic_id", 1).rename(columns={"name":"clinic"})

report_fname = "in_triage_" + time.strftime("%Y%m%d-%H%M%S") + ".csv"
print("Saving file", report_fname)
tr_df.to_csv(report_fname)




