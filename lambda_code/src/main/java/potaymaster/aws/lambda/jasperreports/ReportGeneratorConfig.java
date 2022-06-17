package potaymaster.aws.lambda.jasperreports;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ReportGeneratorConfig {
	public Properties config = new Properties();
	public ReportGeneratorConfig() throws IOException {
		ClassLoader cl = ReportGeneratorConfig.class.getClassLoader();
		InputStream input = cl.getResourceAsStream("config.properties");
		this.config.load(input);
	}
	public String get(String value){
		return this.config.getProperty(value);
	}
}