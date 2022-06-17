package potaymaster.aws.lambda.jasperreports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.query.JRXPathQueryExecuterFactory;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRXmlUtils;
import net.sf.jasperreports.export.*;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.io.File;
import java.io.IOException;
import java.util.*;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRDataSource;

public class ReportGenerator {
	private LambdaLogger logger;
	private ReportGeneratorConfig config;

	public ReportGenerator(LambdaLogger logger, ReportGeneratorConfig reportGeneratorConfig) {
		this.logger = logger;
		this.config = reportGeneratorConfig;
	}

	private long startTime = System.currentTimeMillis();    
	private Map<String, Object> parameters = new HashMap<String, Object>();    

	/**
	 * Generate a report according to the type
	 * @param type 		: "PDF", "XLS" or "XLSX"  (one or more are allowed), if null, the type(s) are read from the XML file
	 * @param reportName: name of the report e.g. "ComplianceBillingReport"
	 * @param xmlFile 	: report XML file
	 * @param jasperPath: location for the templates
	 * @param dataPath	: data sources (CSV, XML)
	 * @param buildPath : destination for the output reports
	 */
	public byte[] generateReport(
			String type,
			String reportName,
			String xmlFile,
			String jasperPath,
			String dataPath,
			String buildPath
			) throws JRException {

		HelperFunctions helper = new HelperFunctions(this.logger);			

		String jasperSource = jasperPath + File.separator + reportName + ".jrxml";

		logger.log("GenerateReport: " + reportName + "t=" + (System.currentTimeMillis() - startTime));

		parameters = new HashMap<String, Object>();
		ArrayList<String> sheetNameList = new ArrayList<String>();
		ArrayList<String> fileNameList = new ArrayList<String>();

		File dataSource = new File(xmlFile);
		if (dataSource.canRead()) {
			logger.log("Report... : Fill from : " + xmlFile);
			Document document = JRXmlUtils.parse(JRLoader.getLocationInputStream(dataSource.getPath()));

			parameters.put(StringLiterals.IUGOLOGO, StringLiterals.TMP_IMAGE);
			parameters.put(JRXPathQueryExecuterFactory.PARAMETER_XML_DATA_DOCUMENT, document);
			parameters.put(JRXPathQueryExecuterFactory.XML_DATE_PATTERN, "yyyy-MM-dd");
			parameters.put(JRXPathQueryExecuterFactory.XML_NUMBER_PATTERN, "#,##0.##");
			parameters.put(JRXPathQueryExecuterFactory.XML_LOCALE, Locale.ENGLISH);
			parameters.put(JRParameter.REPORT_LOCALE, Locale.US);

			Node topNode = document.getChildNodes().item(0);
			if (helper.extractXml(topNode, null, parameters)) {
				logger.log("Report... : Map size=" + parameters.size());
			}

			// get the sheet names & source files from the parameters
			Set<String> keys = parameters.keySet();
			for (String key : keys) {
				if (key.toLowerCase().contains(StringLiterals.SUBREPORT)) {
					// get the key and filename
					String tab = key.split(StringLiterals.FILENAME_FIELD_SEPARATOR)[2];
					logger.log("Report... : key=" + tab);
					sheetNameList.add(tab);
					String subFileName = parameters.get(key).toString();
					fileNameList.add(subFileName);
				}
			}
			parameters.put(StringLiterals.PAGE_COUNT, Integer.toString(fileNameList.size()));        

			retrieveFileFromS3(jasperSource, StringLiterals.TEMPLATE);

			JasperReport jasperDesign = JasperCompileManager.compileReport(StringLiterals.TMP_TEMPLATE);
			JasperPrint jpMaster = JasperFillManager.fillReport(jasperDesign, parameters, (JRDataSource) null);
			logger.log("Report... : Fill from : " + jasperSource);
			logger.log("Filling time : " + (System.currentTimeMillis() - startTime));

			// convert sheetNames list to array, add the home item
			String[] sheetNames = new String[sheetNameList.size() + 1];
			sheetNames[0] = StringLiterals.HOME_NAME;

			createSheetsFromCSVData(sheetNameList, sheetNames, jasperPath, dataPath, fileNameList, jpMaster);

			// find the file type required
			if (type == null && parameters.containsKey(StringLiterals.FILE_TYPE)) {
				type = parameters.get(StringLiterals.FILE_TYPE).toString().toLowerCase();   
			}

			return generateReportFile(type, jpMaster, sheetNames);
		}
		return null;
	}

