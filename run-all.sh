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
    if [ ! -d output/$benh ] ; then
        mkdir output/$bench
    else
        echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Cleaning output files $(tput bold)output/$bench$(tput sgr 0)..."            
        rm -f output/$bench/*
    fi
    
    echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Running $(tput bold)$bench$(tput sgr 0) benchmarks..."
    for f in sypet/benchmarks/$bench/* ; do 
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

function build-csv {

    filename="output/result.csv"
    descriptionfile="output/description.txt"
    declare -a id
    declare -a description
    declare -a synthesis
    declare -a paths
    declare -a progs
    declare -a tests
    declare -a comps
    declare -a holes

    echo "ID,Description,Synthesis Time (s),#Paths,#Progs,#Tests,#Comps,#Holes" > $filename
    for f in {1..30} ; do
	id[$f]=$f
    done

    i=1
    while read -r line ; do
	description[$i]=$line
	(( i++ ))
    done < $descriptionfile

    
    i=1
    for f in math geometry joda xml ; do 
	for z in output/$f/*.log ; do
	    y=`grep "Synthesis Time" $z | cut -d ':' -f 2`
	    synthesis[$i]=`echo "scale=2; $y/1000" | bc -l`
	    (( i++ ))
	done
    done
    
    i=1
    for f in math geometry joda xml ; do
        for z in output/$f/*.log ; do
            paths[$i]=`grep "Number of sketches" $z | cut -d ':' -f 2`
            (( i++ ))
        done
    done

    i=1
    for f in math geometry joda xml ; do
        for z in output/$f/*.log ; do
            progs[$i]=`grep "Number of completed programs" $z | cut -d ':' -f 2`
            (( i++ ))
        done
    done

    i=1
    for f in math geometry joda xml ; do
        for z in output/$f/*.log ; do
            holes[$i]=`grep "Number of holes" $z | cut -d ':' -f 2`
            (( i++ ))
        done
    done

    i=1
    for f in math geometry joda xml ; do
        for z in output/$f/*.log ; do
            comps[$i]=`grep "Number of components" $z | cut -d ':' -f 2`
            (( i++ ))
        done
    done

    i=1
    for f in math geometry joda xml ; do
	for z in sypet/benchmarks/$f/* ; do
	    t=`grep "boolean test" $z/TestSource.java -c`
	    if [ $t -gt 1 ] ; then
		(( t-- ))
	    fi
	    tests[$i]=$t
	    (( i++ ))
	done
    done

    echo "ID,Description,Synthesis Time (s),#Paths,#Progs,#Tests,#Comps,#Holes" > $filename
    for f in {1..30} ; do 
	echo "${id[$f]},${description[$f]},${synthesis[$f]},${paths[$f]},${progs[$f]},${tests[$f]},${comps[$f]},${holes[$f]}" >> $filename
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

build-csv
