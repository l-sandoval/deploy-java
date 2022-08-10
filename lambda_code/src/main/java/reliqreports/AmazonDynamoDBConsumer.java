package reliqreports;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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

        this.createTableIfNotExists();
    }

    public void stageRecord(String filePath, String apiEndpoint, ReportsLiterals.REPORT_CATEGORY reportCategory, String entityId)
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

            String category = "";

            switch (reportCategory) {
                case PATIENT:
                    category = "10"; break;
                case ORGANIZATION:
                    category = "12"; break;
                default: break;
            }

            itemValues.put("RecordId", AttributeValue.builder().s(recordId.toString()).build());
            itemValues.put("EntityId", AttributeValue.builder().s(entityId).build());
            itemValues.put("FilePath", AttributeValue.builder().s(filePath).build());
            itemValues.put("Created", AttributeValue.builder().s(date).build());
            itemValues.put("DocumentCategory", AttributeValue.builder().s(category).build());
            itemValues.put("BucketName", AttributeValue.builder().s(this.bucketName).build());
            itemValues.put("ApiEndpoint", AttributeValue.builder().s(apiEndpoint).build());
            itemValues.put("UploadAttemps", AttributeValue.builder().n("0").build());

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
        AtomicBoolean isStaged = new AtomicBoolean(false);

        ScanResponse response = this.client.scan(ScanRequest.builder().tableName(this.tableName).build());

        response.items().forEach(item -> {
            if(item.get("FilePath").s().equals(filePath)){
                isStaged.set(true);
            }
        });

        return isStaged.get();
    }

    public void createTableIfNotExists(){
        ListTablesResponse tables =  this.client.listTables();
        boolean tableExists = tables.tableNames().contains(this.tableName);

        if(!tableExists){
            CreateTableRequest request = CreateTableRequest.builder()
                    .attributeDefinitions(
                        AttributeDefinition.builder()
                            .attributeName("RecordId")
                            .attributeType(ScalarAttributeType.S)
                            .build(),
                        AttributeDefinition.builder()
                                .attributeName("Created")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                    .keySchema(
                        KeySchemaElement.builder()
                            .attributeName("RecordId")
                            .keyType(KeyType.HASH)
                            .build(),
                        KeySchemaElement.builder()
                                .attributeName("Created")
                                .keyType(KeyType.RANGE)
                                .build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(10L)
                            .writeCapacityUnits(10L)
                            .build())
                    .tableName(tableName)
                    .build();

            this.client.createTable(request);
            DescribeTableRequest tableRequest = DescribeTableRequest.builder()
                    .tableName(tableName)
                    .build();
            DynamoDbWaiter dbWaiter = this.client.waiter();

            // Wait until the Amazon DynamoDB table is created
            WaiterResponse<DescribeTableResponse> waiterResponse =  dbWaiter.waitUntilTableExists(tableRequest);
            waiterResponse.matched().response().ifPresent(System.out::println);
        }
    }

}