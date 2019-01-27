package org.telegram.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class URLConnector implements DataBaseConnector {
    private String URL;
    private Connection connection;

    public URLConnector(String URL) {
        this.URL = URL;
    }

    @Override
    public void connect() {
        try {
            connection = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }
}
