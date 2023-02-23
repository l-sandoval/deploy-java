package reliqreports;

import java.io.*;
import java.util.HashMap;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import org.json.simple.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import reliqreports.ReportsGeneratorHandler.ReportsGeneratorHandler;

public class LambdaFunctionHandler implements RequestStreamHandler
{
	LambdaLogger logger;	
	
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		this.logger = context.getLogger();

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(inputStream);

		JSONObject responseJson = new JSONObject();

		try {
			AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger);
			InputStream jsonDataSource = s3Consumer.getInputStreamFileFromS3(ReportGeneratorConfig.getValue("s3Path.Environments.ApiEndpoints"), StringLiterals.LAMBDA_BUCKET);

			JSONObject environmentsApiEndpoints = objectMapper.readValue(jsonDataSource, JSONObject.class);

			String[] environments = objectMapper.readValue(rootNode.get("environments").toString(), String[].class);
			String[] reportsToBeGenerated = objectMapper.readValue(
					rootNode.get("reportsToBeGenerated").toString(), String[].class);
			HashMap<String, HashMap<String, String[]>> xmlFiles = objectMapper.convertValue(
					rootNode.get("xmlFiles"), new TypeReference<HashMap<String, HashMap<String, String[]>>>() {});
			String generationDate = objectMapper.readValue(rootNode.get("generationDate").toString(), String.class);
			String reportPeriodDate = objectMapper.readValue(rootNode.get("reportPeriodDate").toString(), String.class);
			String entityId = rootNode.get("entityId") != null ? objectMapper.readValue(rootNode.get("entityId").toString(), String.class) : "";
			String entityName = rootNode.get("entityName") != null ? objectMapper.readValue(rootNode.get("entityName").toString(), String.class) : "";
			String organizationId = rootNode.get("organizationId") != null ? objectMapper.readValue(rootNode.get("organizationId").toString(), String.class) : "";
            String organizationName = rootNode.get("organizationName") != null ? objectMapper.readValue(rootNode.get("organizationName").toString(), String.class) : "";
			Boolean shouldStageReport = rootNode.get("shouldStage") != null ? objectMapper.readValue(rootNode.get("shouldStage").toString(), Boolean.class) : false;

			String logoPath = reportsToBeGenerated.length > 0 && Objects.equals(reportsToBeGenerated[0], ReportsLiterals.INDIVIDUAL_PATIENT_REPORT) ? "s3path.IUGOReport-Logo-individualPatientReport" : "s3path.IUGOReport-Logo";
			s3Consumer.retrieveFileFromS3(ReportGeneratorConfig.getValue(logoPath), StringLiterals.IMAGE, StringLiterals.LAMBDA_BUCKET);

			ReportsGeneratorHandler handler = new ReportsGeneratorHandler(
					this.logger,
					reportsToBeGenerated,
					xmlFiles,
					environments,
					environmentsApiEndpoints,
					generationDate,
					reportPeriodDate,
					entityId,
					entityName,
					organizationId,
					organizationName,
					shouldStageReport);
			handler.generateReports();
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