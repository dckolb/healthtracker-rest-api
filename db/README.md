# DB update scripts

This directory contains MongoDB update scripts.
Note that unlike relational DBs MongoDB does not have a DB schema definition.
Instead DB schema is enforced in the code and not via explicit schema defined and saved on
the DB server.
The result is that MongoDB update scripts are limited to data patching functionality only.

The DB schema management frameworks usually keep a record of applied DB patches
in a special table in the same DB.
Because with MongoDB database management scripts we only can patch the data
and there is no schema to change, we can only keep a list of patches that were applied.
Unlike relational DB it is possible and we may be necessary to apply the same patch multiple times.

Every time the patch is applied we add a log record of the fact.
The patching log records are added to the `applied_patches` collection in the same database.

# MongoDB authentication

We use AWS secret management service to store application configuration.
MongoDB patching scripts saved in this directory use AWS API to get access to that configuration.
No DB credentials or host name is provided on the command line when we run the patch script.
Only AWS environment profile name and the HT API configuration key name is required.

# RDS certificates bundle file

In order to access any of the AWS RDS databases AWS RDS CA bundle must be obtained.
For more details see https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/UsingWithRDS.SSL.html.

# Examples

- Dry run of the close_complete_enrollments.py script. Only watch what would be done.

```
python3 close_expired_enrollments.py  --secret_name dev/ht-api --ca_bundle ~/Work/secret/rds-combined-ca-bundle.pem

```

- Make actual DB updates with the close_complete_enrollments.py script

```
python3 close_expired_enrollments.py --run  --secret_name dev/ht-api --ca_bundle ~/Work/secret/rds-combined-ca-bundle.pem

```


python3 set_ht_status_therapy_types.py  --secret_name dev/ht-api --ca_bundle rds-ca-2019-root.pem