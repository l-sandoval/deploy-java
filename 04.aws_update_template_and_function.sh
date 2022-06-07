#!/bin/bash

# Load configuration
source lambda_config.sh

echo "Update S3 buckets with AWS CLI"

aws s3 cp lambda_code/target/$lambda_packaged_file s3://$files_s3_bucket/lambda/
aws lambda update-function-code --function-name java_jasper --s3-bucket test-lambda-files-reliq --s3-key lambda/$lambda_packaged_file
aws s3 sync lambda_test s3://$templates_s3_bucket

echo "S3 buckets updated"