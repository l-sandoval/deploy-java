package reliqreports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.query.JRXPathQueryExecuterFactory;
import net.sf.jasperreports.engine.util.JRXmlUtils;
import net.sf.jasperreports.export.*;
import reliqreports.common.EReportCategory;
import reliqreports.common.SubReportDto;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRDataSource;

public class ReportGenerator {
    private LambdaLogger logger;
    private AmazonS3Consumer s3Consumer;

    public ReportGenerator(LambdaLogger logger) {
        this.logger = logger;
    }

    private long startTime = System.currentTimeMillis();    
    private Map<String, Object> parameters = new LinkedHashMap<String, Object>();    

    /**
     * Generate a report according to the type
     * @param type      : "PDF", "XLS" or "XLSX"  (one or more are allowed), if null, the type(s) are read from the XML file
     * @param reportName: name of the report e.g. "ComplianceBillingReport"
     * @param xmlFile   : XML file with the report parameters
     * @param jasperPath: path for the templates
     * @param buildPath : destination path for the output reports
     * @param apiEndpoint  : current environment api endpoint
     */
    public void generateReport(
            String type,
            String reportName,
            String xmlFile,
            String jasperPath,
            String buildPath,
            String entityId,
            String entityName,
            String organizationId,
            String organizationName,
            String generationDate,
            String apiEndpoint,
            Boolean shouldStageReport
            ) throws JRException {


        boolean shouldStage = HelperFunctions.shouldStageReport(reportName, shouldStageReport);

        String jasperSource = jasperPath + StringLiterals.FILE_SEPARATOR_FOR_S3_QUERIES + reportName + ".jrxml";

        logger.log("GenerateReport: " + reportName + "t=" + (System.currentTimeMillis() - startTime));

        parameters = new LinkedHashMap<String, Object>();
        List<String> fileNameList = new ArrayList<String>();
        
        InputStream dataSource = getInputStreamFileFromS3(xmlFile, StringLiterals.FILES_BUCKET);

        if (dataSource != null) {
            logger.log("Report... : Fill from : " + xmlFile);
            Document document = JRXmlUtils.parse(dataSource);

            parameters.put(JRXPathQueryExecuterFactory.PARAMETER_XML_DATA_DOCUMENT, document);
            parameters.put(JRXPathQueryExecuterFactory.XML_DATE_PATTERN, "yyyy-MM-dd");
            parameters.put(JRXPathQueryExecuterFactory.XML_NUMBER_PATTERN, "#,##0.##");
            parameters.put(JRXPathQueryExecuterFactory.XML_LOCALE, Locale.ENGLISH);
            parameters.put(JRParameter.REPORT_LOCALE, Locale.US);

            Node topNode = document.getChildNodes().item(0);
            if (HelperFunctions.extractXml(topNode, null, parameters)) {
                logger.log("Report... : Map size=" + parameters.size());
            }

            // get the sheet names & source files from the parameters
            Set<String> keys = parameters.keySet();
            Map<String, Object> otherParams = new LinkedHashMap<String, Object>();
            List<SubReportDto> subReportList = new ArrayList<SubReportDto>();
            
            for (String key : keys) {
                if (key.toLowerCase().contains(StringLiterals.SUBREPORT)) {
                    logger.log("Report... : key=" + key);
                    // get the key and filename
                    if (key.toLowerCase().contains(StringLiterals.SHEET_NAME)) {
                        String tab = key.split(StringLiterals.FILENAME_FIELD_SEPARATOR)[2];
                        String subFileNameKey = keys.stream().filter(s -> s.contains(tab) &&  s.contains(StringLiterals.PATH)).findFirst().orElse("");
                        logger.log("Report... : key=" + tab);                

                        String subFileName = parameters.get(subFileNameKey).toString();
                        fileNameList.add(subFileName);
                        SubReportDto dto = new SubReportDto(tab, parameters.get(key).toString(), parameters.get(subFileNameKey).toString());
                        subReportList.add(dto);
                    }
                }
                
                if (key.contains(StringLiterals.PARAMETER_FILES)) {
                    logger.log("Report... : key=" + key);
                    String parameterFileName = parameters.get(key).toString();
                    InputStream parametersSource = getInputStreamFileFromS3(parameterFileName, StringLiterals.FILES_BUCKET);
                    Map<String, String> p = getParametersFromCsvFile(parametersSource);
                    otherParams.putAll(p);
                }
            }
            
            logger.log("Other Params: " + otherParams);
            parameters.putAll(otherParams);            
            parameters.put(StringLiterals.IUGOLOGO, StringLiterals.TMP_IMAGE);
            parameters.put(StringLiterals.PAGE_COUNT, Integer.toString(fileNameList.size()));        

            InputStream templateStream = getInputStreamFileFromS3(jasperSource, StringLiterals.LAMBDA_BUCKET);

            JasperReport jasperDesign = JasperCompileManager.compileReport(templateStream);
            JasperPrint jpMaster = JasperFillManager.fillReport(jasperDesign, parameters, (JRDataSource) null);
            logger.log("Report... : Fill from : " + jasperSource);
            logger.log("Filling time : " + (System.currentTimeMillis() - startTime));

            // convert sheetNames list to array, add the home item
            List<String> sheetNames = new ArrayList<String>();
            sheetNames.add(StringLiterals.HOME_NAME);

            sheetNames.addAll(createSheetsFromCSVData(subReportList, jasperPath, jpMaster));

            // find the file type required
            if (type == null && parameters.containsKey(StringLiterals.FILE_TYPE)) {
                type = parameters.get(StringLiterals.FILE_TYPE).toString().toLowerCase();   
            }

            byte[] fileByteArray = generateReportFile(type, jpMaster, sheetNames.toArray(new String[0]));
            EReportCategory reportCategory = HelperFunctions.getReportCategory(reportName);
            String reportFileName = xmlFile.substring(xmlFile.lastIndexOf("/") + 1, xmlFile.lastIndexOf(".")) + "." + type; 
            String folderPath = getReportFolderPath(reportCategory, buildPath, entityName, organizationName);
            String fileName = folderPath  + StringLiterals.FILE_SEPARATOR_FOR_S3_QUERIES + reportFileName;

            uploadFileToS3(fileName, fileByteArray);

            logger.log("Export " + type + " :" + buildPath + ", creation time : " + (System.currentTimeMillis() - startTime));

            if(shouldStage){
                stageRecord(fileName, apiEndpoint, reportCategory, entityId);

                if(HelperFunctions.shouldSaveZipRecord(reportName) && !StringUtils.isNullOrEmpty(organizationId)) {
                    stageRecord(folderPath, apiEndpoint, EReportCategory.ORGANIZTION_ZIP, organizationId);
                }
            }
        }
    }
    
