package potaymaster.aws.lambda.jasperreports.Local;

import com.amazonaws.services.lambda.runtime.Context;
import potaymaster.aws.lambda.jasperreports.LambdaFunctionHandler;

import java.io.*;

public class Local {

    public static void main(String[] args) throws IOException {
        LambdaFunctionHandler handler = new LambdaFunctionHandler();
        ClassLoader cl = Local.class.getClassLoader();
        InputStream inputStream = cl.getResourceAsStream("local_execution_parameters.json");
        OutputStream outputStream = new ByteArrayOutputStream();
        Context context = new LocalContext();

        handler.handleRequest(inputStream, outputStream, context);
    }
}
