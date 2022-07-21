package potaymaster.aws.lambda.jasperreports;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class HelperFunctions {

	private LambdaLogger logger;
	private String guidRegEx = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";

	public HelperFunctions(LambdaLogger logger) {
		this.logger = logger;
	}

	/*
	 * Decode xml
	 */
	public boolean extractXml(Node node, String name, Map<String, Object> parameters) {

		if (node == null) return false;

		String nodeName = name != null ? name + "-" + node.getNodeName() : node.getNodeName();

		NodeList nodeList = node.getChildNodes();
		for (int i = 0; i < nodeList.getLength(); i++) {
			switch (nodeList.item(i).getNodeType()) {
			case Node.ELEMENT_NODE:
				if (nodeList.item(i).hasChildNodes()) {
					extractXml(nodeList.item(i), nodeName, parameters);
				}
				break;
			case Node.TEXT_NODE:
				String value = nodeList.item(i).getNodeValue().trim();
				if (!value.isEmpty()) {
					parameters.put(nodeName, value);
					logger.log("extractXml : Add to map: " + nodeName + " | " + value);
				}
				break;
			default:
				break;
			}
		}
		return true;
	}

	public String extractEntityId(String filePath){
		Pattern pairRegex = Pattern.compile(this.guidRegEx);
		Matcher matcher = pairRegex.matcher(filePath);
		String result = "";

		logger.log("extractEntityId : filePath: " + filePath);
		while (matcher.find()) {
			result = matcher.group(0);
		}
		return result;
	}

	public ReportsLiterals.REPORT_CATEGORY getReportCategory(String report) {
		String[] individualPatientReports = {
				ReportsLiterals.INDIVIDUAL_PATIENT_REPORT
		};
		ReportsLiterals.REPORT_CATEGORY category = ReportsLiterals.REPORT_CATEGORY.ORGANIZATION;
		if (Arrays.asList(individualPatientReports).contains(report))
			category = ReportsLiterals.REPORT_CATEGORY.PATIENT;

		return category;
	}

	public boolean shouldStageReport(String report) {
		String[] reportsToAvoidStaging = {
				ReportsLiterals.EMERGENCY_RECOVERY_REPORT
		};

		return !Arrays.asList(reportsToAvoidStaging).contains(report);
	}
}
