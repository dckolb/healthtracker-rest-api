include {
  path = find_in_parent_folders()
}

terraform {
  // include the env agnostic module definition here applying the env specific configs below.
  // The double slash is important to allow all subsequent relative paths to work.
  source = "../..//modules/ht-api"
}
