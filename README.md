# Reliq.ReportGenerator
Generic [JasperReports](https://community.jaspersoft.com/project/jasperreports-library) Service using [AWS Lambda](https://aws.amazon.com/es/lambda/)

This repo contains code for a Java JasperReport Lambda function.
It is based on AWS sample code: [Jasper Reports with Lambda, RDS and API Gateway](https://github.com/aws-samples/jasper-reports-with-lambda-rds)

In this case we do not use an RDS connection. The parameters and data sources to include in the report are passed in the body of POST call.

The use of [SDK1](https://github.com/aws/aws-sdk-java) has also been changed to [SDK2](https://github.com/aws/aws-sdk-java-v2) to increase speed and improve performance.

## Content summary:

* Java Maven code
* AWS IaC Files: CloudFormation stack & API swagger
* Test files: JasperReports test template & Postman collection
* Basic scripts to automate process: create AWS buckets, syncronize resources & launch CloudFormation stack

## Requirements
* AWS-CLI installed.
* Build `lambda_code` using *Maven* throught *Eclipse*.

## First time publishing the lambda function in AWS

1. Get AWS credentials (https://docs.aws.amazon.com/en_en/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys).
2. Configure AWS credentials for AWS-Cli (https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html).
3. Configure region for AWS-Cli ($aws configure set region <SELECTED_REGION>).
4. Review *lambda_config.sh* file to ensure correct values for all properties, specially *region*, *files_s3_bucket* and *templates_s3_bucket*. The two last properties must be unique in AWS. Otherwise these cannot be created. See [Amazon S3 Bucket Naming Requirements](https://docs.aws.amazon.com/awscloudtrail/latest/userguide/cloudtrail-s3-bucket-naming-requirements.html).
5. Also, review the `api_swagger.yml` and `jasperreports_stack.yml` located in the `aws_laC` folder to validate that the *region*, *files_s3_bucket* and *templates_s3_bucket* parameters are correct. 
5. Set execute permissions to all *.sh* files.
6. Execute *00.launch_all_steps.sh*.
7. When all scripts ends, go to AWS Web Console to check CloudFormation creation stack status.
8. Wait until stack creation is complete and go to API Gateway to copy URL.
9. Paste API URL into Postman call & launch test example.


## Update lambda publishing

1. Rebuild `lambda_code` using *Maven* throught *Eclipse* to generate de new .jar file.
2. Execute *04.aws_update_template_and_function.sh*
## Lambda function parameters expectations
The following parameters are expected in the function invocation:

```json
{
  "reportsToBeGenerated": ["report1", "report2"],
  "environments": ["ReliqUsDev1", "Training"],
  "xmlFiles": [
    {
    "ReliqUsDev1": {
        "report1": ["pathToReport1File1.xml", "pathToReport1File2.xml"]
      },
    "Training": {
        "report1": ["pathToReport2File1.xml", "pathToReport2File2.xml"]
      }
    }
  ]
}
```

## Local Execution

To execute the report generator function locally:

1. Modify the json file `lambda_code/src/resources/local_execution_parameters.json` to include the parameters you want to use.
2. Add the environment variables, to do so select the `Run` tab in your IDE and then `edit configuration` for the Local.java file, this might vary depending on the IDE you are using. The following is and example of the environment variables needed:

```
```dotenv
DYNAMODB_TABLE=table_name
FILES_BUCKET=file_bucket_name
LAMBDA_BUCKET=lambda_bucket_name
REPORTS_BUCKET=reports_bucket_name
```
