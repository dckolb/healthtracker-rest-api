locals {
  env = "dev-demo"
  alert_email_address = ""
  mattermost_alert_channel = "datadog-alerts-pm-non-prod"

  status_queue_name = "ht-dev-demo-status"
  status_dead_letter_queue_name = "ht-dev-demo-status-dead"
  reminder_queue_name = "ht-dev-demo-reminder"
  reminder_dead_letter_queue_name = "ht-dev-demo-reminder-dead"

  notification_queue_name = "notification-inbound"
}
