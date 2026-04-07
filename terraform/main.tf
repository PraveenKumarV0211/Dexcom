# --- Data sources ---
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}


# --- Security Group ---
resource "aws_security_group" "glucolens" {
  name        = "glucolens-sg"
  description = "GlucoLens app security group"
  vpc_id      = data.aws_vpc.default.id

  # SSH
  ingress {
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "SSH"
  }

  # Frontend
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTP"
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "HTTPS"
  }

  # All outbound
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "glucolens-sg"
    Project = "GlucoLens"
  }
}

# --- IAM Role for EC2 → SNS ---
resource "aws_iam_role" "glucolens_ec2" {
  name = "glucolens-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })

  tags = {
    Project = "GlucoLens"
  }
}

resource "aws_iam_role_policy" "sns_publish" {
  name = "glucolens-sns-publish"
  role = aws_iam_role.glucolens_ec2.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["sns:Publish"]
      Resource = [aws_sns_topic.alerts.arn]
    }]
  })
}

resource "aws_iam_instance_profile" "glucolens" {
  name = "glucolens-instance-profile"
  role = aws_iam_role.glucolens_ec2.name
}

# --- SNS ---
resource "aws_sns_topic" "alerts" {
  name = "glucolens-alerts"

  tags = {
    Project = "GlucoLens"
  }
}

resource "aws_sns_topic_subscription" "email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = var.sns_email
}

# --- Elastic IP ---
resource "aws_eip" "glucolens" {
  instance = aws_instance.glucolens.id
  domain   = "vpc"

  tags = {
    Name    = "glucolens-eip"
    Project = "GlucoLens"
  }
}

# --- EC2 Instance ---
resource "aws_instance" "glucolens" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [aws_security_group.glucolens.id]
  iam_instance_profile   = aws_iam_instance_profile.glucolens.name
  subnet_id              = data.aws_subnets.default.ids[0]

  root_block_device {
    volume_size = 8
    volume_type = "gp3"
  }

  tags = {
    Name    = "glucolens-server"
    Project = "GlucoLens"
  }
}