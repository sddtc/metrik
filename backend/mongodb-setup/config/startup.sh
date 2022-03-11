#!/bin/bash

mkdir -p /data/db
/usr/local/bin/docker-entrypoint.sh mongod -keyFile /app/mongo/keyfile.txt --replSet rs0 --bind_ip_all
