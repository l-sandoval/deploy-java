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
	
	static final String IUGOLOGOPATH = "images/iUGO Care W@3x.png"; //TODO: Move to config file IPM-7990		

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		this.logger = context.getLogger();
		JSONObject responseJson = new JSONObject();
		try {
			AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger);				
			s3Consumer.retrieveFileFromS3(IUGOLOGOPATH, StringLiterals.IMAGE);	
			
			s3Consumer.retrieveFileFromS3("compliance-billing/csv/IUGOReport_2022-01-31_12345678_monthly_compliance_billing.xml", StringLiterals.XML);
			ReportGenerator reportGenerator = new ReportGenerator(this.logger);
			byte[] report = reportGenerator.generateReport("xls",
            		"ComplianceBillingReport",
            		StringLiterals.TMP_XML,
                    "compliance-billing/excel-templates",//type == IUGOReportsApp.TYPE_PDF ? JASPER_PATH_PDF : JASPER_PATH_XLS,
                    "compliance-billing/csv",
                    "compliance-billing/output");
			
			s3Consumer.uploadFileToS3("compliance-billing/output/outputt.xls", report);
			
			
			
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