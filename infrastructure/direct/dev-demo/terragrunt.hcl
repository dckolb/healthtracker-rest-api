//TODO remove this and figure out how to integrate demo to the rest of the env - see ticket HT-4396

// include the state file
include {
  path = find_in_parent_folders()
}

terraform {
  // include the env agnostic module definition here applying the env specific configs below.
  // The double slash is important to allow all subsequent relative paths to work.
  source = "..//modules/ht-api"
}

locals {
  app_vars          = read_terragrunt_config(find_in_parent_folders("nc-app.hcl"))
  aws_vars          = read_terragrunt_config("aws.hcl")
  state_bucket_vars = read_terragrunt_config("tf_state.hcl")

  // Convert to simpler form for convenience
  aws_profile                     = local.aws_vars.locals.aws_profile
  aws_region                      = local.aws_vars.locals.aws_region
  aws_account_alias               = local.aws_vars.locals.aws_account_alias
  sms_monthly_account_spend_limit = local.aws_vars.locals.sms_monthly_account_spend_limit

  env = "demo"
}

inputs = {
  reminder-queue-name = "ht-demo-reminder"
  reminder-fifo       = true
  status-queue-name   = "ht-demo-status"
  status-fifo         = false
  profile             = "${local.aws_profile}"
  region              = "${local.aws_region}"

  nc_conf = {
    app     = local.app_vars.locals.app
    env     = local.env
    service = local.app_vars.locals.service
    team    = local.app_vars.locals.team
  }

  aws_provider_conf = {
    region  = local.aws_region
    profile = local.aws_profile
  }
}