    private String getReportFolderPath(EReportCategory reportCategory, String buildPath, String entity, String organization) {        
        switch (reportCategory) {
            case PATIENT:
                return (buildPath + (!StringUtils.isNullOrEmpty(organization) ? StringLiterals.FILE_SEPARATOR_FOR_S3_QUERIES + organization : ""));    
            default:
                return (buildPath + (!StringUtils.isNullOrEmpty(entity) ? StringLiterals.FILE_SEPARATOR_FOR_S3_QUERIES + entity : ""));
        }
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
            configuration.setCollapseRowSpan(true);
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
            configuration.setCollapseRowSpan(true);
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

        return fileByteArray;
    }    

    private InputStream getInputStreamFileFromS3(String key_name, String bucketType) {
        InputStream s3File = null;
        try {
            s3File = getS3Consumer().getInputStreamFileFromS3(key_name, bucketType);
        } catch (IOException e) {
            logger.log(e.getMessage());
        }
        return s3File;
    }
    
    /**
     * Upload file to S3 bucket
     */
    private synchronized void uploadFileToS3 (String key_name, byte[] bytes){
        AmazonS3Consumer s3Consumer = new AmazonS3Consumer(this.logger);
        try {
            s3Consumer.uploadFileToS3(key_name, bytes);
        } catch (IOException e) {
            logger.log(e.getMessage());
        }
    }

