package potaymaster.aws.lambda.jasperreports;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import software.amazon.awssdk.utils.BinaryUtils;

public class ReportGenerator {	
	private LambdaLogger logger;
	private JRDataSource mainDataSource;

	public ReportGenerator(LambdaLogger logger) {
		this.logger = logger;
		this.mainDataSource = new JREmptyDataSource();
	}

	public String generateBase64EncodedReport() throws JRException, IOException {
		try {			
			File file = new File(StringLiterals.outFile);
			OutputStream outputSteam = new FileOutputStream(file);						
			generateReport(outputSteam);			
			byte[] encoded = BinaryUtils.toBase64Bytes(FileUtils.readFileToByteArray(file));
			return new String(encoded, StandardCharsets.US_ASCII);
		} catch (FileNotFoundException e) {
			logger.log("It was not possible to access the output file: " + e.getMessage());
			throw e;
		} catch (IOException e) {
			logger.log("It was not possible to read and encode the report: " + e.getMessage());
			throw e;
		}
	}

	public void generateReport(OutputStream outputSteam) throws JRException {
		ComplianceBillingReportGenerator reportGenerator = new ComplianceBillingReportGenerator();
		SourceFiles[] csv_list = reportGenerator.retrieveSourceFilesToGenerateReport();
		AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger);
		JasperPrint[] jasperPrints = new JasperPrint[csv_list.length];
		for (int i = 0; i < csv_list.length; i++) {
			try {
				s3Consumer.retrieveFileFromS3(csv_list[i].template, StringLiterals.TEMPLATE);
				s3Consumer.retrieveFileFromS3(csv_list[i].csv, StringLiterals.CSV);	
			} catch (IOException e) {
				logger.log(e.getMessage());
			}

			JasperReport jasperDesign = JasperCompileManager.compileReport(StringLiterals.tmpTemplate);
			try {
				Map<String, Object> parameters = new HashMap<String, Object>();		
				parameters.put("iUGOLogo", StringLiterals.tmpImage);

				this.mainDataSource = getDataSource();

				jasperPrints[i] = JasperFillManager.fillReport(jasperDesign, parameters, this.mainDataSource);	
			} catch (JRException e) {
				logger.log("There was an error while generating the report: " + e.getMessage());
				throw e;
			}
		}
		
		for (int i = 0; i < jasperPrints.length; i++) {
			if (i != 0) {
				List<JRPrintPage> pages = jasperPrints[i].getPages();
				for (int j = 0; j < pages.size(); j++) {
					JRPrintPage object = (JRPrintPage)pages.get(j);
					jasperPrints[0].addPage(object);
				}
			}			
		}

		JasperExportManager.exportReportToPdfStream(jasperPrints[0], outputSteam);
	}	
	
	private JRCsvDataSource getDataSource() throws JRException
	{
		JRCsvDataSource ds = new JRCsvDataSource(JRLoader.getLocationInputStream(StringLiterals.tmpCSV));
		ds.setRecordDelimiter("\r\n");
		ds.setUseFirstRowAsHeader(true); 
		return ds;
	}
}
