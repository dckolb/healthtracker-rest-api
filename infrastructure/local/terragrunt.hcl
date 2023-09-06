//TODO do we need this

include {
  path = find_in_parent_folders()
}

terraform {
  source = "..//modules/ht-api"
}

locals {
  app_vars     = read_terragrunt_config(find_in_parent_folders("app.hcl"))
  aws_vars     = read_terragrunt_config("aws.hcl")
  state_vars   = read_terragrunt_config("tf_state.hcl")
  aws_endpoint = get_env("AWS_ENDPOINT", "http://localhost:4566")
}

remote_state {
  backend = "s3"
  generate = {
    path      = "backend.tf"
    if_exists = "overwrite_terragrunt"
  }

  config = {
    bucket         = "${local.state_vars.locals.tf_state_bucket}"
    dynamodb_table = "nc-terraform-lock-table"

    # use standardized format of <service>/<env>/terraform.tfstate
    key     = "${local.app_vars.locals.service}/${path_relative_to_include()}/terraform.tfstate"
    profile = "${local.aws_vars.locals.aws_profile}"
    region  = "${local.aws_vars.locals.aws_region}"

    // Use localstack endpoints for remote state management
    dynamodb_endpoint = local.aws_endpoint
    endpoint          = local.aws_endpoint
    force_path_style  = true
  }
}

generate "modules" {
  path = "main.tf"
  if_exists = "overwrite"
  contents = ""
}

# Override AWS provider to use localstack
generate "provider" {
  path = "providers.tf"
  if_exists = "overwrite"
  contents = <<EOF
  provider "aws" {
    region                      = "${local.aws_vars.locals.aws_region}"

    skip_credentials_validation = true
    skip_metadata_api_check     = true
    skip_requesting_account_id  = true
    s3_force_path_style         = true

    endpoints {
      cloudformation  = "${local.aws_endpoint}"
      cloudwatch     = "${local.aws_endpoint}"
      dynamodb       = "${local.aws_endpoint}"
      ec2            = "${local.aws_endpoint}"
      iam            = "${local.aws_endpoint}"
      kinesis        = "${local.aws_endpoint}"
      kms            = "${local.aws_endpoint}"
      lambda         = "${local.aws_endpoint}"
      redshift       = "${local.aws_endpoint}"
      route53        = "${local.aws_endpoint}"
      s3             = "${local.aws_endpoint}"
      ses            = "${local.aws_endpoint}"
      sns            = "${local.aws_endpoint}"
      sqs            = "${local.aws_endpoint}"
      ssm            = "${local.aws_endpoint}"
      sts            = "${local.aws_endpoint}"
      stepfunctions  = "${local.aws_endpoint}"
    }
  }
EOF
}
