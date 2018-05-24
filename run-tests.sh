#!/bin/bash
export LC_NUMERIC=C

# change this information if necessary
# ip and port to perform the connection

#ip="192.168.0.7"
ip="100.120.0.7"
#ip="127.0.0.1"
port="30050"
#port="9092"

if [ "$#" -eq 2 ]; then
    ip=$1
    port=$2
fi

function sypet-logo {
    echo "$(tput setaf 4)              ____     ___      __ $(tput sgr 0)"
    echo "$(tput setaf 4)             / __/_ __/ _ \___ / /_$(tput sgr 0)"
    echo "$(tput setaf 4)            _\ \/ // / ___/ -_) __/$(tput sgr 0)"
    echo "$(tput setaf 4)           /___/\_, /_/   \__/\__/ $(tput sgr 0)"
    echo "$(tput setaf 4)               /___/            $(tput sgr 0)"
    echo "$(tput setaf 4)                                $(tput sgr 0)"
    echo "$(tput setaf 4)     [ Connecting to $ip:$port ]$(tput sgr 0)"
    echo "$(tput setaf 4)$(tput sgr 0)"
}

function run-bench {
    echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Running $(tput bold)$bench$(tput sgr 0) benchmarks..."
    for f in *.json ; do 
	   id=$(basename $f)
	   curl -s -X POST -d @$f http://$ip:$port --header "Content-Type:application/json" > output.txt       
        if grep "return" -q output.txt ; then
            echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Benchmark $(tput bold)$id$(tput sgr 0)            $(tput setaf 2)[OK]$(tput sgr 0)"
            cat output.txt
        else 
	       echo "$(tput setaf 4)[SyPet]$(tput sgr 0) Benchmark $(tput bold)$id$(tput sgr 0)            $(tput setaf 1)[FAILED]$(tput sgr 0)"
	   fi
    done
    rm -f output.txt
}

sypet-logo
run-bench
