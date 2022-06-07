#!/bin/bash

# Load configuration
source lambda_config.sh

echo "Copying local resources to S3 buckets with AWS CLI"

aws s3 sync aws_IaC s3://$files_s3_bucket/IaC
aws s3 cp lambda_code/target/$lambda_packaged_file s3://$files_s3_bucket/lambda/
aws s3 sync lambda_test s3://$templates_s3_bucket

echo "Local resources copied"