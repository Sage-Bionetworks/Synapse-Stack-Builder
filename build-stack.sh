#!/bin/sh
# Build a stack.
java -cp target/stack-builder-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.sagebionetworks.stack.BuildStackMain "$@"