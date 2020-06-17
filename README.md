# Dapla Pseudo Service

!!! work in progress !!!

(De/)pseudonymization endpoints

## Curl example

```sh
curl --location --request POST 'http://localhost:8080/depseudonymize/file' \
--header 'Accept: text/csv' \
--form 'data=@src/test/resources/data/15k.json' \
--form 'request={
  "pseudoConfig": {
    "rules": [
      {
        "name": "id",
        "pattern": "**/*nummer",
        "func": "fpe-anychar(secret1)"
      }
    ]
  }
}'
```
Notice that you can specify output format in the Accept header. 
Valid values: 'application/json', 'text/csv'