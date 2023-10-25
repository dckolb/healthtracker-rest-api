variable "queue_names" {
  type = list(string)
}

variable "dead_letter_queue_names" {
  type = list(string)
}

variable "monitor_conf" {
  description = "Wrapper defining all the attributes expected for the datadog module"
  type = object({
    # The name of the Mattermost channel to send monitor notifications to.
    # See https://docs.datadoghq.com/integrations/slack/?tab=slackapplicationus#-mentions-in-slack-from-monitor-alert for format.
    # This must be configured in https://navigatingcancer.datadoghq.com/account/settings#integrations/slack
    mattermost_alert_channel = string
    # The email address to use for notifications
    alert_email_address = string
    # The "aws_account_alias" tag value used to identify the account.  Used for account level metrics like sms spend.
    aws_account_alias = string
  })
}

variable "nc_conf" {
  description = "The NC env/app/team"
  type = object({
    service  = string
    env  = string
    app  = string
    team = string
  })
}

