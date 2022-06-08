#!/bin/bash

# Load configuration
source lambda_config.sh

echo "Create CloudFormation stack with AWS CLI"

aws cloudformation create-stack --stack-name jasperreports --template-url https://$files_s3_bucket.s3.$region.amazonaws.com/IaC/jasperreports_stack.yml --parameters ParameterKey=FilesS3Location,ParameterValue=$files_s3_bucket ParameterKey=TemplatesS3Location,ParameterValue=$templates_s3_bucket ParameterKey=LambdaPackagedFile,ParameterValue=$lambda_packaged_file --capabilities CAPABILITY_AUTO_EXPAND CAPABILITY_NAMED_IAM

echo "CloudFormation stack launch - See status into AWS Web Console"