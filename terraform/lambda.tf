resource "aws_iam_role" "coepi_lambda_backend_role" {
  name = "iam_for_coepi_lambda"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

locals {
  jarfile = "../build/libs/coepi-backend-kotlin-1.0.0-all.jar"
}

resource "aws_lambda_function" "coepi_lambda" {
  filename      = local.jarfile
  function_name = "CoEpiServerLambda"
  role          = aws_iam_role.coepi_lambda_backend_role.arn
  handler       = "org.coepi.api.CoEpiHandler::handleRequest"

  source_code_hash = filebase64sha256(local.jarfile)

  runtime = "java11"
}

//TODO these policy perms could be tightened.
resource "aws_iam_policy" "lambda_dynamodb_access" {
  name        = "coepi_lambda_dynamodb_policy"
  path        = "/"
  description = "IAM policy for DynamoDB access from lambda"

  policy = <<EOF
{
    "Version": "2012-10-17",
        "Statement": [
            {
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
EOF
}

resource "aws_iam_role_policy_attachment" "lambda_dynamodb_policy_attachment" {
  role       = aws_iam_role.coepi_lambda_backend_role.name
  policy_arn = aws_iam_policy.lambda_dynamodb_access.arn
}

resource "aws_iam_role_policy_attachment" "lambda_cloudwatch_policy_attachment" {
  role       = aws_iam_role.coepi_lambda_backend_role.name
  policy_arn = var.cloudwatch_policy_arn
}

resource "aws_lambda_permission" "lambda_apigateway" {
   statement_id  = "AllowAPIGatewayInvoke"
   action        = "lambda:InvokeFunction"
   function_name = aws_lambda_function.coepi_lambda.function_name
   principal     = "apigateway.amazonaws.com"

   # The "/*/*" portion grants access from any method on any resource
   # within the API Gateway REST API.
   source_arn = "${aws_api_gateway_rest_api.coepi_api_gateway.execution_arn}/*/*"
 }
