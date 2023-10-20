package reliqreports.Services;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.hibernate.SessionFactory;
import reliqreports.Config.HibernateConfig;

import java.sql.SQLException;

public class StagingService {

    public SessionFactory sessionFactory;
    private LambdaLogger logger;
    public StagingService(LambdaLogger logger) throws SQLException {
         this.sessionFactory = HibernateConfig.getInstance(logger).getSessionFactory();
         this.logger = logger;
    }
}
