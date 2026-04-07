packer {
  required_plugins {
    amazon = {
      version = ">= 1.2.0"
      source  = "github.com/hashicorp/amazon"
    }
  }
}

variable "aws_region" {
  type    = string
  default = "us-east-2"
}

variable "instance_type" {
  type    = string
  default = "t3.small"
}

source "amazon-ebs" "glucolens" {
  ami_name      = "glucolens-{{timestamp}}"
  instance_type = var.instance_type
  region        = var.aws_region

  source_ami_filter {
    filters = {
      name                = "ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }
    most_recent = true
    owners      = ["099720109477"]
  }

  ssh_username = "ubuntu"

  tags = {
    Name    = "GlucoLens"
    Project = "GlucoLens"
    Built   = "{{timestamp}}"
  }
}

build {
  sources = ["source.amazon-ebs.glucolens"]

  # System dependencies
  provisioner "shell" {
    inline = [
      "sudo apt-get update",
      "sudo apt-get install -y docker.io docker-compose-plugin openjdk-17-jdk-headless nginx",
      "sudo systemctl enable docker",
      "sudo usermod -aG docker ubuntu"
    ]
  }

  # App directory
  provisioner "shell" {
    inline = [
      "sudo mkdir -p /opt/glucolens",
      "sudo chown ubuntu:ubuntu /opt/glucolens"
    ]
  }

  # Docker Compose
  provisioner "file" {
    source      = "../docker-compose.yml"
    destination = "/opt/glucolens/docker-compose.yml"
  }

  # Backend JAR
  provisioner "file" {
    source      = "../target/demo-0.0.1-SNAPSHOT.jar"
    destination = "/opt/glucolens/glucolens.jar"
  }

  # Frontend build
  provisioner "file" {
    source      = "../front-end/dist/"
    destination = "/opt/glucolens/frontend"
  }

  # Systemd service for Spring Boot
  provisioner "shell" {
    inline = [
      <<-EOF
      sudo tee /etc/systemd/system/glucolens.service > /dev/null <<'SERVICE'
      [Unit]
      Description=GlucoLens Spring Boot
      After=network.target docker.service
      Requires=docker.service

      [Service]
      Type=simple
      User=ubuntu
      WorkingDirectory=/opt/glucolens
      EnvironmentFile=/opt/glucolens/.env
      ExecStartPre=/usr/bin/docker compose -f /opt/glucolens/docker-compose.yml up -d
      ExecStartPre=/bin/sleep 15
      ExecStart=/usr/bin/java -Xmx384m -jar /opt/glucolens/glucolens.jar
      Restart=always
      RestartSec=10

      [Install]
      WantedBy=multi-user.target
      SERVICE
      sudo systemctl daemon-reload
      sudo systemctl enable glucolens.service
      EOF
    ]
  }

  # Nginx config
  provisioner "shell" {
    inline = [
      <<-EOF
      sudo tee /etc/nginx/sites-available/glucolens > /dev/null <<'NGINX'
      server {
          listen 80;
          server_name _;

          location /api/ {
              proxy_pass http://localhost:8999;
              proxy_set_header Host $host;
              proxy_set_header X-Real-IP $remote_addr;
              proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
              proxy_read_timeout 90;
          }

          location / {
              root /opt/glucolens/frontend;
              try_files $uri $uri/ /index.html;
          }
      }
      NGINX
      sudo ln -sf /etc/nginx/sites-available/glucolens /etc/nginx/sites-enabled/default
      sudo nginx -t
      EOF
    ]
  }
}