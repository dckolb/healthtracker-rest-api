locals {
  env = "dev"
  alert_email_address = ""
  mattermost_alert_channel = "datadog-alerts-pm-non-prod"
  eks_state_dir = "eks-2022"

  status_queue_name = "ht-dev-status"
  status_dead_letter_queue_name = "ht-dev-status-dead"
  reminder_queue_name = "ht-dev-reminder"
  reminder_dead_letter_queue_name = "ht-dev-reminder-dead"

  notification_queue_name = "notification-requests-dev.fifo"
}
