#!/bin/bash
export LC_NUMERIC=C

ip="127.0.0.1"
port="9092"

function sypet-logo {
    echo "$(tput setaf 4)              ____     ___      __ $(tput sgr 0)"
    echo "$(tput setaf 4)             / __/_ __/ _ \___ / /_$(tput sgr 0)"
    echo "$(tput setaf 4)            _\ \/ // / ___/ -_) __/$(tput sgr 0)"
    echo "$(tput setaf 4)           /___/\_, /_/   \__/\__/ $(tput sgr 0)"
    echo "$(tput setaf 4)               /___/            $(tput sgr 0)"
}

function run-bench {
    echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Running $(tput bold)$bench$(tput sgr 0) benchmarks..."
    for f in *.json ; do 
	   id=$(basename $f)
	   curl -X POST -d @$f http://$ip:$port --header "Content-Type:application/json" &> output.txt       
        if grep "return" -q output.txt ; then
            echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Benchmark $(tput bold)$id$(tput sgr 0)            $(tput setaf 2)[OK]$(tput sgr 0)"
        else 
	       echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Benchmark $(tput bold)$id$(tput sgr 0)            $(tput setaf 1)[FAILED]$(tput sgr 0)"
	   fi
    done
    rm -f output.txt
}

sypet-logo

run-bench
