variable "aws_provider_conf" {
  type = any
}

variable "tfstate" {
  description = "Identifying params for an env's shared remote terraform state bucket"
  type = object({
    # The name of the AWS profile used to store EKS cluster Terraform remote state
    aws_profile : string
    # The AWS region of the S3 bucket used to store EKS cluster Terraform remote state
    aws_region : string
    # The name of the S3 bucket used to store EKS cluster Terraform remote state
    s3_bucket : string
    # The name of the subdirectory used to store EKS cluster Terraform remote state
    eks_state_dir: string
  })
}

variable "nc_conf" {
  description = "The NC env/app/team"
  type = object({
    env  = string
    app  = string
    team = string
    service = string
  })
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

variable "namespace" {
  description = "Kubernetes namespace the service is deployed into."
  type        = string
  default     = "health-tracker"
}

variable "extra_cluster_eks_state_dirs" {
  description = "The state directories for any additional clusters that need to be authorized to use the pod IAM role. Same format as tfstate.eks_state_dir."
  type        = list(string)
  default     = []
}

variable "job_queue_name" {
  description = "The name of the job queue"
  type = string
}

variable "job_dead_letter_queue_name" {
  description = "The name of the job DLQ"
  type = string
}

variable "reminder_queue_name" {
  description = "The name of the reminder queue"
  type = string
}

variable "reminder_dead_letter_queue_name" {
  description = "The name of the reminder DLQ"
  type = string
}

variable "status_queue_name" {
  description = "The name of the status queue"
  type = string
}

variable "status_dead_letter_queue_name" {
  description = "The name of the status DLQ"
  type = string
}

variable "notification_queue_name" {
  description = "The name of the notification queue"
  type = string
}