	/**
	 * Generate the report file according to the file type
	 * @throws JRException 
	 */
	private byte[] generateReportFile (String type, JasperPrint jpMaster, String[] sheetNames) throws JRException{
		File destFile = null;
		// PDF
		if (type != null && type.contains(StringLiterals.TYPE_PDF)) {
			destFile = new File(StringLiterals.TMP_OUT_FILE_PDF);
			JRPdfExporter exporter = new JRPdfExporter();

			// export the final doc
			exporter.setExporterInput(new SimpleExporterInput(jpMaster));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

			// set the configuration for PDF
			SimplePdfReportConfiguration configuration = new SimplePdfReportConfiguration();
			configuration.setSizePageToContent(false);
			configuration.setForceLineBreakPolicy(true);
			exporter.setConfiguration(configuration);

			exporter.exportReport();
		}

		// XLSX or XLS
		if (type != null && type.contains(StringLiterals.TYPE_XLSX)) {
			destFile = new File(StringLiterals.TMP_OUT_FILE_XLS);
			JRXlsxExporter exporter = new JRXlsxExporter();

			// export the final doc
			exporter.setExporterInput(new SimpleExporterInput(jpMaster));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

			// set the configuration for XLSX
			SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
			configuration.setOnePagePerSheet(true);
			configuration.setDetectCellType(true);
			configuration.setCollapseRowSpan(false);
			configuration.setSheetNames(sheetNames);
			exporter.setConfiguration(configuration);

			exporter.exportReport();	
		} // XLS
		else if (type != null && type.contains(StringLiterals.TYPE_XLS)) {
			destFile = new File(StringLiterals.TMP_OUT_FILE_XLS);
			JRXlsExporter exporter = new JRXlsExporter();

			// export the final doc
			exporter.setExporterInput(new SimpleExporterInput(jpMaster));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

			// set the configuration for XLS
			SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
			configuration.setOnePagePerSheet(true);
			configuration.setDetectCellType(true);
			configuration.setCollapseRowSpan(false);
			configuration.setSheetNames(sheetNames);
			exporter.setConfiguration(configuration);

			exporter.exportReport();	
		}		

		byte[] fileByteArray = null;
		try {
			fileByteArray = FileUtils.readFileToByteArray(destFile);
		} catch (IOException e) {
			logger.log(e.getMessage());
		}

		logger.log("Export " + type + " :" + destFile + ", creation time : " + (System.currentTimeMillis() - startTime));

		return fileByteArray;
	}

	/**
	 * Retrieve file from S3 bucket
	 */
	private void retrieveFileFromS3 (String key_name, String file_type){
		AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger, this.config);
		try {
			s3Consumer.retrieveFileFromS3(key_name, file_type);
		} catch (IOException e) {
			logger.log(e.getMessage());
		}
	}

	/**
	 * Create each sheet from the associated CSV data file
	 * @throws JRException 
	 */
	private void createSheetsFromCSVData (ArrayList<String> sheetNameList, String[] sheetNames, String jasperPath, String dataPath, ArrayList<String> fileNameList, JasperPrint jpMaster) throws JRException{
		for (int i = 0; i < sheetNameList.size(); i++) {
			String sheetName = sheetNameList.get(i);

			// copy to the array
			sheetNames[i + 1] = sheetName;

			// set the page number in the report
			parameters.put(StringLiterals.PAGE_NUMBER, Integer.toString(i + 1));
			File sourceFileBK = new File(jasperPath + File.separator + sheetNameList.get(i) + ".jrxml");
			File dataFileBK = new File(dataPath + File.separator + fileNameList.get(i));
			logger.log("Fill...sheet=" + sourceFileBK + " csv=" + dataFileBK + "\r\n");

			retrieveFileFromS3(jasperPath + File.separator + sheetNameList.get(i) + ".jrxml", StringLiterals.TEMPLATE);
			retrieveFileFromS3(dataPath + File.separator + fileNameList.get(i), StringLiterals.CSV);

			File sourceFile = new File(StringLiterals.TMP_TEMPLATE);
			File dataFile = new File(StringLiterals.TMP_CSV);               

			if (sourceFile.canRead() && dataFile.canRead() ) {
				logger.log("Fill...t=" + (System.currentTimeMillis() - startTime));
				JRCsvDataSource source = new JRCsvDataSource(JRLoader.getInputStream(dataFile));
				source.setRecordDelimiter("\r\n");
				source.setUseFirstRowAsHeader(true);
				logger.log("Datasource loaded...");

				JasperReport jasperDesign = JasperCompileManager.compileReport(sourceFile.getPath());
				JasperPrint jasperPrint = JasperFillManager.fillReport(jasperDesign, parameters, source);

				// add all the pages into the master doc
				List<JRPrintPage> pp = jasperPrint.getPages();
				jpMaster.setName(sheetName);
				for (int j = 0; j < pp.size(); j++) {
					logger.log("fill... : Add page : " + sheetName + "|" + j + 1);
					jpMaster.addPage(pp.get(j));
				}
			} else {
				logger.log("Fill...Error - cannot load files\r\n");
			}
		}
	}
}