# see https://registry.terraform.io/providers/DataDog/datadog/latest/docs/resources/monitor for configuration details

locals {
  # General configs
  dd_service_name = "health-tracker.rest"
  seconds_per_min = 60
  dd_trace_url    = "https://navigatingcancer.datadoghq.com/apm/service/${local.dd_service_name}/trace.annotation?env=${var.nc_conf.env}&paused=false"

  # renotify every hour until resolved
  notification_interval_mins_until_resolved = 60

  # Monitor configs
  dlq_runbook_url  = "https://navigatingcancer.atlassian.net/wiki/spaces/HE/pages/2902884420/Runbook+-+Health+Tracker+REST+API#dead-letter-queue"
  dlq_runbook_link = "[Runbook for dead letter queues receiving a message](${local.dlq_runbook_url})"

  queue_age_runbook_url  = "https://navigatingcancer.atlassian.net/wiki/spaces/HE/pages/2902884420/Runbook+-+Health+Tracker+REST+API#queue-age"
  queue_age_runbook_link = "[Runbook for queues message age](${local.queue_age_runbook_url})"
  queue_age_slo_mins     = 10
  queue_age_slo_secs     = local.queue_age_slo_mins * local.seconds_per_min

  nc_standard_tags = [
    "service:${var.nc_conf.service}",
    "app:${var.nc_conf.app}",
    "team:${var.nc_conf.team}",
    "env:${var.nc_conf.env}"
  ]
}

# Monitor jobs sent to DLQ
resource "datadog_monitor" "dlq" {
  for_each = toset(var.dead_letter_queue_names)

  name    = "${local.dd_service_name} dead letter queue ${each.value} has messages in ${var.nc_conf.env} env"
  type    = "metric alert"
  message = <<EOT
{{#is_alert}}

The ${local.dd_service_name} was unable to process one or more SQS queue messages. ${local.dlq_runbook_link}

Please investigate.  [Traces and related logs](${local.dd_trace_url}).

{{/is_alert}}

{{#is_alert_recovery}}

No new 'DLQ' messages; monitor has recovered.

{{/is_alert_recovery}}  @slack-app-${var.monitor_conf.mattermost_alert_channel}
EOT

  escalation_message = <<EOT
{{#is_alert}}
This is a re-alert
{{/is_alert}}
EOT
  
  query = "max(last_15m):max:aws.sqs.approximate_number_of_messages_visible{queuename:${each.value},env:${var.nc_conf.env}} >= 1"

  monitor_thresholds {
    critical          = 1
    critical_recovery = 0.999
  }

  notify_no_data    = false
  renotify_interval = local.notification_interval_mins_until_resolved

  notify_audit        = false
  timeout_h           = 24
  include_tags        = true
  validate            = true
  require_full_window = false

  tags = local.nc_standard_tags
}

# Job queue approximate_age_of_oldest_message > SLO
resource "datadog_monitor" "queue_age" {
  for_each = toset(var.queue_names)

  name    = "${local.dd_service_name} queue ${each.value} processing time exceeded in ${var.nc_conf.env} env"
  type    = "metric alert"
  message = <<EOT
{{#is_alert}}

The ${local.dd_service_name} was unable to process one or more SQS queue messages within the SLA of ${local.queue_age_slo_mins} minutes. ${local.queue_age_runbook_link}

Please investigate.  [Traces and related logs](${local.dd_trace_url}).

{{/is_alert}}

{{#is_alert_recovery}}

Backlog of ${local.dd_service_name} messages has recovered.

{{/is_alert_recovery}}  @slack-app-${var.monitor_conf.mattermost_alert_channel}
EOT

  escalation_message = <<EOT
{{#is_alert}}
This is a re-alert
{{/is_alert}}
EOT

  query = "max(last_1h):max:aws.sqs.approximate_age_of_oldest_message{queuename:${each.value},env:${var.nc_conf.env}} > ${local.queue_age_slo_secs}"

  monitor_thresholds {
    critical = local.queue_age_slo_secs
    warning  = local.queue_age_slo_secs / 2
    # set recover for critical to 10% of slo
    critical_recovery = local.queue_age_slo_secs / 10
    # set recover for critical to 20% of slo
    warning_recovery = local.queue_age_slo_secs / 5
  }

  notify_no_data    = false
  renotify_interval = local.notification_interval_mins_until_resolved

  notify_audit        = false
  timeout_h           = 24
  include_tags        = true
  validate            = true
  require_full_window = false

  tags = local.nc_standard_tags
}
