locals {
  env = "prod"
  alert_email_address = ""
  mattermost_alert_channel = "datadog-alerts-pm-prod"
  eks_state_dir = "eks-2022"

  status_queue_name = "prod-ht-status"
  status_dead_letter_queue_name = "prod-ht-status-dead"
  reminder_queue_name = "prod-ht-reminder"
  reminder_dead_letter_queue_name = "prod-ht-reminder-dead"

  notification_queue_name = "notification-requests-prod.fifo"
}