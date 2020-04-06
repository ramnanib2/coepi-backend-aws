# CoEpi Server on AWS

This repo contains server and infrastructure code for deploying and running
CoEpi Cloud API on AWS.

* **Compute**: AWS Lambda
* **Data Store**: DynamoDB
* **Routing and Load Balancing**: API Gateway
* **Permissions Management**: IAM


## Build


```
gradle build
```

Then, run:

```
./gradlew shadowJar
```


## Running

TODO: Add steps for executing on Lambda


## Documentation for API Endpoints

Swagger Definition and API documentation is located
under [**api_definition**](api_definition/coepi_api_0.3.0.yml) folder:

The API can be tested by pasting the definition on [Swagger Editor](http://editor.swagger.io/)

### v3
Method | HTTP request | Description
------------- | ------------- | -------------
[**cenreportPost**](docs/DefaultApi.md#cenreportpost) | **POST** /cenreport | Submit symptom or infection report
[**cenreportTimestampLowerTimestampUpperGet**](docs/DefaultApi.md#cenreporttimestamplowertimestampupperget) | **GET** /cenreport?timestampLower={tsLower}&timestampUpper={tsUpper} | Returns a list of reports generated between a timestamp range


### TO DO

1. Add documentation for running on AWS Lambda
2. Add a cloud formation template for automated deployment of the infrastructure in a new AWS account
