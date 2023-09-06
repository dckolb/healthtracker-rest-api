# values common to this service across all environments
locals {
  app  = "health_tracker"
  service = "ht-api"
  team = "patient-monitoring"

  read_queues_secrets = [
    "ht-reminder-queue",
    "ht-status-queue"
  ]

  write_queues_secrets = [
    "notification-queue",
    "ht-status-queue"
  ]
}
