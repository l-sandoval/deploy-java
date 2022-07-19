package potaymaster.aws.lambda.jasperreports;

import java.io.*;
import java.util.HashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import org.json.simple.JSONObject;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import potaymaster.aws.lambda.jasperreports.ReportsGeneratorHandler.ReportsGeneratorHandler;

public class LambdaFunctionHandler implements RequestStreamHandler
{
	LambdaLogger logger;	

	ReportGeneratorConfig config;

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		this.logger = context.getLogger();

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(inputStream);

		JSONObject responseJson = new JSONObject();

		this.config = new ReportGeneratorConfig();
		try {
			AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger, this.config);				
			s3Consumer.retrieveFileFromS3(this.config.get("s3path.IUGOReport-Logo"), StringLiterals.IMAGE);
			s3Consumer.retrieveFileFromS3(config.get("s3Path.Environments.ApiEndpoints"), StringLiterals.JSON);
			File jsonDataSource = new File(StringLiterals.TMP_JSON);

			JSONObject environmentsApiEndpoints = objectMapper.readValue(jsonDataSource, JSONObject.class);

			String[] environments = objectMapper.readValue(rootNode.get("environments").toString(), String[].class);
			String[] reportsToBeGenerated = objectMapper.readValue(
					rootNode.get("reportsToBeGenerated").toString(), String[].class);
			HashMap<String, HashMap<String, String[]>> xmlFiles = objectMapper.convertValue(
					rootNode.get("xmlFiles"), new TypeReference<HashMap<String, HashMap<String, String[]>>>() {});
			ReportsGeneratorHandler handler = new ReportsGeneratorHandler(
					this.logger, this.config,
					reportsToBeGenerated, xmlFiles,
					environments,
					environmentsApiEndpoints);
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