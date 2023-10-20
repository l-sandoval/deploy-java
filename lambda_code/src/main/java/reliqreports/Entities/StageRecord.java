package reliqreports.Entities;

import jakarta.persistence.*;
import reliqreports.StringLiterals;
import reliqreports.common.enums.EProcessCategory;
import reliqreports.common.enums.EReportCategory;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(
        name = StringLiterals.DEFAULT_STAGING_RECORDS_TABLE_NAME,
        schema = "dbo"
)
public class StageRecord {
    @Id
    @Column(name = "RecordId", columnDefinition = "uniqueidentifier")
    @GeneratedValue(generator = "uuid2")
    private UUID recordId;

    @Column(name = "Created", nullable = false)
    private Date created;

    @Column(name = "ApiEndpoint", nullable = false)
    private String apiEndpoint;

    @Column(name = "BucketName", nullable = false)
    private String bucketName;

    @Column(name = "DocumentCategory", nullable = false)
    private Integer documentCategory;

    @Column(
            name = "EntityId",
            columnDefinition = "uniqueidentifier",
            nullable = false
    )
    private UUID entityId;

    @Column(name = "FilePath", nullable = false)
    private String filePath;

    @Column(name = "ProcessCategory", nullable = false)
    private String processCategory;

    @Column(
            name = "Status",
            nullable = false,
            columnDefinition = "smallint"
    )
    private Integer status = 0;

    @Column(name = "UploadAttempts", nullable = false)
    private Integer uploadAttempts = 0;

    @Column(name = "ErrorMessage")
    private String errorMessage;

    public StageRecord(
            Date created,
            String apiEndpoint,
            String bucketName,
            EReportCategory documentCategory,
            UUID entityId,
            String filePath,
            EProcessCategory processCategory
    ) {
        this.created = created;
        this.apiEndpoint = apiEndpoint;
        this.bucketName = bucketName;
        this.documentCategory = Integer.parseInt(documentCategory.category);
        this.entityId = entityId;
        this.filePath = filePath;
        this.processCategory = processCategory.category;
    }
}
