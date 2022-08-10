package reliqreports;

import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import java.util.Properties;

import org.slf4j.LoggerFactory;

public class ReportGeneratorConfig {
    
    private static final Logger LOG  = LoggerFactory.getLogger(ReportGeneratorConfig.class);    
	private static Properties config = null;
		
	public static String getValue(String key) {
	    loadProperties();
        return config.getProperty(key);
    }
    
    private static void loadProperties() {
        if(config == null) {
            config = new Properties();
            
            try {
                InputStream inputStream = ReportGeneratorConfig.class
                        .getClassLoader().getResourceAsStream("config.properties");
                    
                config.load(inputStream);
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }

}