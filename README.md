# Dapla Pseudo Service

(De/)pseudonymization and export endpoints.

## Examples

### Export a dataset
```sh
curl "${root_url}/export" -i -H "Authorization: Bearer ${dapla_auth_token}" --data @export-request.json
```
Where `root_url` points to an instance of the pseudo-service, `dapla_auth_token` is a JWT token and
`export-request.json` is a file containing the request. E.g:

```json
{
  "sourceDataset": {
    "root": "gs://ssb-dev-demo-enhjoern-a-data-produkt",
    "path": "/path/to/data",
    "version": "123"
  },
  "targetContentName": "test",
  "targetContentType":  "application/json",
  "targetPassword": "kensentme",
  "depseudonymize": true,
  "pseudoRules": [
    {
      "name": "kontonummer",
      "pattern": "**/kontonummer",
      "func": "fpe-anychar(secret1)"
    }
  ]
}
```

This example exports all columns matching either `**/foedsel` or `**/kontonummer` from a dataset located in a GCS
bucket at `gs://ssb-dev-demo-enhjoern-a-data-produkt/path/to/data/123`.
Columns matching `**/kontonummer` will be depseudonymized using the function `fpe-anychar(secret1)` and then compressed,
encrypted and uploaded (as json) to the preconfigured data export bucket (see config).      

Note that the above will export all data. If you only need a subset of fields, you can specify this with column selector
glob expressions, like so:
```
  "columnSelectors": [
    "**/foedsel*",
    "**/kontonummer"
  ]
```


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

### Depseudonymize JSON file and upload to google cloud storage as zipped CSV-file
```sh
curl "${root_url}/depseudonymize/file" \
--header "Authorization: Bearer ${dapla_auth_token}" \
--form 'data=@src/test/resources/data/15k-pseudonymized.json' \
--form 'request={
  "targetUri": "gs://ssb-dev-demo-enhjoern-a-data-export/path/to/depseudonymized-csv.zip",
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