    public void stageRecord(String filePath, String apiEndpoint, EReportCategory reportCategory, String entityId) {
        AmazonDynamoDBConsumer dynamoDBConsumer = new AmazonDynamoDBConsumer(this.logger);
        try {
            dynamoDBConsumer.stageRecord(filePath, apiEndpoint, reportCategory, entityId);
        } catch (IOException e) {
            logger.log(e.getMessage());
        }
    }

    /**
     * Create each sheet from the associated CSV data file
     * @throws JRException 
     */
    private List<String> createSheetsFromCSVData (List<SubReportDto> subReportList, String jasperPath, JasperPrint jpMaster) throws JRException{
        List<String> sheetNames = new ArrayList<String>();
        
        for(int i = 0; i < subReportList.size(); i++) {
            SubReportDto sr = subReportList.get(i);

            // copy to the array
            sheetNames.add(sr.getSubReportSheetName());

            // set the page number in the report
            parameters.put(StringLiterals.PAGE_NUMBER, Integer.toString(i + 1));            

            InputStream sourceStream = getInputStreamFileFromS3(
                    jasperPath + StringLiterals.FILE_SEPARATOR_FOR_S3_QUERIES + sr.getSubReportName() + ".jrxml",
                    StringLiterals.LAMBDA_BUCKET);

            InputStream dataStream = getInputStreamFileFromS3(
                    sr.getSubReportFilePath(),
                    StringLiterals.FILES_BUCKET);

            logger.log("Retrieve from S3 template= " + sr.getSubReportName() + ".jrxml" + " csv= " + sr.getSubReportFilePath() + "\r\n");

            if (sourceStream != null && dataStream != null ) {
                logger.log("Fill...t=" + (System.currentTimeMillis() - startTime));
                JRCsvDataSource source = new JRCsvDataSource(dataStream);
                source.setRecordDelimiter("\r\n");
                source.setUseFirstRowAsHeader(true);
                logger.log("Datasource loaded...");

                JasperReport jasperDesign = JasperCompileManager.compileReport(sourceStream);
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperDesign, parameters, source);

                // add all the pages into the master doc
                List<JRPrintPage> pp = jasperPrint.getPages();
                jpMaster.setName(sr.getSubReportSheetName());
                for (int j = 0; j < pp.size(); j++) {
                    logger.log("fill... : Add page : " + sr.getSubReportName() + "|" + j + 1);
                    jpMaster.addPage(pp.get(j));
                }
            } else {
                logger.log("Fill...Error - cannot load files\r\n");
            }
        }
        return sheetNames;
    }
    
    private Map<String, String> getParametersFromCsvFile(InputStream parametersSource) throws JRException {
        Map<String, String> csvParameters = new LinkedHashMap<String, String>();
        BufferedReader reader = null;
        logger.log("Extract csv parameters");
        
        if (parametersSource != null) {

            try {
                reader = new BufferedReader(new InputStreamReader(parametersSource, "UTF-8"));
                //Read header line
                reader.readLine();
                String line = null;
                
                // Read parameters from csv files
                while ((line = reader.readLine()) != null) {                    
                    if (!line.isEmpty()) {
                        String[] fields = line.split(StringLiterals.CSV_FIELD_SEPARATOR);

                        if(fields.length > 0) {
                            String key = fields[0].trim();
                            String value = fields.length > 1 ? fields[1].trim() : "";
                            csvParameters.put(key, value);
                        }
                    }
                }
            } catch (Exception ex) {
                logger.log(ex.getMessage());
            } finally {
                if (reader != null)
                    try {
                        reader.close();
                    } catch (IOException e) {
                        //Do nothing
                    }
            }
        }
        logger.log("CSV PARAMETERS: " + csvParameters);
        return csvParameters;
    }

    public AmazonS3Consumer getS3Consumer() {
        if(s3Consumer == null) 
            s3Consumer = new AmazonS3Consumer(this.logger);
        return s3Consumer;
    }

    public void setS3Consumer(AmazonS3Consumer s3Consumer) {
        this.s3Consumer = s3Consumer;
    }
}