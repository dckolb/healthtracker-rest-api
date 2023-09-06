# HealthTracker Rest API

| Branch | Build | Test Coverage |
| --- | --- | --- |
| Master | [![CircleCI](https://circleci.com/gh/NavigatingCancer/healthtracker-rest-api/tree/master.svg?style=svg&circle-token=b1e4b7e3a61d7fee4b534f2ac2e435b029399da3)](https://circleci.com/gh/NavigatingCancer/healthtracker-rest-api/tree/master) | [DataDog](https://navigatingcancer.datadoghq.com/dashboard/2xt-fin-96t/health-tracker-code-coverage) |
| Develop | [![CircleCI](https://circleci.com/gh/NavigatingCancer/healthtracker-rest-api/tree/develop.svg?style=svg&circle-token=b1e4b7e3a61d7fee4b534f2ac2e435b029399da3)](https://circleci.com/gh/NavigatingCancer/healthtracker-rest-api/tree/develop)  | [DataDog](https://navigatingcancer.datadoghq.com/dashboard/2xt-fin-96t/health-tracker-code-coverage) |

Basic RESTful api for Health Tracker Enrollments and CheckIns.

## Project Info

This is a Spring Boot application backed by MongoDB.

It utilizes the Spring Data MongoDB project for data access (Reactive Java).

Unit tests utilize an embedded mongo instance.

[Swagger-UI](https://eks-dev-2022.nc-acceptance.com/ht-api/swagger-ui.html) is embedded.


## Running
### Prerequisites
- JDK 17  Corretto LTS
- Maven 3
- Make AWS IAM credentials [accessible to the JVM](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/DefaultAWSCredentialsProviderChain.html)
    - If not already done, make sure [AWS programmatic access](https://navigatingcancer.atlassian.net/wiki/spaces/INF/pages/1113622172/AWS+access#Programmatic-access) and [Kubernetes cluster access](https://navigatingcancer.atlassian.net/wiki/spaces/INF/pages/1892188510/K8s+Cluster+access#Configure-cluster-access) is setup for AWS Direct and eks-dev-2022 kubernetes environment.
- Import the [RDS Certificate Authority](https://docs.aws.amazon.com/documentdb/latest/developerguide/ca_cert_rotation.html) (this may require `sudo` depending on who owns your `$JAVA_HOME`):
```
keytool -importcert -cacerts -file src/main/resources/rds-ca-2019-root.pem -storepass changeit -alias aws-rds -noprompt
```

### Local Development
To start the service locally in development mode, use `make run-local`. This requires access to the development Kubernetes cluster (eks-dev-2022). Once the service is listening, the swagger UI will open in your default browser.



### Lombok

This project uses Lombok Annotations to create methods.

If you are using vscode you will need to install [this extension](https://marketplace.visualstudio.com/items?itemName=GabrielBB.vscode-lombok).

## Date and time

When dates are returned by the service the dates should be of Java Date or LocalDate type.
The Date class is essentially is just a wrapper over epoch time.
The LocalDate class is a representation of the local date without the time component.
When send over the wire in JSON format the Date class is sent in ISO 8601 format with
the time zone component of "+000".
The LocalDate class is send in ISO 8601 with the date component only.
In both cases that is sufficient for the UI to display date or date-time information correctly.

## Building

* Unit test `mvn test`
* Build shaded jar `mvn package`

### docker-compose

This project contains a docker-comopose file for local integration testing.

On MacOS you will need to set `TMPDIR=/private$TMPDIR` before running `make integration`.

## Swagger

You can view the swagger docs [here](https://eks-test.nc-acceptance.com/ht-api/swagger-ui.html).

## Postman

This project attempts to contain a set of Postman tests that can be used for development and debugging purposes.

## Browser Tests

CircleCI triggers a job with browser level regression test after deploys to acceptance

## Deployment

Currently this project is deployed to a environment running on kubernetes via [CircleCI](https://circleci.com/gh/NavigatingCancer/healthtracker-rest-api).

| environment | url |
|-------------|-----|
| development | [https://eks-dev-2022.nc-acceptance.com/ht-api](https://eks-dev-2022.nc-acceptance.com/ht-api) |
| acceptance | [https://htaccept2.nc-acceptance.com/ht-api](https://htaccept2.nc-acceptance.com/ht-api) |
| staging | [https://staging.nav.care/ht-api](https://staging.nav.care/ht-api) |
| preprod | [https://preprod.nav.care/ht-api](https://nav.care/ht-api) |
| production | [https://nav.care/ht-api](https://nav.care/ht-api) |

### kubectl

You can find instructions on installing `kubectl` from Amazon [here](https://docs.aws.amazon.com/eks/latest/userguide/install-kubectl.html).

#### Amazon AWS CLI

The instructions for installing the Amazon AWS CLI can be found [here](https://docs.aws.amazon.com/cli/latest/userguide/install-bundle.html#install-bundle-user).

#### Amazon IAM AWS

You find instructions on setting up Amazon IAM AWS [here](https://docs.aws.amazon.com/eks/latest/userguide/install-aws-iam-authenticator.html).

## Makefile

This project strives to use the Makefile as a way for the team to utilize individual commands related to the project, e.g. gather dependencies is a seperate one from build. These individual commands can then be aggregated to achieve more complicated operations.

## Responsibilities
### Interactions with other services
REST API has the following interactions with other services
- Consumes the reminder SQS queue from [Scheduler Service](https://github.com/NavigatingCancer/scheduler-service)
- Produces messages for the notification SQS queue for [Notification Service](https://github.com/NavigatingCancer/notification-service)
- Consumes the gc events RabbitMQ queue from [GC](https://github.com/NavigatingCancer/gc)
- Produces messages for the ht events RabbitMQ queue for [GC](https://github.com/NavigatingCancer/gc)
- Produces and consumes the ht status SQS queue
- Provides check in, enrollment, and patient request data to [Patient Experience Server](https://github.com/NavigatingCancer/patient-experience-server)
- Consumes demographic information (name, birth date, etc) from [Patient Info Service](https://github.com/NavigatingCancer/patient_info_service)
- Serves as [Health Tracker Server](https://github.com/NavigatingCancer/health_tracker_server)'s primary gateway to data stored in DocumentDB
- Gets clinic configuration data from [Survey API](https://github.com/NavigatingCancer/survey-api) to support the rules engine

### Rules engine
REST API also contains a drools rules engine that acts as a state machine for Health Tracker Statues

### Desired development direction
Moving forward we would like to move the rules engine from REST API into it's own service and move to make the REST API the only access point for our DocumentDB collections.