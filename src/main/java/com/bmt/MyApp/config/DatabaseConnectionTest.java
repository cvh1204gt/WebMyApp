package com.bmt.MyApp.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.sql.Connection;

@Component
public class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @EventListener(ApplicationReadyEvent.class)
    public void testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("✅ Database connected successfully: " + conn.getMetaData().getURL());
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to DB:");
            e.printStackTrace();
        }
    }
}
