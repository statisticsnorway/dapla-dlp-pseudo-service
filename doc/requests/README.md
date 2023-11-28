# HTTP Client tests

This folder contains files for Jetbrains' 
[HTTP Client plugin](https://www.jetbrains.com/help/idea/2023.1/http-client-in-product-code-editor.html). With these 
you can create, edit, and execute HTTP requests directly in the IntelliJ IDEA code editor.

To be able to run these requests you should create an environment file called `http-client.env.json` 
inside this folder. For security reasons **this file should not be checked into git**. The file should have
the following structure:

```
{
  "local": {
    "base_url": "localhost:10210",
    "keycloak_token": "..."
  },
  "staging": {
    "base_url": "localhost:10210",
    "keycloak_token": "..."
  },
  "prod": {
    "base_url": "localhost:10210",
  }
}
```
Note that the `keycloak_token` is sensitive, and should not be shared with others or checked into git.