terraform {
  required_version = ">= 1"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.55.0"
    }
    datadog = {
      source  = "datadog/datadog"
      version = "~> 2.18"
    }
  }
}

provider "aws" {
  profile = var.aws_provider_conf.profile
  region  = var.aws_provider_conf.region
  default_tags {
    tags = {
      app       = var.nc_conf.app
      env       = var.nc_conf.env
      sensitive = "yes"
      service   = var.nc_conf.service
      team      = var.nc_conf.team
    }
  }
}

provider "datadog" {
  api_key = data.aws_secretsmanager_secret_version.datadog_api_key.secret_string
  app_key = data.aws_secretsmanager_secret_version.datadog_app_key.secret_string
}

data "aws_secretsmanager_secret" "datadog_api_key" {
  name = "${var.nc_conf.env}/datadog-api-key"
}

data "aws_secretsmanager_secret_version" "datadog_api_key" {
  secret_id = data.aws_secretsmanager_secret.datadog_api_key.id
}

# There currently is only one value used across all 4 envs
data "aws_secretsmanager_secret" "datadog_app_key" {
  name = "datadog-app-key-${var.nc_conf.service}"
}

data "aws_secretsmanager_secret_version" "datadog_app_key" {
  secret_id = data.aws_secretsmanager_secret.datadog_app_key.id
}