variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-2"
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.small"
}

variable "key_name" {
  description = "SSH key pair name"
  type        = string
}

variable "ami_id" {
  description = "AMI ID for EC2"
  type        = string
}

variable "sns_email" {
  description = "Email for SNS alerts"
  type        = string
  default     = "vpraveenkumar0211@gmail.com"
}

variable "db_host" {
  description = "MongoDB Atlas URI"
  type        = string
  sensitive   = true
}

variable "dexcom_client_id" {
  description = "Dexcom API client ID"
  type        = string
  sensitive   = true
}

variable "dexcom_client_secret" {
  description = "Dexcom API client secret"
  type        = string
  sensitive   = true
}

variable "grok_token" {
  description = "Groq API key"
  type        = string
  sensitive   = true
}

variable "gemini_key" {
  description = "Gemini API key"
  type        = string
  sensitive   = true
}

variable "hf_token" {
  description = "Hugging Face token"
  type        = string
  sensitive   = true
}