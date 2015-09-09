#!/bin/bash

./sbt "project $APPLICATION_NAME" test assets dist
