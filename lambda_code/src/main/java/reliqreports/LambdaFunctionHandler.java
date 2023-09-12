package reliqreports;

import java.io.*;

import org.json.simple.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import reliqreports.ReportsGeneratorHandler.ReportsGeneratorHandler;
import reliqreports.common.enums.EReportCategory;
import reliqreports.common.dto.ReportGeneratorDto;

public class LambdaFunctionHandler implements RequestStreamHandler
{
	LambdaLogger logger;	
	
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		this.logger = context.getLogger();
		JSONObject responseJson = new JSONObject();

		try {			
			AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger);
			ReportGeneratorDto payload = new ReportGeneratorDto(inputStream);
			ObjectMapper objectMapper = new ObjectMapper();

			this.logger.log("Generating reports with payload: " + objectMapper.writeValueAsString(payload));
			
			if (payload.reportToBeStaged != null) {
				AmazonDynamoDBConsumer dynamoConsumer = new AmazonDynamoDBConsumer(this.logger);
				dynamoConsumer.stageRecord(payload.reportPath, payload.deliveryEndpoint, EReportCategory.API_DELIVERY, null);
			} else {
				InputStream jsonDataSource = s3Consumer.getInputStreamFileFromS3(ReportGeneratorConfig.getValue("s3Path.Environments.ApiEndpoints"), StringLiterals.LAMBDA_BUCKET);			
				payload.environmentsApiEndpoints = objectMapper.readValue(jsonDataSource, JSONObject.class);
				s3Consumer.retrieveFileFromS3(ReportGeneratorConfig.getValue(payload.logoPath), StringLiterals.IMAGE, StringLiterals.LAMBDA_BUCKET);
	
				ReportsGeneratorHandler handler = new ReportsGeneratorHandler(
					this.logger,
					payload
				);
				handler.generateReports();
			}

		}
		catch (Exception e) {
			this.buildErrorResponse(e.getMessage(), 500, responseJson);
		}
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
		writer.write(responseJson.toString());
		writer.close();
	}

	@SuppressWarnings("unchecked")
	public void buildErrorResponse(String body, int statusCode, JSONObject responseJson) {
		responseJson.put("body", body);
		responseJson.put("statusCode", statusCode);
	}
}