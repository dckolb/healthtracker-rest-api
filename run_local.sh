#!/usr/bin/env bash
#
# This script starts the application locally using the dev k8s cluster

function kill_bg()
{
   # shellcheck disable=SC2046
   kill $(jobs -p) > /dev/null 2>&1
} 

function wait_for()
{
   while ! curl --output /dev/null --silent --head "$1"; do
      sleep 1
   done
   echo "✅ $2 is listening at $1"
}

function open_browser_when_ready()
{
   wait_for http://localhost:3434 Swagger

   # TODO: cross-platform browser open
   open http://localhost:3434/ht-api/swagger-ui.html
}

# Switch to our desired kubernetes cluster.
kubectl config use-context eks-dev-2022

# Forward the services we depend on
kubectl -n health-tracker port-forward svc/survey-api 8097:80 > /dev/null 2>&1 &
export SURVEY_API_URL=http://localhost:8097

kubectl -n health-tracker port-forward svc/health-tracker-documents 9005:80 > /dev/null 2>&1 &
export HT_DOCUMENTS_API_URL=http://localhost:9005

kubectl -n health-tracker port-forward svc/pt-info-svc 9595:80 > /dev/null 2>&1 &
export PATIENT_INFO_SERVICE_URL=http://localhost:9595/patientinfo

kubectl -n health-tracker port-forward svc/scheduler-service 7373:80 > /dev/null 2>&1 &
export SCHEDULER_SERVICE_URL=http://localhost:7373/scheduler-service

# Kill the background processes on exit
trap kill_bg EXIT

echo "Waiting for port-forwards to wake up"
wait_for $SURVEY_API_URL "Survey API"
wait_for $HT_DOCUMENTS_API_URL "HT Documents API"
wait_for $PATIENT_INFO_SERVICE_URL "Patient Info Service"
wait_for $SCHEDULER_SERVICE_URL "Scheduler Service"

echo "Configuring environment from live service"
eval "$(kubectl exec deployment/ht-api -n health-tracker -- env | grep 'MONGO_DB\|_QUEUE\|GC_\|PX_')"
# Mongo config
export MONGO_DB_PORT
export MONGO_DB_NAME
export MONGO_DB_HOST
export MONGO_DB_USER
export MONGO_DB_PWD

# Queue config
export NOTIFICATION_QUEUE
export HT_REMINDER_QUEUE
export HT_STATUS_QUEUE
export HT_JOBS_QUEUE

# GC config
export GC_API_URL
export GC_ACCESS_KEY
export GC_ACCESS_SECRET

# PX config
export PX_DOMAIN
export PX_KEY
export PX_SECRET

export LOG_LEVEL=info

export PAGER=''
identity=$(aws sts get-caller-identity --output json --profile=direct)
accountId=$(echo "$identity" | jq -r '.Account')
roleName=$(echo "$identity" | jq -r '.Arn | sub("^.*AWSReservedSSO_(?<role>.*?)_[a-f0-9]{16}/.*$"; .role)')
accessToken=$(jq -r '.accessToken | select(.)' ~/.aws/sso/cache/*.json)
awsRegion=$(jq -r '.region | select(.)' ~/.aws/sso/cache/*.json)
credentials=$(aws sso get-role-credentials --role-name="$roleName" --account-id="$accountId" --access-token="$accessToken" --profile="$1" --output json)

export AWS_ACCESS_KEY_ID=$(echo "$credentials" | jq -r '.roleCredentials.accessKeyId')
export AWS_SECRET_ACCESS_KEY=$(echo "$credentials" | jq -r '.roleCredentials.secretAccessKey')
export AWS_SESSION_TOKEN=$(echo "$credentials" | jq -r '.roleCredentials.sessionToken')
export AWS_REGION=${awsRegion}

open_browser_when_ready &

echo "Starting service"

mvn spring-boot:run -Dspring-boot.run.profiles=local | jq --raw-input '.
                                                                      | try fromjson
                                                                      | reduce to_entries[] as {$key, $value} (null;
                                                                         if   $key == "error.stack_trace"      then ."error.stack_trace" = $value / "\n\t"
                                                                         else                                 .[$key] = $value end
                                                                      )'
