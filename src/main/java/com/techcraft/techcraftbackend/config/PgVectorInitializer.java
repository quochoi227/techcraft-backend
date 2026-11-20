package com.techcraft.techcraftbackend.config;

import com.pgvector.PGvector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Component
@Order(1)
public class PgVectorInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    public PgVectorInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector;");
            PGvector.registerTypes(conn);
            log.info("Initialized pgvector extension and registered PGvector types successfully.");
        } catch (SQLException e) {
            log.warn("Could not initialize pgvector extension or register types: {}", e.getMessage());
        }
    }
}
