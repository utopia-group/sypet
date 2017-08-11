#!/bin/bash

ANT="ant"
ID=$1
OPT=$2

# argument check
if [ $# -le 0 ]; then
    echo 'Usage: ./run-sypet.sh <path/json/file>'
    exit 1
fi

if [ $# -eq 1 ]; then
    OPT="-r"
fi

# run the experiment
$ANT sypet -Dargs="-file $1 $OPT"


