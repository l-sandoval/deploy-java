package reliqreports;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import reliqreports.common.EReportCategory;

import java.util.List;

public class HelperFunctions {

	private static String GUID_REG_EX = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";
    private static List<String> INDIVIDUAL_PATIENT_REPORTS = new ArrayList<String>(Arrays.asList(ReportsLiterals.INDIVIDUAL_PATIENT_REPORT));

	/*
	 * Decode xml
	 */
	public static boolean extractXml(Node node, String name, Map<String, Object> parameters) {

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
				}
				break;
			default:
				break;
			}
		}
		return true;
	}

	public static String extractEntityId(String filePath){
		Pattern pairRegex = Pattern.compile(GUID_REG_EX);
		Matcher matcher = pairRegex.matcher(filePath);
		String result = "";

		while (matcher.find()) {
			result = matcher.group(0);
		}
		return result;
	}

	public static EReportCategory getReportCategory(String report) {
	    EReportCategory category = EReportCategory.ORGANIZATION;
		if (INDIVIDUAL_PATIENT_REPORTS.contains(report))
			category = EReportCategory.PATIENT;

		return category;
	}

	public static boolean shouldStageReport(String report) {
		String[] reportsToAvoidStaging = {
				ReportsLiterals.EMERGENCY_RECOVERY_REPORT
		};

		return !Arrays.asList(reportsToAvoidStaging).contains(report);
	}
	
	public static boolean shouldSaveZipRecord(String reportType) {
	    return INDIVIDUAL_PATIENT_REPORTS.contains(reportType);
	}
}
