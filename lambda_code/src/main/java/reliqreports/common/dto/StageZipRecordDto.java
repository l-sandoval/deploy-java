package reliqreports.common.dto;

import reliqreports.HelperFunctions;

public class StageZipRecordDto {
    public String tenantFolderPath;
    public String organizationFolderPath;
    public String apiEndpoint;
    public String entityId;
    public String reportName;
    public String deliveryEndpoint;
    public String organizationId;
    public String reportPath;

    public StageZipRecordDto(ReportGeneratorDto payload) {
        super();
        this.tenantFolderPath = payload.buildPath;
        this.apiEndpoint = payload.apiEndpoint;
        this.entityId = payload.entityId;
        this.reportName = payload.reportName;
        this.organizationId = HelperFunctions.getPreferredString(payload.primaryOrganizationId, payload.organizationId);
    }

    public StageZipRecordDto(ReportsGeneratorHandlerDto payload) {
        super();
        this.tenantFolderPath = payload.reportPath;
        this.entityId = payload.entityId;
        this.reportName = HelperFunctions.getPreferredString(payload.reportToBeStaged, payload.reportsToBeGenerated[0]);
        this.deliveryEndpoint = payload.deliveryEndpoint;
        this.organizationId = HelperFunctions.getPreferredString(payload.primaryOrganizationId, payload.organizationId);
        this.apiEndpoint = HelperFunctions.getPreferredString(
                payload.deliveryEndpoint,
                payload.environmentsApiEndpoints.get(payload.environments[0]).toString()
        );
        this.reportPath = payload.reportPath;
    }
}
