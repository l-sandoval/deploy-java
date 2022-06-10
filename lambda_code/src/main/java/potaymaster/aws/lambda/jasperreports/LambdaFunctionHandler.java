package potaymaster.aws.lambda.jasperreports;

import java.io.File;
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
	
	static final String IUGOLOGO = "images/iUGO Care W@3x.png"; //TODO: Move to config file IPM-7990		

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		this.logger = context.getLogger();
		JSONObject responseJson = new JSONObject();
		try {
			AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger);				
			s3Consumer.retrieveFileFromS3(IUGOLOGO, StringLiterals.IMAGE);	
			
			/*ReportGenerator reportGenerator = new ReportGenerator(this.logger);
			String encodedReport = reportGenerator.generateBase64EncodedReport();*/
			
			s3Consumer.retrieveFileFromS3("compliance-billing/csv/IUGOReport_2022-01-31_12345678_monthly_compliance_billing.xml", StringLiterals.XML);
			research rs = new research();
			rs.generateReport("xls",
            		"ComplianceBillingReport",
            		StringLiterals.tmpXML,
                    "xls",//type == IUGOReportsApp.TYPE_PDF ? JASPER_PATH_PDF : JASPER_PATH_XLS,
                    "compliance-billing/csv",
                    "compliance-billing/output");
			

		     /*   File folder = new File("compliance-billing/csv/");
		        for (File file : folder.listFiles()) {
		            // find the correct xml file
		            if (file.getName().toLowerCase().contains(XML_SUFFIX)) {
		                String reportXmlFile = file.getAbsolutePath();
		                System.err.println("generateReport xml found: " + file.getName());

		                rs.generateReport("xls",
		                		"ComplianceBillingReport",
		                        reportXmlFile,
		                        "xls",//type == IUGOReportsApp.TYPE_PDF ? JASPER_PATH_PDF : JASPER_PATH_XLS,
		                        "compliance-billing/csv/",
		                        "compliance-billing/output/");
		                break;
		            }
		        }*/
		    
			
			
			
			
			
			//buildSuccessfulResponse(encodedReport, responseJson);
		}
		catch (Exception e) {
			this.buildErrorResponse(e.getMessage(), 500, responseJson);
		}
		OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
		writer.write(responseJson.toString());
		writer.close();
	}

	@SuppressWarnings("unchecked")
	public void buildSuccessfulResponse(String encodedReport, JSONObject responseJson) {
		JSONObject headerJson = new JSONObject();
		headerJson.put("Content-Type", "application/pdf");
		headerJson.put("Accept", "application/pdf");
		headerJson.put("Content-disposition", "attachment; filename=file.pdf");
		responseJson.put("body", encodedReport);
		responseJson.put("statusCode", 200);
		responseJson.put("isBase64Encoded", true);
		responseJson.put("headers", headerJson);
	}

	@SuppressWarnings("unchecked")
	public void buildErrorResponse(String body, int statusCode, JSONObject responseJson) {
		responseJson.put("body", body);
		responseJson.put("statusCode", statusCode);
	}
}