output "gateway_base_url" {
  value = aws_api_gateway_deployment.lambda_gateway.invoke_url
}
