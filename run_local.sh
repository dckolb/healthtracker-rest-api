#!/usr/bin/env bash
#
# This script starts the application locally using the dev k8s cluster

function kill_bg()
{
   kill $(jobs -p)
} 

function wait_for()
{
   while ! curl --output /dev/null --silent --head "$1"; do
      sleep 1
   done
}

function open_browser_when_ready()
{
   wait_for http://localhost:8080

   # TODO: cross-platform browser open
   open http://localhost:8080/ht-api/swagger-ui.html
}

# Switch to our desired kubernetes cluster.
kubectl config use-context eks-dev-2022

kubectl -n health-tracker port-forward svc/survey-api 8097:80 > /dev/null 2>&1 &
export SURVEY_API_URL=http://localhost:8097

kubectl -n health-tracker port-forward svc/health-tracker-documents 9005:80 > /dev/null 2>&1 &
export HT_DOCUMENTS_API_URL=http://localhost:9005

# Kill the background processes on exit
trap kill_bg EXIT

echo "Waiting for port-forwards to wake up"
wait_for $SURVEY_API_URL
wait_for $HT_DOCUMENTS_API_URL

echo "Starting service"

# Configure the database using the deployed service's config
eval "$(kubectl exec deployment/ht-gql -n health-tracker -- env | grep MONGO_DB)"
export MONGO_DB_PORT
export MONGO_DB_NAME
export MONGO_DB_HOST
export MONGO_DB_USER
export MONGO_DB_PWD
export NOTIFICATION_QUEUE=notification-requests-dev.fifo
export HT_REMINDER_QUEUE=ht-dev-reminder
export HT_STATUS_QUEUE=ht-dev-status

JSON_BASEPATH="${HOME}/.aws/sso/cache"
JSON_FILE=$(ls -tr "${JSON_BASEPATH}" | tail -n1)
EXPIRATION_ISO=$(cat ${JSON_BASEPATH}/${JSON_FILE} | jq -r '.expiresAt')
EXPIRATION_TIMESTAMP=$(date -jf "%Y-%m-%dT%H:%M:%SZ" $EXPIRATION_ISO "+%s")
NOW=$(date +%s)
if [ $EXPIRATION_TIMESTAMP -lt $NOW ] ;
then
   echo "AWS SSO credentials are expired.  Please renew them and try again"
   exit 1
fi 

export AWS_ACCESS_KEY_ID=$(cat ${JSON_BASEPATH}/${JSON_FILE} | jq -r '.clientId')
export AWS_SECRET_ACCESS_KEY=$(cat ${JSON_BASEPATH}/${JSON_FILE} | jq -r '.clientSecret')
export AWS_SESSION_TOKEN=$(cat ${JSON_BASEPATH}/${JSON_FILE} | jq -r '.accessToken')
export AWS_REGION="us-east-1"

open_browser_when_ready &

mvn spring-boot:run -Dspring-boot.run.profiles=local | jq --raw-input '. | try fromjson'
