#!/bin/bash -e

docker login
docker build -t webis/netspeak-indexing:1.0.3 .
docker push webis/netspeak-indexing:1.0.3
