# Manual AWS Infrastructure Setup

> NOTE: Please do not use or rely on this manual setup method - it is important to
> encode all infrastructure assumptions into [Terraform](./terraform) for the
> sake of maintainability and provision AWS using that.

You need to be logged in to the AWS console as admin or as a user that has permissions to create, update 
DynamoDB, Lambda and API Gateway resources along with roles and policies.

Select an appropriate region from the top right corner of the AWS Console based on proximity to users.
For example, ***Oregon (us-west-2)***.
All the resources will be created in this region.
 
### Set up DynamoDB Table

1. Select DynamoDB service from AWS Console
2. Select **Create Table**. 
3. **Name**: Reports
4. **Partition Key**: did, **Type**: String
5. Turn on the **Add sort key** check mark.
6. **Sort Key**: reportTimestamp, **Type**: Number 
7. Hit **Create**.

**Note**: Table schema is subject to change. Please refer to the schema 
as defined [here](src/main/kotlin/org/coepi/api/dao/ReportItem.kt) before creating the table.

Refer to this documentation while following the below steps. We're pretty much doing the same thing:

https://dzone.com/articles/calling-lambda-function-through-aws-api-gateway

### Create Lambda Function

1. Select AWS Lambda from the console and click on **Create Function** on the top-right corner
2. Make sure **Author from scratch** option is selected.
3. Write function name as **CoEpiServerLambda**.
4. Choose Runtime to be **Java8**
5. Keeping everything else as default, hit **Create Funtion**.
6. Under this locally downloaded git repo, run ```./gradlew shadowJar```. This will 
create a fat jar of the service's business logic under ```build/libs/<project_name>-all.jar```
7. On the lambda console, go to the **Function Code** section and upload the jar file that was created above.
8. Keep runtime to be **Java 8** and paste this in the handler section: ```org.coepi.api.CoEpiHandler::handleRequest```
9. Hit **Save** on the top-right corner of the console.
10. Wait for the function to be created.

### Provide the lambda function permissions to talk to DynamoDB

1. On a separate tab select AWS Service **IAM** from the services menu
2. From the left pane, select **Policies**.
3. Click on **Create Policy** and select **JSON** tab.
4. Paste the below policy, go to **Review Policy**, provide a name (ex, **CoEpiLambdaPolicy**) and an appropriate description.
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": [
                "dynamodb:DeleteItem",
                "dynamodb:DescribeContributorInsights",
                "dynamodb:RestoreTableToPointInTime",
                "dynamodb:ListTagsOfResource",
                "dynamodb:UpdateContributorInsights",
                "dynamodb:UpdateContinuousBackups",
                "dynamodb:TagResource",
                "dynamodb:DescribeTable",
                "dynamodb:GetItem",
                "dynamodb:DescribeContinuousBackups",
                "dynamodb:BatchGetItem",
                "dynamodb:UpdateTimeToLive",
                "dynamodb:BatchWriteItem",
                "dynamodb:ConditionCheckItem",
                "dynamodb:UntagResource",
                "dynamodb:PutItem",
                "dynamodb:Scan",
                "dynamodb:Query",
                "dynamodb:DescribeStream",
                "dynamodb:UpdateItem",
                "dynamodb:DescribeTimeToLive",
                "dynamodb:DescribeGlobalTableSettings",
                "dynamodb:GetShardIterator",
                "dynamodb:DescribeGlobalTable",
                "dynamodb:RestoreTableFromBackup",
                "dynamodb:DescribeBackup",
                "dynamodb:GetRecords",
                "dynamodb:DescribeTableReplicaAutoScaling"
            ],
            "Resource": "*"
        },
        {
            "Sid": "VisualEditor1",
            "Effect": "Allow",
            "Action": [
                "dynamodb:DescribeReservedCapacityOfferings",
                "dynamodb:DescribeReservedCapacity",
                "dynamodb:PurchaseReservedCapacityOfferings",
                "dynamodb:DescribeLimits",
                "dynamodb:ListStreams"
            ],
            "Resource": "*"
        }
    ]
}
```
* On the policy list panel, select the **CoEpiLambdaPolicy**, click on **Policy Actions**, then **Attach**.
* On the search bar, type the name of the lambda function (**CoEpiServerLambda**)
* Select the role that comes up that that name as the prefix and hit **Attach Policy**.

### Set up API gateway routes to Lambda function 

1. Select API Gateway from the AWS console service menu.
2. Select **REST API** and click on **Build**.
3. Select **Import from Swagger or Open API 3**
4. Paste the [**api_definition**](api_definition/coepi_api_0.3.0.yml)
5. Click on **Import**. This will bring you to a panel that lists the API paths as described in the definition.
6. For each path **GET** and **POST**, follow the below steps.
7. **Integration type**: Lambda Function
8. Mark checked on **Use Lambda Proxy integration**
9. Select the region to be the same one where the Lambda function is deployed (eg. us-west-2 for Oregon).
10. **Lambda Function**: CoEpiServerLambda
11. Mark checked on **Use Default Timeout** and hit **Save** and then **OK** on the 'give permission...' prompt.
12. You can use [**this guide**](https://docs.aws.amazon.com/apigateway/latest/developerguide/how-to-test-method.html) 
to test the service
13. Click on **Actions** next to Resources and hit **Deploy API**.
14. **Deployment stage**: [New Stage]
15. **Stage Name**: v3
16. Click Save
17. This will create publicly reachable endpoints.
18. Test the deployed API's using curl. Below is the example for **v3**

```
curl -X POST https://q69c4m2myb.execute-api.us-west-2.amazonaws.com/v3/cenreport -d '{ "report": "dWlyZSBhdXRob3JgdsF0aW9uLgo=", "cenKeys": [ "baz", "das" ]}'

curl -X GET https://q69c4m2myb.execute-api.us-west-2.amazonaws.com/v3/cenreport
[{"did":"2020-04-06","reportTimestamp":1586157667433,"report":"dWlyZSBhdXRob3JpemF0aW9uLgo=","cenKeys":["bar","foo"]},{"did":"2020-04-06","reportTimestamp":1586158348099,"report":"dWlyZSBhdXRob3JpemF0aW9uLgo=","cenKeys":["bar","foo"]},{"did":"2020-04-06","reportTimestamp":1586158404001,"report":"dWlyZSBhdXRob3JgdsF0aW9uLgo=","cenKeys":["baz","das"]}]
``` 
