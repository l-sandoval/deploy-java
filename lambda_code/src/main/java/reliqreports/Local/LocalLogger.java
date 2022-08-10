package reliqreports.Local;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

public class LocalLogger implements LambdaLogger {
    public void log(String message) {
        System.out.println(message);
    }

    public void log(byte[] message) {
        System.out.println(new String(message));
    }
}
