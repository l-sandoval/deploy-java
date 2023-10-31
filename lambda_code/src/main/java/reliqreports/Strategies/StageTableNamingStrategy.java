package reliqreports.Strategies;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import reliqreports.StringLiterals;
import software.amazon.awssdk.utils.StringUtils;


public class StageTableNamingStrategy extends PhysicalNamingStrategyStandardImpl {
    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        String tableName = logicalName.getText();
        String environmentTableName = System.getenv("STAGING_RECORDS_TABLE_NAME");

        if (
                tableName.equals(StringLiterals.DEFAULT_STAGING_RECORDS_TABLE_NAME)
                && !StringUtils.isEmpty(environmentTableName)
        ) {
            tableName = environmentTableName;
        }
        return jdbcEnvironment.getIdentifierHelper().toIdentifier(
                tableName,
                logicalName.isQuoted()
        );
    }
}
