data "aws_caller_identity" "current" {}

data "aws_sqs_queue" "notification_queue" {
    name = var.notification_queue_name
}

resource "aws_sqs_queue" "job_queue" {
    name =  var.job_queue_name
    fifo_queue = true
    delay_seconds = 30
    redrive_policy = jsonencode({
        deadLetterTargetArn = aws_sqs_queue.job_dead_letter_queue.arn
        maxReceiveCount     = 1
    })
    kms_master_key_id                 = "alias/aws/sqs"
}

resource "aws_sqs_queue" "job_dead_letter_queue" {
    name = var.job_dead_letter_queue_name
    fifo_queue = true

    kms_master_key_id                 = "alias/aws/sqs"
}

#
## HT Status queues
resource "aws_sqs_queue" "status_queue" {
    name =  var.status_queue_name
    redrive_policy = jsonencode({
        deadLetterTargetArn = aws_sqs_queue.status_dead_letter_queue.arn
        maxReceiveCount     = 3
    })

    kms_master_key_id                 = "alias/aws/sqs"
}

resource "aws_sqs_queue_policy" "status_queue_policy" {
    queue_url = aws_sqs_queue.status_queue.id
    policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "sqspolicy",
  "Statement": [
    {
      "Sid": "First",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "sqs:*",
      "Resource": "${aws_sqs_queue.status_queue.arn}",
      "Condition": {
        "StringLike": {
          "aws:PrincipalArn": "arn:aws:iam::${data.aws_caller_identity.current.account_id}:*"
        }
      }
    }
  ]
}
POLICY
}

resource "aws_sqs_queue" "status_dead_letter_queue" {
    name = var.status_dead_letter_queue_name

    kms_master_key_id                 = "alias/aws/sqs"
}


# HT Reminder queues
resource "aws_sqs_queue" "reminder_queue" {
    name =  var.reminder_queue_name
    redrive_policy = jsonencode({
        deadLetterTargetArn = aws_sqs_queue.reminder_dead_letter_queue.arn
        maxReceiveCount     = 3
    })

    kms_master_key_id                 = "alias/aws/sqs"
}

resource "aws_sqs_queue" "reminder_dead_letter_queue" {
    name = var.reminder_dead_letter_queue_name

    kms_master_key_id                 = "alias/aws/sqs"
}

#
resource "aws_sqs_queue_policy" "reminder_queue_policy" {
    queue_url = aws_sqs_queue.reminder_queue.id
    policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "sqspolicy",
  "Statement": [
    {
      "Sid": "First",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "sqs:*",
      "Resource": "${aws_sqs_queue.reminder_queue.arn}",
      "Condition": {
        "StringLike": {
          "aws:PrincipalArn": "arn:aws:iam::${data.aws_caller_identity.current.account_id}:*"
        }
      }
    }
  ]
}
POLICY
}