#!/bin/bash
set -e

# wait for Cassandra to be ready
sleep 30

# start spring app server
java -jar /usr/src/tinyurl.jar --spring.config.location=file:/opt/conf/application.properties
