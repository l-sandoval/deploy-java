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
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.*;

/**
 * Shared class for report generation
 */
public class research {
    private final String PAGE_NUMBER = "IUGOReport-PageNumber";
    private final String PAGE_COUNT = "IUGOReport-PageCount";
    private final String FILE_TYPE = "IUGOReport-FileType";
    private final String TYPE_PDF = "pdf";
    private final String TYPE_XLSX = "xlsx";
    private final String TYPE_XLS = "xls";
    
    public research(){}

    private long startTime = System.currentTimeMillis();
    private final String HOME_NAME = "Home";
    private Map<String, Object> parameters = new HashMap<String, Object>();

    /*
     * Helper function to decode xml
     */
    private boolean extractXml(Node node, String name) {

        if (node == null) return false;

        String nodeName = name != null ? name + "-" + node.getNodeName() : node.getNodeName();

        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            switch (nodeList.item(i).getNodeType()) {
                case Node.ELEMENT_NODE:
                    if (nodeList.item(i).hasChildNodes()) {
                        extractXml(nodeList.item(i), nodeName);
                    }
                    break;
                case Node.TEXT_NODE:
                    String value = nodeList.item(i).getNodeValue().trim();
                    if (!value.isEmpty()) {
                        parameters.put(nodeName, value);
                        System.err.println("extractXml : Add to map: " + nodeName + " | " + value);
                    }
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    /**
     * generate a report according to the type
     * If type is null, the type(s) are read from the xml file
     *
     * @param type : "pdf", "xls" or "xlsx"  (one or more are allowed)
     * @param reportName : name of the report e.g. "ComplianceBillingReport"
     * @param xmlFile : report xml file
     * @param jasperPath : location for the .jasper templates
     * @param dataPath : data sources (csv, xml)
     * @param buildPath : destination for the output reports
     */
    public void generateReport(String type,
                                String reportName,
                                String xmlFile,
                                String jasperPath,
                                String dataPath,
                                String buildPath
                                ) throws JRException {

        String jasperSource = jasperPath + File.separator + reportName + ".jasper";

        // todo do we need all 3?
        final String XLS_OUTPUT_FILE = buildPath + File.separator + reportName + "." + TYPE_XLS;
        final String XLSX_OUTPUT_FILE = buildPath + File.separator + reportName + "." + TYPE_XLSX;
        final String PDF_OUTPUT_FILE = buildPath + File.separator + reportName + "." + TYPE_PDF;

        System.err.println("generateReport: " + reportName + "t=" + (System.currentTimeMillis() - startTime));

        parameters = new HashMap<String, Object>();
        ArrayList<String> sheetNameList = new ArrayList<String>();
        ArrayList<String> fileNameList = new ArrayList<String>();

        File dataSource = new File(xmlFile);
        if (dataSource.canRead()) {
            System.err.println("Report... : Fill from : " + xmlFile);
            Document document = JRXmlUtils.parse(JRLoader.getLocationInputStream(dataSource.getPath()));
            parameters.put(JRXPathQueryExecuterFactory.PARAMETER_XML_DATA_DOCUMENT, document);
            parameters.put(JRXPathQueryExecuterFactory.XML_DATE_PATTERN, "yyyy-MM-dd");
            parameters.put(JRXPathQueryExecuterFactory.XML_NUMBER_PATTERN, "#,##0.##");
            parameters.put(JRXPathQueryExecuterFactory.XML_LOCALE, Locale.ENGLISH);
            parameters.put(JRParameter.REPORT_LOCALE, Locale.US);

            Node topNode = document.getChildNodes().item(0);
            if (extractXml(topNode, null)) {
                System.err.println("Report... : Map size=" + parameters.size());
            }

            // get the sheet names & source files from the parameters
            Set<String> keys = parameters.keySet();
            for (String key : keys) {
                if (key.toLowerCase().contains("subreport")) {

                    // get the key and filename
                    String tab = key.split("-")[2];  // TODO
                    System.err.println("Report... : key=" + tab);
                    sheetNameList.add(tab);
                    String subFileName = parameters.get(key).toString();
                    fileNameList.add(subFileName);
                }
            }
            parameters.put(PAGE_COUNT, Integer.toString(fileNameList.size()));

            JasperPrint jpMaster = JasperFillManager.fillReport(jasperSource, parameters, (JRDataSource) null);
            System.err.println("Report... : Fill from : " + jasperSource);
            System.err.println("Filling time : " + (System.currentTimeMillis() - startTime));

            // convert sheetNames list to array, add the home item
            String[] sheetNames = new String[sheetNameList.size() + 1];
            sheetNames[0] = HOME_NAME;

            // create each sheet from the associated csv data file
            for (int i = 0; i < sheetNameList.size(); i++) {
                String sheetName = sheetNameList.get(i);

                // copy to the array
                sheetNames[i + 1] = sheetName;

                // set the page number in the report
                parameters.put(PAGE_NUMBER, Integer.toString(i + 1));
                File sourceFile = new File(jasperPath + File.separator + sheetNameList.get(i) + ".jasper");
                File dataFile = new File(dataPath + File.separator + fileNameList.get(i));
                System.err.println("Fill...sheet=" + sourceFile + " csv=" + dataFile + "\r\n");
                if (sourceFile.canRead() && dataFile.canRead()) {
                    System.err.println("Fill...t=" + (System.currentTimeMillis() - startTime));
                    JRCsvDataSource source = new JRCsvDataSource(JRLoader.getInputStream(dataFile));
                    source.setRecordDelimiter("\r\n");
                    source.setUseFirstRowAsHeader(true);
                    System.err.println("datasource loaded ");
                    JasperPrint jasperPrint = JasperFillManager.fillReport(sourceFile.getPath(), parameters, source);
                    // add all the pages into the master doc
                    List<JRPrintPage> pp = jasperPrint.getPages();
                    jpMaster.setName(sheetName);
                    for (int j = 0; j < pp.size(); j++) {
                        System.err.println("fill... : Add page : " + sheetName + "|" + j + 1);
                        jpMaster.addPage(pp.get(j));
                    }
                } else {
                    System.err.println("Fill...Error - cannot load files\r\n");
                }
            }

            // find the file type required
            if (type == null && parameters.containsKey(FILE_TYPE)) {
                type = parameters.get(FILE_TYPE).toString().toLowerCase();
            }
            // now generate the file according to the file type
            if (type != null && type.contains(TYPE_PDF)) {
                File destFile = new File(PDF_OUTPUT_FILE);
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

                System.err.println("pdf... : " + destFile + " creation time : " + (System.currentTimeMillis() - startTime));
            }
            // produce xlsx or xls, but not both
            if (type != null && type.contains(TYPE_XLSX)) {
                File destFile = new File(XLSX_OUTPUT_FILE);
                JRXlsxExporter exporter = new JRXlsxExporter();

                // export the final doc
                exporter.setExporterInput(new SimpleExporterInput(jpMaster));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

                // set the configuration for xls
                SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
                configuration.setOnePagePerSheet(true);
                configuration.setDetectCellType(true);
                configuration.setCollapseRowSpan(false);
                configuration.setSheetNames(sheetNames);
                exporter.setConfiguration(configuration);

                exporter.exportReport();

                System.err.println("xlsx... : " + destFile + " creation time : " + (System.currentTimeMillis() - startTime));
            } else if (type != null && type.contains(TYPE_XLS)) {
                File destFile = new File(XLS_OUTPUT_FILE);
                JRXlsExporter exporter = new JRXlsExporter();

                // export the final doc
                exporter.setExporterInput(new SimpleExporterInput(jpMaster));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(destFile));

                // set the configuration for xls
                SimpleXlsReportConfiguration configuration = new SimpleXlsReportConfiguration();
                configuration.setOnePagePerSheet(true);
                configuration.setDetectCellType(true);
                configuration.setCollapseRowSpan(false);
                configuration.setSheetNames(sheetNames);
                exporter.setConfiguration(configuration);

                exporter.exportReport();

                System.err.println("xls... : " + destFile + " creation time : " + (System.currentTimeMillis() - startTime));
            }
        }else {
        	System.err.println("no read" + reportName + jasperSource + " " + XLS_OUTPUT_FILE);
        }
    }
}
