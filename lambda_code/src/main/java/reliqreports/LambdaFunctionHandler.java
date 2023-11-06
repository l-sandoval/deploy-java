package reliqreports;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import reliqreports.ReportsGeneratorHandler.ReportsGeneratorHandler;
import reliqreports.Services.StagingService;
import reliqreports.common.dto.ReportsGeneratorHandlerDto;
import reliqreports.common.dto.StageZipRecordDto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class LambdaFunctionHandler implements RequestStreamHandler
{
	LambdaLogger logger;	
	
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		this.logger = context.getLogger();
		JSONObject responseJson = new JSONObject();

		try {
			AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger);
			ReportsGeneratorHandlerDto payload = new ReportsGeneratorHandlerDto(inputStream);
			ObjectMapper objectMapper = new ObjectMapper();

			InputStream jsonDataSource = s3Consumer.getInputStreamFileFromS3(
					ReportGeneratorConfig.getValue("s3Path.Environments.ApiEndpoints"),
					StringLiterals.LAMBDA_BUCKET
			);
			payload.environmentsApiEndpoints = objectMapper.readValue(jsonDataSource, JSONObject.class);

			this.logger.log("Generating reports with payload: " + objectMapper.writeValueAsString(payload));
			
			if (payload.reportToBeStaged != null) {
				StagingService stagingService = new StagingService(this.logger);
				StageZipRecordDto stageZipRecordInput = new StageZipRecordDto(payload);
				String[] pathArray = payload.reportPath.split("/");
				String[] pathArrayWithoutLast = Arrays.copyOf(pathArray, pathArray.length - 1);
				stageZipRecordInput.tenantFolderPath = String.join("/", pathArrayWithoutLast);
				stagingService.stageZipRecords(stageZipRecordInput);

			} else {
				s3Consumer.retrieveFileFromS3(
						ReportGeneratorConfig.getValue(payload.logoPath),
						StringLiterals.IMAGE,
						StringLiterals.LAMBDA_BUCKET
				);
	
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