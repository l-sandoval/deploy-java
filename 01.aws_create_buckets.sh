#!/bin/bash

# Load configuration
source lambda_config.sh

echo "Creating S3 buckets with AWS CLI"

aws s3 mb s3://$files_s3_bucket
aws s3 mb s3://$templates_s3_bucket

echo "S3 buckets created"