import org.database.DataBaseConnector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Heroku implements DataBaseConnector {
    private Connection connection;
    public String url;


    public Heroku() {
    }



    public void connect() {
        String dbUrl = System.getenv("JDBC_DATABASE_URL");
        url = dbUrl;
        try {
            connection = DriverManager.getConnection(dbUrl);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
