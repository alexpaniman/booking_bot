package org.database;

import java.sql.Connection;

public interface DataBaseConnector {
    void connect();
    Connection getConnection();
}
