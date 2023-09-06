locals {
  /*
  Load TF backend variables as TG only allows use of locals in the same hcl file.  Moving the
  variables that are used by TG itself (i.e. state) into their own files allows loading in the env
  folder and here w/out repeating.
  */
  tf_vars = read_terragrunt_config(find_in_parent_folders("tf_state.hcl"))

  # Load AWS values used in env inputs and remote_state
  aws_vars = read_terragrunt_config(find_in_parent_folders("aws.hcl"))

  # Load NC values used in env inputs and remote_state
  app_vars = read_terragrunt_config(find_in_parent_folders("nc-app.hcl"))

  # Load ENV values used in env inputs and remote_state
  env_vars = read_terragrunt_config("env.hcl")

  # Extract the variables we need for easy access
  aws_region      = local.aws_vars.locals.aws_region
  aws_profile     = local.aws_vars.locals.aws_profile
  tf_state_bucket = local.tf_vars.locals.tf_state_bucket
  app_name        = local.app_vars.locals.app
}

remote_state {
  backend = "s3"
  generate = {
    path      = "backend.tf"
    if_exists = "overwrite_terragrunt"
  }
  config = {
    bucket         = "${local.tf_state_bucket}"
    dynamodb_table = "nc-terraform-lock-table"
    encrypt        = true
    # use standardized format of <app>/<env>/terraform.tfstate
    key     = "${local.app_name}/${path_relative_to_include()}/terraform.tfstate"
    profile = "${local.aws_profile}"
    region  = "${local.aws_region}"
  }
}


inputs = {
  nc_conf = {
    env  = local.env_vars.locals.env
    app  = local.app_vars.locals.app
    team = local.app_vars.locals.team
    service = local.app_vars.locals.service
  }

  aws_provider_conf = {
    region  = local.aws_vars.locals.aws_region
    profile = local.aws_vars.locals.aws_profile
  }

  monitor_conf = {
    aws_account_alias        = local.aws_vars.locals.aws_account_alias
    alert_email_address      = local.env_vars.locals.alert_email_address
    mattermost_alert_channel = local.env_vars.locals.mattermost_alert_channel
  }

  read_queues_secrets = [
    "ht-reminder-queue",
    "ht-status-queue"
  ]

  write_queues_secrets = [
    "notification-queue",
    "ht-status-queue"
  ]

  tfstate = {
    aws_profile = local.aws_vars.locals.aws_profile
    aws_region  = local.aws_vars.locals.aws_region
    s3_bucket   = local.tf_vars.locals.tf_state_bucket
    eks_state_dir = try(local.env_vars.locals.eks_state_dir, "eks")
  }
}