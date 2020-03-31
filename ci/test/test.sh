#!/bin/sh -e
#

# Validate Jenkinsfile
# Usage: ./test.sh
# should output: Jenkinsfile successfully validated.

docker build -t jenkinslint .
docker run -d --rm -p 8080:8080 --env-file=./env.file --name jenkinslint jenkinslint
sleep 15

curl -u user:pass -X POST -F "jenkinsfile=<../Jenkinsfile" http://localhost:8080/pipeline-model-converter/validate

docker rm -f jenkinslint