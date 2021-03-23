# Dapla Pseudo Service

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
    "parentUri": "gs://ssb-dataexport-dev-default/datastore",
    "path": "/test/raadata/20201027",
    "version": "1616359002000"
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
  "target": {
    "contentName": "test",
    "contentType": "application/json",
    "password": "kensentme",
    "path": "path/to/export"
  }
}


```
This example exports all columns matching either `**/foedsel` or `**/kontonummer` from a dataset located in `gs://ssb-dataexport-dev-default/datastore/test/raadata/20201027` (with version`1616359002000`).
Columns matching `**/kontonummer` will be depseudonymized using the function `fpe-anychar(secret1)` and then compressed, encrypted and uploaded (as json) to the preconfigured data export bucket (see config).      

Note: The above example only selects a subset of all fields to export. A dataset can however be exported in its entirety by simply omitting any column selectors.
Doing this however would mean a slower export as it would require a full scan of the entire dataset. 


### Pseudonymize JSON file and stream back the result 

```sh
curl 'http://localhost:30950/pseudonymize/file' \
--header "Authorization: Bearer ${dapla_auth_token}" \
--form 'data=@src/test/resources/data/15k.json' \
--form 'request={
  "targetContentType": "application/json",
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
curl 'http://localhost:30950/depseudonymize/file' \
--header "Authorization: Bearer ${dapla_auth_token}" \
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
curl 'http://localhost:30950/depseudonymize/file' \
--header "Authorization: Bearer ${dapla_auth_token}" \
--form 'data=@src/test/resources/data/15k-pseudonymized.json' \
--form 'request={
  "targetUri": "gs://ssb-dataexport-dev-default/export/depseudomized-json.zip",
  "targetContentType": "text/csv",
  "pseudoConfig": {
    "rules": [
      {
        "name": "allthenumbers",
        "pattern": "**/*nummer",
        "func": "fpe-anychar(secret1)"
      }
    ]
  },
  "compression": {
    "password": "kensentme"
  }
}'
```

### Depseudonymize archive with multiple JSON files and download a zipped CSV-file
```sh
curl --output depseudonymized.zip 'http://localhost:30950/depseudonymize/file' \
--header "Authorization: Bearer ${dapla_auth_token}" \
--form 'data=@src/test/resources/data/multiple-json-files.zip' \
--form 'request={
  "targetContentType": "text/csv",
  "pseudoConfig": {
    "rules": [
      {
        "name": "id",
        "pattern": "**/*identifikator*",
        "func": "fpe-fnr(secret1)"
      }
    ]
  },
  "compression": {
    "password": "kensentme"
  }
}'
```


## A note regarding encrypted archives

Standard zip encryption is weak. Thus, for enhanced security, all compressed archives are password encrypted
using AES256. You might need to use a non-standard unzip utility in order to decompress these files. A good
alternative is 7zip.

To unzip using 7zip:
```sh
7z x <my-archive.zip>
```


## Pseudo rules

Pseudo rules are defined by:

* _name_ (used only for logging purposes)
* _pattern_ - [glob pattern](https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob) that matches fields 
  to be (de)/pseudonymized.
* _func_ - references a pseudo function (such as `fpe-anychar`, `fpe-fnr` or `fpe-digits`). The function references the
  pseudo secret to be used.