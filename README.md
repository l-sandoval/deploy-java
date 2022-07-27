# Reliq.ReportGenerator

JasperReports service using AWS Lambda function.

## Publishing the lambda function in AWS
1. Create a new AWS Lambda function using the runtime `Java 8 on Amazon Linux 1`
2. Update the lambda function handler to `reliqreports.LambdaFunctionHandler::handleRequest`
3. Update the lambda function timeout to the maximum of minutes
4. Update the lambda function environment variables to the following example
```dotenv
DYNAMODB_TABLE=table_name
FILES_BUCKET=file_bucket_name
LAMBDA_BUCKET=lambda_bucket_name
REPORTS_BUCKET=reports_bucket_name
```
5. Create s3 buckets mentioned above, the dynamodb table will be created automatically by the report generator
6. The GitHub workflow will compile the code and upload the .jar file, the templates, assets and environment endpoints files to the `LAMBDA_BUCKET` and update the
   lambda function code with the .jar file.

## Local Execution

### Requirements
* [AWS-CLI installed](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html).
* [Get AWS credentials](https://docs.aws.amazon.com/en_en/general/latest/gr/aws-sec-cred-types.html#access-keys-and-secret-access-keys).
* [Configure AWS credentials for AWS-Cli](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html).
* Configure region for AWS-Cli ($aws configure set region <SELECTED_REGION>).

To execute the report generator function locally:

1. Modify the json file `lambda_code/src/resources/local_execution_parameters.json` to include the parameters you want to use.
2. Add the environment variables, to do so select the `Run` tab in your IDE and then `edit configuration`
for the Local.java file, this might vary depending on the IDE you are using.

## Lambda function parameters
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
        "report2": ["pathToReport2File1.xml", "pathToReport2File2.xml"]
      }
    }
  ]
}
```
