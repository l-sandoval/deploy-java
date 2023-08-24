package reliqreports.common.dto;

import java.io.*;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.type.TypeReference;
import org.json.simple.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reliqreports.ReportsLiterals;

public class ReportGeneratorDto {

    public JSONObject environmentsApiEndpoints;
    public String[] environments;
    public String[] reportsToBeGenerated;
    public Map<String, Map<String, String[]>> xmlFiles;
    public String generationDate;
    public String reportPeriodDate;
    public String entityId;
    public String entityName;
    public String organizationId;
    public String organizationName;
    public Boolean shouldStageReport;
    public String reportPath;
    public String deliveryEndpoint;
    public String reportToBeStaged;

    public String logoPath;

    private ObjectMapper objectMapper;

    public ReportGeneratorDto(InputStream inputStream) throws IOException {
        super();
        this.objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(inputStream);

        this.environments = readValueFromJsonNode(rootNode, "environments", String[].class);
        this.reportsToBeGenerated = readValueFromJsonNode(rootNode, "reportsToBeGenerated", String[].class, new String[] {});
        this.xmlFiles = readValueFromJsonNode(rootNode, "xmlFiles", new TypeReference<Map<String, Map<String, String[]>>>() {});
        this.generationDate = readValueFromJsonNode(rootNode, "generationDate", String.class);
        this.reportPeriodDate = readValueFromJsonNode(rootNode, "reportPeriodDate", String.class);
        this.entityId = readValueFromJsonNode(rootNode, "entityId", String.class);
        this.entityName = readValueFromJsonNode(rootNode, "entityName", String.class);
        this.organizationId = readValueFromJsonNode(rootNode, "organizationId", String.class);
        this.organizationName = readValueFromJsonNode(rootNode, "organizationName", String.class);
        this.shouldStageReport = readValueFromJsonNode(rootNode, "shouldStage", Boolean.class, false);
        this.reportPath = readValueFromJsonNode(rootNode, "reportPath", String.class);
        this.deliveryEndpoint = readValueFromJsonNode(rootNode, "deliveryEndpoint", String.class);
        this.reportToBeStaged = readValueFromJsonNode(rootNode, "reportToBeStaged", String.class);

        this.logoPath = reportsToBeGenerated.length > 0 && Objects.equals(this.reportsToBeGenerated[0], ReportsLiterals.INDIVIDUAL_PATIENT_REPORT)
                ? "s3path.IUGOReport-Logo-individualPatientReport" : "s3path.IUGOReport-Logo";
    }

    private <T> T readValueFromJsonNode(JsonNode node, String field, Class<T> valueType) throws IOException {
        return node.get(field) != null ? this.objectMapper.readValue(node.get(field).toString(), valueType) : null;
    }

    private <T> T readValueFromJsonNode(JsonNode node, String field, TypeReference<T> valueType) throws IOException {
        return node.get(field) != null ? this.objectMapper.readValue(node.get(field).toString(), valueType) : null;
    }

    private <T> T readValueFromJsonNode(JsonNode node, String field, Class<T> valueType, T defaultValue) throws IOException {
        return node.get(field) != null ? this.objectMapper.readValue(node.get(field).toString(), valueType) : defaultValue;
    }

}