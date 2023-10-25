module "eks-pod-iam-role" {
  source                       = "../eks-pod-iam-role"
  nc_conf                      = var.nc_conf
  tfstate                      = var.tfstate
  extra_cluster_eks_state_dirs = var.extra_cluster_eks_state_dirs
  job_queue_arn                = aws_sqs_queue.job_queue.arn
  reminder_queue_arn           = aws_sqs_queue.reminder_queue.arn
  status_queue_arn             = aws_sqs_queue.status_queue.arn
  notification_queue_arn       = data.aws_sqs_queue.notification_queue.arn
}

module "datadog" {
  source            = "../datadog"
  nc_conf           = var.nc_conf
  monitor_conf      = var.monitor_conf
  queue_names       = [
    var.job_queue_name,
    var.reminder_queue_name,
    var.status_queue_name
  ]
  dead_letter_queue_names = [
    var.job_dead_letter_queue_name,
    var.status_dead_letter_queue_name,
    var.reminder_dead_letter_queue_name
  ]
}
