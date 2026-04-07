output "public_ip" {
  description = "Server public IP"
  value       = aws_eip.glucolens.public_ip
}

output "sns_topic_arn" {
  description = "SNS topic ARN"
  value       = aws_sns_topic.alerts.arn
}

output "ssh_command" {
  description = "SSH into server"
  value       = "ssh -i ~/.ssh/${var.key_name}.pem ubuntu@${aws_eip.glucolens.public_ip}"
}

output "app_url" {
  description = "Application URL"
  value       = "http://${aws_eip.glucolens.public_ip}"
}