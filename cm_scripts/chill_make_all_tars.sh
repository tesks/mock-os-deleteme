#!/bin/sh

# Script used by the Jenkins deploy build helpers

if [ $# -ne 2 ]
then
    echo "Error: Not enough input arguments"
    echo "Usage: chill_make_all_tars.sh <mpcs_version> <mpcstools_version>"
    exit
fi

./chill_maketar.p -m msl -v $1 -k $2
./chill_maketar.p -m ody  -v $1 -k $2
./chill_maketar.p -m generic -v $1 -k $2
./chill_maketar.p -m smap -v $1 -k $2
./chill_maketar.p -m nsyt -v $1 -k $2
./chill_maketar.p -m mco -v $1 -k $2
./chill_maketar.p -m nsp -v $1 -k $2
