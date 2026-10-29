package com.feedback;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBUtil: the ONLY place where MySQL connection settings live.
 * Every servlet calls DBUtil.getConnection() to talk to the database.
 */
public class DBUtil {

    // ====== CHANGE THESE THREE VALUES TO YOUR OWN MYSQL SETTINGS ======
    private static final String URL =
        "jdbc:mysql://localhost:3306/feedback_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";                  // your MySQL username
    private static final String PASSWORD = "@Anshul123";   // your MySQL password
    // ==================================================================

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    // Private constructor: nobody needs to create a DBUtil object
    private DBUtil() {
    }

    /**
     * Opens and returns a new connection to MySQL.
     * The caller must close it (we use try-with-resources for that).
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Loads the MySQL driver class (makes sure Tomcat finds it)
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found. Is the Connector/J JAR in WEB-INF/lib?", e);
        }
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}