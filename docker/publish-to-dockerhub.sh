#!/bin/bash -e

docker login
docker build -t webis/netspeak-indexing:1.0.2 .
docker push webis/netspeak-indexing:1.0.2
