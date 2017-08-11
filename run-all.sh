#!/bin/bash
export LC_NUMERIC=C

bench="math"

function sypet-logo {
    echo "$(tput setaf 4)              ____     ___      __ $(tput sgr 0)"
    echo "$(tput setaf 4)             / __/_ __/ _ \___ / /_$(tput sgr 0)"
    echo "$(tput setaf 4)            _\ \/ // / ___/ -_) __/$(tput sgr 0)"
    echo "$(tput setaf 4)           /___/\_, /_/   \__/\__/ $(tput sgr 0)"
    echo "$(tput setaf 4)               /___/            $(tput sgr 0)"
}

function run-bench {
    if [ ! -d output ] ; then
	mkdir output
    fi
    
    if [ ! -d output/$bench ] ; then
        mkdir output/$bench
    else
        echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Cleaning output files $(tput bold)output/$bench$(tput sgr 0)..."            
        rm -f output/$bench/*
    fi
    
    echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Running $(tput bold)$bench$(tput sgr 0) benchmarks..."
    for f in benchmarks/$bench/* ; do 
	id=$(basename $f)
	./run-sypet.sh "$f/benchmark$id.json" &> output/$bench/benchmark$id.log
	if grep TIMEOUT -q output/$bench/benchmark$id.log ; then
	    echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Benchmark $(tput bold)$id$(tput sgr 0)            $(tput setaf 1)[TIMEOUT]$(tput sgr 0)"
	elif grep Solution -q output/$bench/benchmark$id.log ; then
	    echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Benchmark $(tput bold)$id$(tput sgr 0)            $(tput setaf 2)[OK]$(tput sgr 0)"
	else
	    echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Benchmark $(tput bold)$id$(tput sgr 0)            $(tput setaf 1)[FAILED]$(tput sgr 0)"
	fi
    done
}

sypet-logo

bench="math"
run-bench

bench="geometry"
run-bench

bench="joda"
run-bench

bench="xml"
run-bench

