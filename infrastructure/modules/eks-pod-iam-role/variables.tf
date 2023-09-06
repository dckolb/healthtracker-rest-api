variable "read_queues_secrets" {
  description = "Secret names for SQS queues from which REST API needs permission to read messages"
  type = list(string)
}

variable "write_queues_secrets" {
  description = "Secret names for SQS queues from which REST API needs permission to write messages"
  type = list(string)
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