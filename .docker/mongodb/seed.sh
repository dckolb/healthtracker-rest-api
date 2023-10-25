#!/bin/bash

# seed
mongo --port 27018 ht --eval "db.dropDatabase()"
mongo --port 27018 --eval "db.getSiblingDB('ht').createUser({ user: 'testuser', pwd: 'testsecret', roles:[{role: 'readWrite', db: 'ht'}] })"
mongoimport --port 27018 --jsonArray --db ht --collection enrollments --file ./.docker/mongodb/enrollments-active.json
mongoimport --port 27018 --jsonArray --db ht --collection enrollments --file ./.docker/mongodb/enrollments-completed.json
mongoimport --port 27018 --jsonArray --db ht --collection enrollments --file ./.docker/mongodb/enrollments-stopped.json
mongoimport --port 27018 --jsonArray --db ht --collection checkins --file ./.docker/mongodb/checkins.json
