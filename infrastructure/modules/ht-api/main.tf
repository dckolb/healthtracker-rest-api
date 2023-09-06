module "eks-pod-iam-role" {
  source             = "../eks-pod-iam-role"
  nc_conf            = var.nc_conf
  tfstate            = var.tfstate
  read_queues_secrets  = var.read_queues_secrets
  write_queues_secrets  = var.write_queues_secrets
  extra_cluster_eks_state_dirs = var.extra_cluster_eks_state_dirs
}