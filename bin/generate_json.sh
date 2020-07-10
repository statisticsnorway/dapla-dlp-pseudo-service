#!/usr/bin/env bash

# Show usage
usage () {
    if [ -n "$1" ]
    then
        echo "$1"
    fi

    cat << EOP

$0 -n|--num-records <number of records to generate>    (default 1000)
   -d|--detached-records                               (default false)
   -h|--help"

Example:
  $0 -n 500000 > myfile.json
EOP

    exit 0
}

num_records=1000
detached_records=0

# Read commandline arguments
while [ "$1" != "" ]
do
    case $1 in
        -n | --num-records ) shift
                         num_records=$1
                         ;;
        -d | --detached-records ) detached_records=1
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
    while [ $i -le $num_records ]; do
    echo -ne "{\"id\":$i, \"navn\": \"Donald Duck $((i))\", \"alder\": 42, \"adresse\": {\"adresselinjer\": [\"Andeveien $((i))\", \"Andedammen\"], \"postnummer\": \"3158\", \"poststed\": \"Andebu\"}}\n"
    let i=i+1
    done
}

if [ $detached_records -eq 0 ]
then
    # Add array brackets and separate each record by comma
    generate_records | sed '1s/^/[/;$!s/$/,/;$s/$/]/'
else
    generate_records
fi
