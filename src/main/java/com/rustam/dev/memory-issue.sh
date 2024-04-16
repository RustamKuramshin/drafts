#!/usr/bin/env bash

java -Xms256m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./jvm_heap_dump.hprof MemoryIssue.java