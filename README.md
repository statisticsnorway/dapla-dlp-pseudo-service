# Dapla Pseudo Service

!!! work in progress !!!

(De/)pseudonymization endpoints

## Development notes
* This project requires JDK15. 
* Specify `-Dmicronaut.environments=local` when running the app locally
in order to initialize the app with local application configuration.

See `Makefile` for details/examples of common dev tasks.
```
build-all                      Build all and create docker image
build-mvn                      Build project and install to you local maven repo
build-docker                   Build dev docker image
run-local                      Run the app locally (without docker)
release-dryrun                 Simulate a release in order to detect any issues
release                        Release a new version. Update POMs and tag the new version in git
```

## Curl examples

### Export a dataset
```sh
curl 'http://localhost:8080/export' -i -H "Authorization: Bearer ${dapla_auth_token}" --data @export-request.json
```
Where `dapla_auth_token` is a jwt token and `export-request.json` is a file containing the request. E.g:

```json
{
  "datasetIdentifier": {
    "parentUri": "gs://ssb-data-prod-kilde-ske-freg/datastore",
    "path": "kilde/ske/freg/person/rådata/v1.4-20200819",
    "version": "1598522534000"
  },
  "columnSelectors": [
    "**/foedsel",
    "**/kontonummer"
  ],
  "pseudoConfig": {
    "rules": [
      {
        "name": "kontonummer",
        "pattern": "**/kontonummer",
        "func": "fpe-anychar(secret1)"
      }
    ]
  },
  "compression": {
    "type": "application/zip",
    "encryption": "AES",
    "password": "kensentme"
  },
  "targetUri": "gs://ssb-dataexport-prod-default/freg/29092020/person-foedsel.zip",
  "targetContentType": "application/json"
}
```
This example exports all columns matching either `**/foedsel` or `**/kontonummer` from a dataset located in `gs://ssb-data-prod-kilde-ske-freg/datastore/kilde/ske/freg/person/rådata/v1.4-20200819/1598522534000`.
Columns matching `**/kontonummer` will be depseudonymized using the function `fpe-anychar(secret1)` and then compressed, encrypted and uploaded (as json) to `gs://ssb-dataexport-prod-default/freg/29092020/person-foedsel.zip`.      

Note: The above example only selects a subset of all fields to export. A dataset can however be exported in its entirety by simply omitting any column selectors.
Doing this however would mean a slower export as it would require a full scan of the entire dataset. 


### Pseudonymize JSON file and stream back the result 

```sh
curl 'http://localhost:8080/pseudonymize/file' \
--form 'data=@src/test/resources/data/15k.json' \
--form 'request={
  "pseudoConfig": {
    "rules": [
      {
        "name": "allthenumbers",
        "pattern": "**/*nummer",
        "func": "fpe-anychar(secret1)"
      }
    ]
  }
}'
```

### Depseudonymize JSON file and stream back the result as CSV

```sh
curl 'http://localhost:8080/depseudonymize/file' \
--form 'data=@src/test/resources/data/15k-pseudonymized.json' \
--form 'request={
  "targetContentType": "text/csv",
  "pseudoConfig": {
    "rules": [
      {
        "name": "allthenumbers",
        "pattern": "**/*nummer",
        "func": "fpe-anychar(secret1)"
      }
    ]
  }
}'
```

### Depseudonymize JSON file and upload to google cloud storage as a zipped CSV-file
```sh
curl 'http://localhost:8080/depseudonymize/file' \
--form 'data=@src/test/resources/data/15k-pseudonymized.json' \
--form 'request={
  "targetUri": "gs://my-bucket/depseudomized-csv.zip",
  "targetContentType": "text/csv",
  "pseudoConfig": {
    "rules": [
      {
        "name": "id",
        "pattern": "**/*identifikator*",
        "func": "fpe-fnr(pseudo-secret-sirius-person-fastsatt)"
      }
    ]
  },
  "compression": {
        "type": "application/zip",
        "encryption": "AES",
        "password": "kensentme"
  }
}'
```
