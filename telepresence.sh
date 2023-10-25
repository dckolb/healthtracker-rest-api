#!/bin/sh
SERVICE_NAME="ht-api"
INTERCEPT_PORT="3000"
NAMESPACE="health-tracker"
SERVICE_PORT_NAME="api"

run_local() {
  input="./telepresence.env"
  while read -r line; do
    export $line
  done < "$input"

  export SURVEY_API_URL=http://survey-api
  export HT_DOCUMENTS_API_URL=http://health-tracker-documents

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

  mvn spring-boot:run -Dspring-boot.run.profiles=local -DskipTests | jq --raw-input '. | try fromjson'
}

help() {
  cat <<EOF
This script connects to the telepresence instance in our cluster and sets up an intercept for this service ($SERVICE_NAME).
EOF
  exit
}

set_context() {
  kubectl config use-context $CONTEXT
}

connect() {
  echo "Connecting to telepresence"
  OUTPUT=$(telepresence connect 2>&1)
  if [[ $OUTPUT == *"command not found"* ]]; then
    echo "Telepresence is not installed.  Would you like to brew install it? (y|n)"
    read answer
    if [[ $answer == *"y"* ]]; then
      package="datawire/blackbird/telepresence"
      if [[ $(arch) == *"arm"* ]]; then package=$package-arm64 ; fi
      brew install $package
    else
      echo "For more infomation on getting setup with telepresence see https://www.telepresence.io/docs/latest/quick-start/"
      exit 2
    fi
    echo "Retrying connect"
    connect
  elif [[ $OUTPUT == *"error"* ]]; then
    echo "Unable to connect: $OUTPUT"
    exit 2
  fi
}

busy() {
  SERVICE_STATUS=$(telepresence list -n $NAMESPACE | grep $SERVICE_NAME)
  if [[ $SERVICE_STATUS == *"intercepted"* ]]; then
    echo "$SERVICE_NAME is currently being intercepted by someone else"
    BUSY=true
  else
    echo "$SERVICE_NAME is availabe to be intercepted"
    BUSY=false
  fi
}

intercept() {
  busy
  if [ $BUSY = true ]; then exit 1; fi
  echo "Intercepting $SERVICE_NAME"
  telepresence intercept $SERVICE_NAME --port $INTERCEPT_PORT:$SERVICE_PORT_NAME --env-file telepresence.env -n $NAMESPACE
  if [ $? -ne 0 ]; then
    echo "Intercept failed"
    exit 3
  fi
}

leave() {
  echo "\nClosing intercept"
  telepresence leave $SERVICE_NAME-$NAMESPACE
  telepresence quit
}

while getopts 'c:hb' OPTION; do
  case "$OPTION" in 
    c)
      CONTEXT="$OPTARG"
      ;;
    h)
      help
      ;;
    b)
      busy
      exit 1
      ;;
    ?)
      echo "Invalid flag provided. Script usage: $(basename "${BASH_SOURCE[0]}")" >&2
      exit 1
      ;;
  esac
done

if [ ! -z "$CONTEXT" ]; then set_context; fi
connect
trap leave EXIT
intercept
run_local
