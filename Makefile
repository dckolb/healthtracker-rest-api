# capture git sha & tag
export GIT_COMMIT_SHA = $(shell git rev-parse HEAD)
export GIT_COMMIT_TAG = $(shell git tag --points-at HEAD)

clean:
	mvn clean -s .circleci/maven.settings.xml

install:
	mvn install -s .circleci/maven.settings.xml

compile:
	mvn clean compile -s .circleci/maven.settings.xml 

package:
	mvn clean package -s .circleci/maven.settings.xml

test: 
	mvn test -s .circleci/maven.settings.xml

format:
	mvn git-code-format:format-code
	
run-local:
	bash run_local.sh

# build docker image
docker-image:
	docker build ./ -t ht-api
	docker build ./ -t ht-api:latest
	docker tag ht-api ht-api:${GIT_COMMIT_SHA}

# run location integration setup
integration-run:
	cd .docker && docker-compose up -d --remove-orphans
	sleep 1
	./.docker/mongodb/seed.sh

# teardown integration
integration-teardown:
	cd .docker && docker-compose down --remove-orphans

# run local mongo via docker-compose
integration-run-mongo:
	cd .docker && docker-compose run -d -p 27018:27017 mongodb 
	sleep 1
	./.docker/mongodb/seed.sh
