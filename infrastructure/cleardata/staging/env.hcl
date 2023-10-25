locals {
  env = "staging"
  alert_email_address = ""
  mattermost_alert_channel = "datadog-alerts-pm-non-prod"
  eks_state_dir = "eks-2022"

  status_queue_name = "stg-ht-status"
  status_dead_letter_queue_name = "stg-ht-status-dead"
  reminder_queue_name = "stg-ht-reminder"
  reminder_dead_letter_queue_name = "stg-ht-reminder-dead"

  notification_queue_name = "notification-requests-staging.fifo"
}