# Dapla Pseudo Service

(De/)pseudonymization and export endpoints.

Browse the API docs as:
* [Redoc](https://dapla-pseudo-service.staging-bip-app.ssb.no/api-docs/redoc) (recommended)
* [Rapidoc](https://dapla-pseudo-service.staging-bip-app.ssb.no/api-docs/rapidoc)
* [Swagger UI](https://dapla-pseudo-service.staging-bip-app.ssb.no/api-docs/swagger-ui)

... or parse [the Open API specs](https://dapla-pseudo-service.staging-bip-app.ssb.no/api-docs/dapla-pseudo-service-1.0.yml) for yourself 🤓

## Examples

### Pseudonymize JSON file and stream back the result 

```sh
curl "${root_url}/pseudonymize/file" \
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
curl "${root_url}/depseudonymize/file" \
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

### Depseudonymize JSON file and download a zipped CSV-file
```sh
curl "${root_url}/depseudonymize/file" \
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
  },
  "compression": {
    "password": "kensentme"
  }
}'
```

### Depseudonymize archive with multiple JSON files and download a zipped CSV-file
```sh
curl --output depseudonymized.zip "${root_url}/depseudonymize/file" \
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


## Development

See `Makefile` for details/examples of common dev tasks.
```
build-all                      Build all and create docker image
build-mvn                      Build project and install to you local maven repo
build-docker                   Build dev docker image
run-local                      Run the app locally (without docker)
release-dryrun                 Simulate a release in order to detect any issues
release                        Release a new version. Update POMs and tag the new version in git
```
