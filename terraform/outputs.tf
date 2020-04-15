output "gateway_base_url" {
  value = "${aws_api_gateway_deployment.coepi_lambda_gateway.invoke_url}/cenreport"
}

output "tcn_base_url" {
  value = "${aws_api_gateway_deployment.tcn_lambda_gateway.invoke_url}/tcnreport"
}
