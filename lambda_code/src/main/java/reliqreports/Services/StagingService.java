package reliqreports.Services;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import reliqreports.Config.HibernateConfig;
import reliqreports.Entities.StageRecord;
import reliqreports.HelperFunctions;
import reliqreports.common.dto.StageZipRecordDto;
import reliqreports.common.enums.EProcessCategory;
import reliqreports.common.enums.EReportCategory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class StagingService {

    public SessionFactory sessionFactory;
    private LambdaLogger logger;
    private final String bucketName;
    public StagingService(LambdaLogger logger) {
         this.sessionFactory = HibernateConfig.getInstance(logger).getSessionFactory();
         this.logger = logger;
        this.bucketName = System.getenv("REPORTS_BUCKET");
    }

    public void stageZipRecords(StageZipRecordDto payload){
        ArrayList<EProcessCategory> categories = HelperFunctions.getRecordProcessCategories(payload);

        for (EProcessCategory category : categories) {
            String folderPath = HelperFunctions.getProcessCategoryFolderPath(category, payload);
            try {
                this.stageRecord(
                        folderPath,
                        payload.apiEndpoint,
                        EReportCategory.ORGANIZATION,
                        payload.organizationId,
                        category
                );
            } catch (IOException e) {
                this.logger.log("There was a error trying to stage zip the record "+category+" : " + e.getMessage());
            }
        }
    }

    public void stageRecord(
            String filePath,
            String apiEndpoint,
            EReportCategory reportCategory,
            String entityId,
            EProcessCategory processCategory
    ) throws IOException {
        Session session = this.sessionFactory.getCurrentSession();
        try {
            session.beginTransaction();
            if (this.isRecordStaged(filePath, processCategory, new Date())) {
                this.logger.log("Record already staged for file path: " + filePath + " and process category: " + processCategory.category);
                session.close();
                return;
            }

            StageRecord record = new StageRecord(
                    new Date(),
                    apiEndpoint,
                    this.bucketName,
                    reportCategory,
                    UUID.fromString(entityId),
                    filePath,
                    processCategory
            );

            this.logger.log("Staging record for " +processCategory+" with body: " + record.toString());
            session.persist(record);
            session.getTransaction().commit();
            session.close();
        } catch (Exception e) {
            this.logger.log("Error while staging record: " + e.getMessage());
            session.getTransaction().rollback();
            session.close();
            throw e;
        }
    }

    public boolean isRecordStaged(String filePath, EProcessCategory processCategory, Date currentDate) {
        Session session = this.sessionFactory.getCurrentSession();
        try {
            CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
            CriteriaQuery<StageRecord> criteriaQuery = criteriaBuilder.createQuery(StageRecord.class);
            Root<StageRecord> root = criteriaQuery.from(StageRecord.class);

            Predicate finalCondition = criteriaBuilder.and(
                criteriaBuilder.equal(root.get("filePath"), filePath),
                criteriaBuilder.equal(root.get("processCategory"), processCategory.category)
            );

            criteriaQuery.select(root).where(finalCondition);
            Query query = session.createQuery(criteriaQuery);

            return !query.getResultList().isEmpty();

        } catch (Exception e) {
            this.logger.log("Error while staging record: " + e.getMessage());
            throw e;
        }
    }
}
