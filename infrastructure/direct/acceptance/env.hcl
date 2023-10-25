locals {
  env = "acceptance"
  alert_email_address = ""
  mattermost_alert_channel = "datadog-alerts-pm-non-prod"
  eks_state_dir = "eks-2022"

  status_queue_name = "ht-status"
  status_dead_letter_queue_name = "ht-status-dead"
  reminder_queue_name = "ht-reminder"
  reminder_dead_letter_queue_name = "ht-reminder-dead"

  notification_queue_name = "notification-requests-acceptance.fifo"
}
