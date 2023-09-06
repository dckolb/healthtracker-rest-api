import boto3, json
from datetime import datetime
import os

def get_db_name(ht_api_secret_name="dev/ht-api", aws_profile="default", ca_bundle_path="rds-combined-ca-bundle.pem"):
    ''' Return MonogoDB URI (connection string) and 
        the DB name from the environment configuration saved in AWS secrets service '''
    session = boto3.Session(profile_name=aws_profile)
    sc = session.client('secretsmanager')
    s = sc.get_secret_value(SecretId=ht_api_secret_name)
    s = json.loads(s["SecretString"])
    mongo_uri = s["spring.data.mongodb.uri"]
    mongo_db = s["spring.data.mongodb.database"]
    return (mongo_uri, mongo_db)


def log_db_patch(db, patch_name, comment):
    ''' Add DB patching record
    '''
    changes_log = db["applied_patches"]
    changes_log.insert_one({"patch":patch_name, "comment":comment, "applied":datetime.utcnow()})


def common_cli_args(parser):
    ht_api_secret_name = "dev/ht-api"
    parser.add_argument('--secret_name', dest='ht_api_secret_name',
                   default=ht_api_secret_name,
                   help='AWS HT API secret name, defaults to ' + ht_api_secret_name)
    parser.add_argument('--profile_name', dest='profile_name',
                   default="default",
                   help='If there are multiple environments in .aws/credentials set the desired profile name')
    parser.add_argument('--ca_bundle', dest='ca_bundle_path',
                   default=os.environ['HOME']+"/rds-combined-ca-bundle.pem",
                   help='CA bundle file path')
    parser.add_argument('--run', dest='run',
                    default=False, const=True, type=bool, nargs="?",
                    help='Say --run if you want to make DB changes, otherwise no changes will be made')

