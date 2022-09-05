package reliqreports;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.util.StringUtils;

import reliqreports.common.EReportCategory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AmazonDynamoDBConsumer {

    Region region;
    private final LambdaLogger logger;
    private final DynamoDbClient client;
    private final String tableName;
    private final String bucketName;

    public AmazonDynamoDBConsumer(LambdaLogger logger) {
        this.logger = logger;
        this.region = Region.of(ReportGeneratorConfig.getValue("aws.region"));

        this.client = DynamoDbClient.builder()
                .region(region)
                .build();
        this.tableName = System.getenv("DYNAMODB_TABLE");
        this.bucketName = System.getenv("FILES_BUCKET");
    }

    public void stageRecord(String filePath, String apiEndpoint, EReportCategory reportCategory, String entityId)
            throws IOException {
        try{
            if(isRecordStaged(filePath)){
                logger.log("Record already staged");
                return;
            }
            HashMap<String,AttributeValue> itemValues = new HashMap<String,AttributeValue>();

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String date = dateFormat.format(new Date());
            UUID recordId = UUID.randomUUID();

            itemValues.put("RecordId", AttributeValue.builder().s(recordId.toString()).build());
            
            if(!StringUtils.isNullOrEmpty(entityId)) {
                itemValues.put("EntityId", AttributeValue.builder().s(entityId).build());
            }            
            itemValues.put("FilePath", AttributeValue.builder().s(filePath).build());
            itemValues.put("Created", AttributeValue.builder().s(date).build());
            itemValues.put("DocumentCategory", AttributeValue.builder().n(reportCategory.category).build());
            itemValues.put("BucketName", AttributeValue.builder().s(this.bucketName).build());
            itemValues.put("ApiEndpoint", AttributeValue.builder().s(apiEndpoint).build());
            itemValues.put("UploadAttemps", AttributeValue.builder().n("0").build());
            itemValues.put("Status", AttributeValue.builder().n("0").build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(this.tableName).item(itemValues).build();

            this.client.putItem(request);
            this.logger.log("Staged record: " + filePath);
        } catch (DynamoDbException e) {
            this.logger.log("There was a error trying to stage the record: " + e.getMessage());
            throw e;
        }
    }

    public boolean isRecordStaged(String filePath) {
        Map<String, AttributeValue> itemValues = new HashMap<>();

        itemValues.put(":filePath", AttributeValue.builder().s(filePath).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(this.tableName)
                .filterExpression("FilePath = :filePath")
                .expressionAttributeValues(itemValues)
                .build();

        ScanResponse response = this.client.scan(request);
        
        List<Map<String, AttributeValue>> items = new ArrayList<Map<String, AttributeValue>>();
        items.addAll(response.items());
        
        while(response.hasLastEvaluatedKey() && response.lastEvaluatedKey().get("RecordId") != null && !StringUtils.isNullOrEmpty(response.lastEvaluatedKey().get("RecordId").s())) {
            request = ScanRequest.builder()
                    .tableName(this.tableName)
                    .filterExpression("FilePath = :filePath")
                    .expressionAttributeValues(itemValues)
                    .exclusiveStartKey(response.lastEvaluatedKey())
                    .build();
            response = this.client.scan(request);
            items.addAll(response.items());
        }      
        
        return items.size() > 0;
    }
}