# Dapla Pseudo Service

!!! work in progress !!!

(De/)pseudonymization endpoints

## Development notes
* This project requires JDK14 with preview features enabled. 
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
