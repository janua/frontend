#!/bin/bash

if [ @APPLICATION_NAME != "javascript_tests" ]
  then
    ./sbt "project $APPLICATION_NAME" test assets dist
fi
