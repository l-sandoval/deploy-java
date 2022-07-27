package reliqreports.Local;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalLogger implements LambdaLogger {
    public void log(String message) {
        System.out.println(message);
    }

    public void log(byte[] message) {
        System.out.println(new String(message));
    }
}
