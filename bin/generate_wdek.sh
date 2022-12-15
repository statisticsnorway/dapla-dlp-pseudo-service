#!/usr/bin/env bash

# Functions -------------------------------------------------------------------

# Show usage
function usage {
    if [ -n "$1" ]
    then
        echo "$1"
    fi

    cat << EOP
Generate a new wrapped data encryption key using tinkey (https://developers.google.com/tink/generate-encrypted-keyset#tinkey)

$0
   -k|--kek-uri        url to key encryption key in KMS, such as gcp-kms://projects/blah/locations/europe-north1/keyRings/my-keyring/cryptoKeys/my-kek-1
   -n|--no-include-kek set if uri to the should not be included in the generated key  (default: off)
   -s|--silent         set if the should not be printed to the console after creation  (default: off)
   -h|--help"
EOP

    exit 0
}


# Variables -------------------------------------------------------------------

# Read environment variables from .env file
if [ -f ".env" ]
then
    if [ $debug -eq 1 ]
    then
      log "Environment variables read from .env"
      grep -v '^#' .env
    fi
    export $(grep -v '^#' .env | xargs)
fi

# timestamp is used for temporary folders and files
timestamp=$(date +%Y%m%d%H%M%S)

# kms_kek_uri is a Cloud KMS URI for the Key Kncryption Key (KEK) that should be used for wrapping/encrypting the generated Data Encryption Key (DEK)
# Example: gcp-kms://projects/<gcp-project-id>/locations/europe-north1/keyRings/<keyring-id>/cryptoKeys/<key-id>
kms_kek_uri=${KMS_KEK_URI}

# wdek_tmpfile is the name of temporary file used to hold the generated WDEK
wdek_filename_tmp=tmp-wdek-${timestamp}.json

# print_wdek=1 means that the Wrapped Data Encryption Key will be printed to the console
print_wdek=1

# include_kek_uri_in_result=1 means that the uri to the kek will be included in the generated key json
include_kek_uri_in_result=1

# Entry point -----------------------------------------------------------------

# Process commandline arguments
while [ "$1" != "" ]
do
    case $1 in
        -k | --kek-uri ) shift
                         kms_kek_uri=$1
                         ;;
        -s | --silent ) print_wdek=0
                         ;;
        -n | --no-include-kek ) include_kek_uri_in_result=0
                         ;;
        -h | --help )    usage
                         exit
                         ;;
        * )              usage
                         exit 1
    esac
    shift
done


# Validate arguments
if [ -z "${kms_kek_uri}" ]
then
  echo "Missing --kek-uri"
  echo "Please specify a Cloud KMS URI for the Key Encryption Key (KEK) that should be used for wrapping/encrypting the generated Data Encryption"
  echo "Example: --kek-uri gcp-kms://projects/<gcp-project-id>/locations/europe-north1/keyRings/<keyring-id>/cryptoKeys/<key-id>"
  exit 1
fi

tinkey create-keyset \
--master-key-uri=${kms_kek_uri} \
--key-template=AES256_SIV \
--out=${wdek_filename_tmp} \
--out-format=json

# Rename temporary file to include primary key id from the generated file
dek_id=$(cat ${wdek_filename_tmp} | jq '.keysetInfo.primaryKeyId')
wdek_filename=wdek-${dek_id}.json
mv ${wdek_filename_tmp} ${wdek_filename}

if [[ include_kek_uri_in_result -eq 1 ]]
then
  jq -c --arg KEK_URI "$kms_kek_uri" '. += {"kekUri":$KEK_URI}' ${wdek_filename} > ${wdek_filename_tmp}
  mv ${wdek_filename_tmp} ${wdek_filename}
fi

if [[ print_wdek -eq 1 ]]
then
  cat ${wdek_filename} | jq
fi

pbcopy < ${wdek_filename}

echo
echo "Generated file ${wdek_filename} - contents copied to your clipboard ✨"