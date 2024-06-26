package com.example.memic.common.database;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class DatabaseCleaner {

    @SneakyThrows
    public static void clear(ApplicationContext applicationContext) {
        var entityManager = applicationContext.getBean(EntityManager.class);
        var jdbcTemplate = applicationContext.getBean(JdbcTemplate.class);
        var transactionTemplate = applicationContext.getBean(TransactionTemplate.class);

        try (var connection = jdbcTemplate.getDataSource().getConnection()) {
            if (!connection.getMetaData().getDriverName().contains("H2")) {
                throw new UnsupportedOperationException("Database Cleaning not supported.");
            }
        }

        transactionTemplate.execute(status -> {
            entityManager.clear();
            deleteAll(jdbcTemplate, entityManager);
            return null;
        });
    }

    private static void deleteAll(JdbcTemplate jdbcTemplate, EntityManager entityManager) {
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
        for (String tableName : findDatabaseTableNames(jdbcTemplate)) {
            entityManager.createNativeQuery("TRUNCATE TABLE %s".formatted(tableName)).executeUpdate();
        }
        entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
    }

    private static List<String> findDatabaseTableNames(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate
                .query("SHOW TABLES", (rs, rowNum) -> rs.getString(1))
                .stream().toList();
    }
}
