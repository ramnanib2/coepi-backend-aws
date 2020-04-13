data "template_file" "coepi_api_swagger" {
  template = file(var.api_spec_path)

  # #Pass the varible value if needed in swagger file
  vars = {
   lambda_invoke_arn = aws_lambda_function.coepi_lambda.invoke_arn
  }
}

resource "aws_api_gateway_rest_api" "coepi_api_gateway" {
  name        = "coepi_api_gateway"
  description = "API Gateway for CoEPI backend"
  body        = data.template_file.coepi_api_swagger.rendered
}

resource "aws_api_gateway_deployment" "lambda_gateway" {
  rest_api_id = aws_api_gateway_rest_api.coepi_api_gateway.id
  stage_name  = "v3"
}
