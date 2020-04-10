variable "region" {
  type = string
  description = "The AWS region to deploy to"
}

variable "appName" {
  type = string
  description = "Name of this app"
  default = "CoEpi-AWS-Backend"
}

variable "env" {
  type = string
  description  = "Name of the environment class this app is deployed to (staging, test, prod, etc)"
}

variable "api_spec_path" {
  type = string
  description = "Path to the API Swagger/OpenAPI definition being used"
}

variable "cloudwatch_policy_arn" {
  type = string
  description = "ARN for the Lambda cloudwatch access policy"
  default = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}
