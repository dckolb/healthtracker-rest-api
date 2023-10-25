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

variable "job_queue_arn" {
  description = "ARN for the job queue"
  type = string
}

variable "status_queue_arn" {
  description = "ARN for status queue"
  type = string
}

variable "reminder_queue_arn" {
  description = "ARN for reminder queue"
  type = string
}

variable "notification_queue_arn" {
  description = "ARN for notification queue"
  type = string
}

