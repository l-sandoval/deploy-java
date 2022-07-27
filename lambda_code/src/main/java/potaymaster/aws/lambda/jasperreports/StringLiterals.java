package potaymaster.aws.lambda.jasperreports;

import java.io.File;

public class StringLiterals {

	public static final String tmpdir = System.getProperty("java.io.tmpdir");
	public static final String FILE_SEPARATOR_FOR_S3_QUERIES = "/";
	public static final String REPORT_NAME_SUFFIX = "iugoreport_";
	public static final String TMP_OUT_FILE_PDF = tmpdir + File.separator + "Reports.pdf";
	public static final String TMP_OUT_FILE_XLS =  tmpdir + File.separator + "Reports.xls";
	public static final String TMP_OUT_FILE_XLSX =  tmpdir + File.separator + "Reports.xlsx";
	
	public static final String TMP_TEMPLATE =  tmpdir + File.separator + "template.jrxml";
	public static final String TMP_CSV =  tmpdir + File.separator + "rawdata.csv";
	public static final String TMP_IMAGE =  tmpdir + File.separator + "image.png";
	public static final String TMP_XML =  tmpdir + File.separator + "dataxml.xml";
	public static final String TMP_JSON =  tmpdir + File.separator + "datajson.json";

	public static final String TEMPLATE = "template";
	public static final String CSV = "csv";
	public static final String IMAGE = "image";
	public static final String XML = "xml";
	public static final String JSON = "json";

	public static final String IUGOLOGO = "IUGOReport-Logo";
	public static final String PAGE_NUMBER = "IUGOReport-PageNumber";
	public static final String PAGE_COUNT = "IUGOReport-PageCount";
	public static final String FILE_TYPE = "IUGOReport-FileType";
	public static final String SUBREPORT = "subreport";
	public static final String TYPE_PDF = "pdf";
	public static final String TYPE_XLSX = "xlsx";
	public static final String TYPE_XLS = "xls";
	public static final String HOME_NAME = "Home";	
	
	public static final String FILENAME_FIELD_SEPARATOR = "-";

	public static final String LAMBDA_BUCKET = "lambda-bucket";
	public static final String FILES_BUCKET = "files-bucket";
}
