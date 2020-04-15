resource "aws_dynamodb_table" "coapi-dynamodb-table" {
  name           = "Reports"
  read_capacity  = 20
  write_capacity = 20
  hash_key       = "did"
  range_key      = "reportTimestamp"

  attribute {
    name = "did"
    type = "S"
  }

  attribute {
    name = "reportTimestamp"
    type = "N"
  }

  tags = {
    Name        = var.appName
    Environment = var.env
  }
}

resource "aws_dynamodb_table" "tcn-dynamodb-table" {
  name           = "TCNReports"
  read_capacity  = 20
  write_capacity = 20
  hash_key       = "reportId"
  range_key      = "randomId"

  attribute {
    name = "reportId"
    type = "S"
  }

  attribute {
    name = "randomId"
    type = "S"
  }

  tags = {
    Name        = var.appName
    Environment = var.env
  }
}
