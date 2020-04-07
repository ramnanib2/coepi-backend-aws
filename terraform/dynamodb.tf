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

  attribute {
    name = "cenKeys"
    type = "SS"
  }

  attribute {
    name = "report"
    type = "B"
  }

  tags = {
    Name        = var.appName
    Environment = var.env
  }
}
