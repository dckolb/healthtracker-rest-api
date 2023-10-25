#!/bin/bash

# clean build environment
make clean

# build project
make install

# build docker image
make docker-image

# run docker-compose
make integration-run

# seed mongo db
./.docker/mongodb/seed.sh