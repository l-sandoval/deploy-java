package reliqreports.Config;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import reliqreports.Strategies.StageTableNamingStrategy;

public class HibernateConfig {
    private static HibernateConfig instance;
    private SessionFactory sessionFactory;

    private HibernateConfig(LambdaLogger logger) {
        try {
            Configuration configuration = new Configuration();

            configuration.setProperty("hibernate.connection.url", System.getenv("SQL_CONNECTION_URL"));
            configuration.setProperty("hibernate.connection.username", System.getenv("SQL_USERNAME"));
            configuration.setProperty("hibernate.connection.password", System.getenv("SQL_PASSWORD"));

            // strategy to use to set the staging table name to the value in the environment variables
            configuration.setPhysicalNamingStrategy(new StageTableNamingStrategy());

            configuration.configure("hibernate.cfg.xml");

            sessionFactory = configuration.buildSessionFactory();
        } catch (Exception e) {
            logger.log("There was an error setting up configuration for database: " + e.getMessage());
            throw e;
        }
    }

    public static HibernateConfig getInstance(LambdaLogger logger) {
        if (instance == null) {
            instance = new HibernateConfig(logger);
        }
        return instance;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
