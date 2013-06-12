#!/bin/sh
# Build a stack.
# Use CMD_PROPS to pass command line properties.
if [ -z "$CMD_PROPS" ] ;
  then $CMD_PROPS=""
fi
java -cp target/stack-builder-0.1.9-SNAPSHOT-jar-with-dependencies.jar $CMD_PROPS org.sagebionetworks.stack.BuildStackMain "$@"