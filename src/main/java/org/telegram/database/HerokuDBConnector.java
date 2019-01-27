package org.telegram.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class HerokuDBConnector implements DataBaseConnector {

    private Connection connection;

    @Override
    public void connect() {
        String dbUrl = System.getenv("JDBC_DATABASE_URL");
        try {
            connection = DriverManager.getConnection(dbUrl);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }
}
