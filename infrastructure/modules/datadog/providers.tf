terraform {
  required_version = ">=0.14"

  required_providers {
    datadog = {
      source  = "datadog/datadog"
      version = "~> 2.18"
    }
  }
}