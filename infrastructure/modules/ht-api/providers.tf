terraform {
  required_version = ">= 1"

  required_providers {
    aws = {
      # pinned to 3.15 until https://github.com/hashicorp/terraform-provider-aws/issues/19318 is resolved
      source  = "hashicorp/aws"
      version = "3.58"
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
