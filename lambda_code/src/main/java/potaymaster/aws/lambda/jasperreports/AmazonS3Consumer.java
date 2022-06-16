package potaymaster.aws.lambda.jasperreports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;

public class AmazonS3Consumer {
    
	Region region;
	private LambdaLogger logger;
	private ReportGeneratorConfig config;

	public AmazonS3Consumer(LambdaLogger logger, ReportGeneratorConfig reportGeneratorConfig) {
		this.config = reportGeneratorConfig;
		this.logger = logger;
		this.region = Region.of(config.get("aws.region"));
	}	

	public void retrieveFileFromS3(String key_name, String file_type) throws IOException {
		String tmp_file = "";
		switch (file_type) {
			case StringLiterals.TEMPLATE: 
				tmp_file = StringLiterals.TMP_TEMPLATE; break;
			case StringLiterals.CSV: 
				tmp_file = StringLiterals.TMP_CSV; break;
			case StringLiterals.IMAGE: 
				tmp_file = StringLiterals.TMP_IMAGE; break;
			case StringLiterals.XML: 
				tmp_file = StringLiterals.TMP_XML; break;
			default: break;
		}

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

            File myFile = new File(tmp_file);
            OutputStream os = new FileOutputStream(myFile);
            os.write(data);
            logger.log("Successfully obtained bytes from an S3 object");
            os.close();

        } catch (IOException ex) {
        	logger.log("There was an error when reading file from S3: " + ex.getMessage());
			throw ex;
        } catch (S3Exception e) {
        	logger.log("There was an error when creating the output temporal file: " + e.getMessage());
		   throw e;
        }
    }
	
	public void uploadFileToS3(String key_name, byte[] bytes) throws IOException {

		String bucket_name = System.getenv("BUCKET_NAME");
		logger.log("Uploading file " + key_name + " to bucket " + bucket_name);
		
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();

		try {
			PutObjectRequest objectRequest = PutObjectRequest.builder()
				.bucket(bucket_name)
				.key(key_name)
				.build();
		
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
            s3.putObject(objectRequest, RequestBody.fromByteBuffer(buffer));
            
            logger.log("Successfully obtained bytes from an S3 object");
        } catch (S3Exception e) {
        	logger.log("There was an error uploading the output report file: " + e.getMessage());
		   throw e;
        }
    }
}