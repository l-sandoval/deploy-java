package potaymaster.aws.lambda.jasperreports;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.json.simple.JSONObject;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class LambdaFunctionHandler implements RequestStreamHandler
{
	LambdaLogger logger;
	
	static final String IUGOLOGOPATH = "images/iUGO Care W@3x.png"; //TODO: Move to config file IPM-7972		

	ReportGeneratorConfig config;

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		this.logger = context.getLogger();
		JSONObject responseJson = new JSONObject();
		this.config = new ReportGeneratorConfig();
		try {
			AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger, this.config);				
			s3Consumer.retrieveFileFromS3(IUGOLOGOPATH, StringLiterals.IMAGE);	
			
			//TODO: Call to ReportGenerator IPM-7972
			/*s3Consumer.retrieveFileFromS3("", StringLiterals.XML);
			ReportGenerator reportGenerator = new ReportGenerator(this.logger, this.config);
			byte[] report = reportGenerator.generateReport(parameters);
			
			s3Consumer.uploadFileToS3("", report);*/
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