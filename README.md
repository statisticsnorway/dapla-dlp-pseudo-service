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

### Pseudonymize JSON file

```sh
curl -X POST 'http://localhost:8080/depseudonymize/file' \
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

### Depseudonymize JSON file, output CSV

```sh
curl -X POST 'http://localhost:8080/depseudonymize/file' \
--header 'Accept: text/csv' \
--form 'data=@src/test/resources/data/15k-pseudonymized.json' \
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

Note that you can specify output format in the Accept header. 
Valid values: `application/json`, `text/csv`