# Reliq.ReportGenerator

JasperReports service using AWS Lambda function.

## Publishing the lambda function in AWS
1. Create a new AWS Lambda function using the runtime `Java 11 (Corretto)`
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

## Lambda function parameters for report generation
The following parameters used in the function invocation to generate reports:

* `reportsToBeGenerated`: Array of strings with the names of the reports to generate.
* `environments`: Array of strings with the names of the environments to which the reports belong.
* `xmlFiles`: Object with the xml path to generate each report.
* `generationDate`: Datetime when report generator was called. Used in the folder structure.
* `reportPeriodDate`: Report year and month. Used for the folder structure.
* `entityId`: Report entity id to which the reports belong . Used in the staged record process (optional).
* `entityName`: Report entity name to which the reports belong. Used in the folder structure (optional).
* `organizationId`: Entity organization id. Used in the staged record process (optional).
* `organizationName`: Entity organization name. Used in the folder structure (optional). 
* `shouldStage`: Indicates if the report needs to be saved in the staging DB, default is false (optional).

Individual Patient report's json example:

```json
{
  "reportsToBeGenerated": ["IndividualPatient"], 
  "environments": ["Reliq"], 
  "xmlFiles": {
    "Reliq": {
      "IndividualPatient": ["raw-data/2023-02/2023-03-09T19:06:53.476695+00:00/Reliq/11111111-1111-1111-1111-111111111111/iUGO_Report_John_Doe_Individual-Patient-Report_2023-03-09T19:06.xml"]
    }
  }, 
  "generationDate": "2023-03-09 19:06:53", 
  "reportPeriodDate": "2023-02",
  "entityId": "11111111-AA11-AA11-1111-111111111111",
  "entityName": "John Doe",
  "organizationId": "22222222-22B2-2B22-2222-222222222222",
  "organizationName": "Organization Clinic",
  "shouldStage": true
}
```

## Lambda function parameters for report staging
The following parameters used in the function invocation to stage a report:

* `reportPath`: String with the report path in s3
* `deliveryEndpoint`: String with the url to deliver the report
* `reportToBeStaged`: String with the name of the report to stage

```json
{
  "reportPath": "output/2022-06/Monthly/2023-08-21 21:50:30/DigiiMed/Tenant_DigiiMed_MonthlyJSONReport_20230821.json",
  "deliveryEndpoint": "digiimed-endpoint.com",
  "reportToBeStaged": "MonthlyJSONReport"
}
```

## Supported reports
The following list contains the supported reports:
* ComplianceBillingReport
* CustomerBillingReport
* EmergencyRecoveryReport
* IndividualRPMReadingsReport
* IndividualPatient
* DailyCriticalReadingsReport
* InstanceMonthlyDataReport
* WeeklyAdherenceReport
* InstanceComplianceBillingReport
* InstanceRPMReadingsReport
* InstanceCustomerBillingReport