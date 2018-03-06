#!/bin/bash

# argument check
if [ $# -le 0 ]; then
    echo 'Usage: ./run-sypet.sh <path/to/json> <options>'
    echo 'Options:'
    echo '       -disableObjfunction   Disable Objective Function.'
    echo '       -disablePruning       Disable Pruning.'
    echo '       -disableRoundrobin    Disable Round Robin.'
    exit 1
fi

# run the experiment
ant -buildfile sypet/build.xml sypet -Dargs="-file $PWD/$1 $2"
