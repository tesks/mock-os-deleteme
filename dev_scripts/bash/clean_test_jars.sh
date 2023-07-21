#!/bin/bash

dir=$1
# Clean test JARs that might interfere with actual release apps
numfiles=`find $dir/adaptations -type f -iname "*-tests.jar" | wc -l`
echo "Deleting test JARs from adaptations; number found: $numfiles"
find $dir/adaptations -type f -iname "*-tests.jar" -delete
