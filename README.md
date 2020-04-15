# CoEpi Server on AWS

This repo contains server and infrastructure code for deploying and running
CoEpi Cloud API on AWS.

* **Compute**: AWS Lambda
* **Data Store**: DynamoDB
* **Routing and Load Balancing**: API Gateway
* **Permissions Management**: IAM

## Build

JDK 11 or newer required

```sh
gradle build
```

Then, run:

```sh
./gradlew shadowJar
```

## AWS Deploy via Terraform

1. Ensure you have the AWS CLI configured, working, and pointing to the default AWS
   account you wish to deploy to.
2. [Install Terraform](https://www.terraform.io/downloads.html) 0.12 or newer
   (Recommendation is to install via Choco for Windows/Homebrew for macOS/package managers for
   *nix)
3. Run the Build step above
4. `cd` to the `terraform` folder in this repo
5. Create an S3 bucket to store Terraform's statefile: `aws s3api create-bucket --bucket tf-coepi-backend-bucket --region us-east-1`
6. Run `terraform init -backend-config="region=us-east-1"` to tell Terraform to
   use the state bucket you created in `us-east-1`
7. Run `terraform plan` to see what changes will be applied to
   your AWS account
8. Run `terraform apply -auto-approve` to make the changes and deploy the
   server.
9. When the Terraform scripts are updated or you wish to redeploy, repeat steps
   7 and 8.

The API Gateway root URL will be echoed to the shell, and you can CURL the
deployed API:

```sh
curl -X POST https://q69c4m2myb.execute-api.us-west-2.amazonaws.com/v3/cenreport -d '{ "report": "dWlyZSBhdXRob3JgdsF0aW9uLgo=", "cenKeys": [ "baz", "das" ]}'

curl -X GET https://q69c4m2myb.execute-api.us-west-2.amazonaws.com/v3/cenreport
[{"did":"2020-04-06","reportTimestamp":1586157667433,"report":"dWlyZSBhdXRob3JpemF0aW9uLgo=","cenKeys":["bar","foo"]},{"did":"2020-04-06","reportTimestamp":1586158348099,"report":"dWlyZSBhdXRob3JpemF0aW9uLgo=","cenKeys":["bar","foo"]},{"did":"2020-04-06","reportTimestamp":1586158404001,"report":"dWlyZSBhdXRob3JgdsF0aW9uLgo=","cenKeys":["baz","das"]}]
```

## Running

For running the server on your developer AWS account, follow the steps
below under ***AWS Insfrastructure Setup*** section.

For testing the lambda function and API locally, you can use SAM CLI. Below docs should be helpful.

https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html

https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-using-invoke.html 

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
