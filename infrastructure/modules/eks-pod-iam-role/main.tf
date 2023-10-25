data "aws_iam_policy_document" "eks_assume_role_policy" {
  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]
    effect  = "Allow"

    principals {
      identifiers = [data.terraform_remote_state.eks.outputs.eks_iam_openid_connect_provider_arn]
      type        = "Federated"
    }

    condition {
      test = "StringEquals"
      variable = "${replace(data.terraform_remote_state.eks.outputs.eks_iam_openid_connect_provider_url, "https://", "")}:sub"
      values = ["system:serviceaccount:${var.namespace}:${var.nc_conf.service}"]
    }
  }

  # support for additional eks clusters
  dynamic "statement" {
    for_each = var.extra_cluster_eks_state_dirs

    content {
      actions = ["sts:AssumeRoleWithWebIdentity"]
      effect  = "Allow"

      principals {
        identifiers = [data.terraform_remote_state.extra_eks_clusters[statement.key].outputs.eks_iam_openid_connect_provider_arn]
        type        = "Federated"
      }

      condition {
        test = "StringEquals"
        variable = "${replace(data.terraform_remote_state.extra_eks_clusters[statement.key].outputs.eks_iam_openid_connect_provider_url, "https://", "")}:sub"
        values = ["system:serviceaccount:${var.namespace}:${var.nc_conf.service}"]
      }
    }
  }
}

resource "aws_iam_role" "health_tracker_rest_api" {
  name               = "${var.nc_conf.env}-${var.nc_conf.service}-role"
  assume_role_policy = data.aws_iam_policy_document.eks_assume_role_policy.json
}

resource "aws_iam_role_policy" "health_tracker_rest_api" {
  name   = "${var.nc_conf.env}-${var.nc_conf.service}-inline"
  policy = data.aws_iam_policy_document.health_tracker_rest_api.json
  role   = aws_iam_role.health_tracker_rest_api.id
}

data "aws_iam_policy_document" "health_tracker_rest_api" {
  // Secret access
  // See https://github.com/NavigatingCancer/java-nc-commons/blob/94e92032c31e30932fb64000dc017c9f51bdbd3e/src/main/java/com/navigatingcancer/ssm/AwsSecretsManagerEnvironmentPostProcessor.java#L38
  // See https://github.com/NavigatingCancer/devops-tooling/blob/master/terraform/modules/eks/nc-deployments/eks-worker-iam-policies/main.tf
  statement {
    sid = "SecretAccess"
    
    actions = [
      "secretsmanager:GetResourcePolicy",
      "secretsmanager:GetSecretValue",
      "secretsmanager:DescribeSecret",
      "secretsmanager:ListSecretVersionIds"
    ]
    
    // Replace the 6 char suffix at the end of the ARNs with a wildcard, so the role 
    // doesn't lose permission if the secret is updated
    resources = [  
      "${regex("(.*-).*$", data.aws_secretsmanager_secret.service_secret.arn)[0]}??????",
      "${regex("(.*-).*$", data.aws_secretsmanager_secret.shared_secret.arn)[0]}??????"
    ]
  }

  statement {
    sid = "ReadFromSqsQueuePermissions"
    actions = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes"
    ]

    resources = [
      var.reminder_queue_arn,
      var.status_queue_arn,
      var.job_queue_arn
    ]
  }

  statement {
    sid = "WriteToSqsQueuePermissions"
    actions = [
      "sqs:SendMessage",
      "sqs:GetQueueUrl",
      "sqs:GetQueueAttributes"
    ]

    resources = [
      var.status_queue_arn,
      var.job_queue_arn,
      var.notification_queue_arn
    ]
  }

}

// Look up the secret and queues so we don't accidentally
// create a policy for an invalid ARN
data "aws_secretsmanager_secret" "service_secret" {
  name = "${var.nc_conf.env}/ht-api"
}

data "aws_secretsmanager_secret" "shared_secret" {
  name = "${var.nc_conf.env}/shared"
}

data "aws_secretsmanager_secret_version" "shared_secret" {
  secret_id = data.aws_secretsmanager_secret.shared_secret.id
}

data "terraform_remote_state" "eks" {
  backend = "s3"
  config = {
    profile = var.tfstate.aws_profile
    region  = var.tfstate.aws_region
    bucket  = var.tfstate.s3_bucket
    encrypt = true
    key     = "${var.tfstate.aws_profile}/${var.nc_conf.env}/${var.tfstate.eks_state_dir}/eks-cluster/terraform.tfstate"
  }
}

data "terraform_remote_state" "extra_eks_clusters" {
  count = length(var.extra_cluster_eks_state_dirs)

  backend = "s3"
  config = {
    profile = var.tfstate.aws_profile
    region  = var.tfstate.aws_region
    bucket  = var.tfstate.s3_bucket
    encrypt = true
    key     = "${var.tfstate.aws_profile}/${var.nc_conf.env}/${var.extra_cluster_eks_state_dirs[count.index]}/eks-cluster/terraform.tfstate"
  }
}