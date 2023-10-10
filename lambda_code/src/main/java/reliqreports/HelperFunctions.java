package reliqreports;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.util.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import reliqreports.common.dto.StageZipRecordDto;
import reliqreports.common.enums.EProcessCategory;
import reliqreports.common.enums.EReportCategory;

public class HelperFunctions {

	private static String GUID_REG_EX = "\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}";
    private static final List<String> INDIVIDUAL_PATIENT_REPORTS = new ArrayList<>(
			List.of(
					ReportsLiterals.INDIVIDUAL_PATIENT_REPORT
			)
	);
	private static final List<String> BILLING_REPORTS = new ArrayList<>(
			List.of(
					ReportsLiterals.COMPLIANCE_BILLING_REPORT,
					ReportsLiterals.INSTANCE_COMPLIANCE_BILLING_REPORT,
					ReportsLiterals.CUSTOMER_BILLING_REPORT,
					ReportsLiterals.INSTANCE_CUSTOMER_BILLING_REPORT,
					ReportsLiterals.INDIVIDUAL_RPM_READINGS_REPORT,
					ReportsLiterals.INSTANCE_RPM_READINGS_REPORT,
					ReportsLiterals.MONTHLY_DATA_REPORT,
					ReportsLiterals.INSTANCE_SUMMARY_REPORT
			)
	);
    private static List<String> NON_STAGING_REPORTS = new ArrayList<String>(
			Arrays.asList(
					ReportsLiterals.EMERGENCY_RECOVERY_REPORT,
					ReportsLiterals.DAILY_CRITICAL_READINGS_REPORT
			)
	);

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

	public static boolean shouldStageReport(String report, Boolean shouldStageReport) {
		return shouldStageReport && !NON_STAGING_REPORTS.contains(report);
	}

	public static String getPreferredString(String firstString, String secondString) {
		if (!StringUtils.isNullOrEmpty(firstString)) {
			return firstString;
		}

		return secondString;
	}
	public static String getProcessCategoryFolderPath(EProcessCategory processCategory, StageZipRecordDto payload) {
		switch(processCategory) {
			case API_DELIVERY:
				return payload.reportPath;
			case TENANT_INDIVIDUAL_ZIP:
			case BILLING_ZIP:
				return payload.tenantFolderPath;
			case ORGANIZATION_ZIP:
				return payload.organizationFolderPath;
			default:
				return "";
		}
	}

	public static ArrayList<EProcessCategory> getRecordProcessCategories(StageZipRecordDto payload) {
		ArrayList<EProcessCategory> categories = new ArrayList<>();

		if (!StringUtils.isNullOrEmpty(payload.deliveryEndpoint)) {
			categories.add(EProcessCategory.API_DELIVERY);
		}

		if (shouldSaveZipRecord(payload.reportName)) {
			categories.add(EProcessCategory.TENANT_INDIVIDUAL_ZIP);
			if (!StringUtils.isNullOrEmpty(payload.organizationId)) {
				categories.add(EProcessCategory.ORGANIZATION_ZIP);
			}
		}

		if (shouldSaveBillingZipRecord(payload.reportName)) {
			categories.add(EProcessCategory.BILLING_ZIP);
		}

		return categories;
	}
	
	public static boolean shouldSaveZipRecord(String reportType) {
	    return INDIVIDUAL_PATIENT_REPORTS.contains(reportType);
	}

	public static boolean shouldSaveBillingZipRecord(String reportType) {
	    return BILLING_REPORTS.contains(reportType);
	}
	
	public static String getTypeOfReportFolderName(String reportType) {
	    switch(reportType) {
	        case ReportsLiterals.DAILY_CRITICAL_READINGS_REPORT:
	            return StringLiterals.DAILY;
	        case ReportsLiterals.WEEKLY_ADHERENCE_REPORT:
	            return StringLiterals.WEEKLY;
	        case ReportsLiterals.CUSTOMER_BILLING_REPORT:
	        case ReportsLiterals.EMERGENCY_RECOVERY_REPORT:
	        case ReportsLiterals.COMPLIANCE_BILLING_REPORT: 
	        case ReportsLiterals.INDIVIDUAL_RPM_READINGS_REPORT:
	        case ReportsLiterals.INDIVIDUAL_PATIENT_REPORT:
	        case ReportsLiterals.COMPLIANCE_DATA_REPORT_FOR_INSTANCE:
	        case ReportsLiterals.INSTANCE_COMPLIANCE_BILLING_REPORT:
            case ReportsLiterals.INSTANCE_CUSTOMER_BILLING_REPORT:
            case ReportsLiterals.INSTANCE_RPM_READINGS_REPORT:
	        default:
	            return StringLiterals.MONTLY;
	    }
	}
}
