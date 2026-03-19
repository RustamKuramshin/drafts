#!/usr/bin/env bash

java -Xms256m -Xmx512m -XX:+FlightRecorder -XX:StartFlightRecording=duration=60s,filename=myrecording.jfr MemoryIssue.java