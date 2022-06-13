package potaymaster.aws.lambda.jasperreports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseBytes;

public class AmazonS3Consumer {
	Region region;
	private LambdaLogger logger;
	private ReportGeneratorConfig config;

	public AmazonS3Consumer(LambdaLogger logger, ReportGeneratorConfig reportGeneratorConfig) {
		this.config = reportGeneratorConfig;
		this.logger = logger;
		this.region = Region.of(config.get("aws.region"));
	}
	
	public void retrieveTemplateFromS3(String key_name) throws IOException {
		String bucket_name = System.getenv("BUCKET_NAME");
    	logger.log("Downloading file " + key_name + " from bucket " + bucket_name);
		
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();

		try {
			GetObjectRequest objectRequest = GetObjectRequest.builder()
				.bucket(bucket_name)
				.key(key_name)
				.build();
		
            ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
            byte[] data = objectBytes.asByteArray();

            File myFile = new File(this.config.get("temp.template"));
            OutputStream os = new FileOutputStream(myFile);
            os.write(data);
            logger.log("Successfully obtained bytes from an S3 object");
            os.close();

        } catch (IOException ex) {
            logger.log("There was an error when reading template file: " + ex.getMessage());
			throw ex;
        } catch (S3Exception e) {
			logger.log("There was an error when creating the output template file: " + e.getMessage());
		   throw e;
        }
    }
	
	public void retrieveCSVFromS3() throws IOException {
		String bucket_name = System.getenv("BUCKET_NAME");
		//TODO: Retrieve the corresponding CSV name according with the report 
		String csv_name = "test.csv";
		
    	logger.log("Downloading file " + csv_name + " from bucket " + bucket_name);

        S3Client s3 = S3Client.builder()
                .region(region)
                .build();

		try {
			GetObjectRequest objectRequest = GetObjectRequest.builder()
				.bucket(bucket_name)
				.key(csv_name)
				.build();
		
            ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
            byte[] data = objectBytes.asByteArray();

            File myFile = new File(this.config.get("temp.csv"));
            OutputStream os = new FileOutputStream(myFile);
            os.write(data);
            logger.log("Successfully obtained bytes from an S3 object");
            os.close();

        } catch (IOException ex) {         	
            logger.log("There was an error when reading CSV file: " + ex.getMessage());            
			throw ex;
			
        } catch (S3Exception e) {        	
			logger.log("There was an error when creating the output temporal CSV file: " + e.getMessage());	
			throw e;		   
        }
    }
}