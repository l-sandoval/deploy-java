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
    
	Region region = Region.US_EAST_2; //TODO: Move to config file IPM-7990
	
	private LambdaLogger logger;

	public AmazonS3Consumer(LambdaLogger logger) {
		this.logger = logger;
	}	

	public void retrieveFileFromS3(String key_name, String file_type) throws IOException {
		String tmp_file = "";
		switch (file_type) {
			case StringLiterals.TEMPLATE: 
				tmp_file = StringLiterals.tmpTemplate; break;
			case StringLiterals.CSV: 
				tmp_file = StringLiterals.tmpCSV; break;
			case StringLiterals.IMAGE: 
				tmp_file = StringLiterals.tmpImage; break;
			case StringLiterals.XML: 
				tmp_file = StringLiterals.tmpXML; break;
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
}