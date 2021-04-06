#!/usr/bin/env bash

# Show usage
usage () {
    if [ -n "$1" ]
    then
        echo "$1"
    fi

    cat << EOP

$0 -n|--num-records <number of records to generate>    (default 1000)
   -d|--delimiter                                      (default ;)
   -s|--skip-header                                    (default false)
   -h|--help"

Example:
  $0 -n 500000 -d , > myfile.csv
EOP

    exit 0
}

num_records=1000
delimiter=";"
skip_header=0

# Read commandline arguments
while [ "$1" != "" ]
do
    case $1 in
        -n | --num-records ) shift
                         num_records=$1
                         ;;
        -d | --delimiter ) shift
                         delimiter=$1
                         ;;
        -s | --skip-header ) skip_header=1
                         ;;
        -h | --help )    usage
                         exit
                         ;;
        * )              usage
                         exit 1
    esac
    shift
done


function generate_records {
    i=1

    if [ $skip_header -eq 0 ]
    then
        echo -ne "id${delimiter}navn${delimiter}alder${delimiter}adresse1${delimiter}adresse2${delimiter}postnummer${delimiter}poststed${delimiter}medlem\n"
    fi

    while [ $i -le $num_records ]; do
        echo -ne "$i${delimiter}\"Donald Duck $((i))\"${delimiter}42${delimiter}\"Andeveien $((i))\"${delimiter}Andedammen${delimiter}3158${delimiter}Andebu${delimiter}$(( i % 2 ))\n"
        let i=i+1
    done
}

generate_records
