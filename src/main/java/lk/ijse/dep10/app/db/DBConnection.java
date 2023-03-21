package lk.ijse.dep10.app.db;

import javafx.scene.control.Alert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static DBConnection dbConnection;
    private final Connection connection;

    private DBConnection() {
        Properties configurations = new Properties();
        File file = new File("application.properties");

        try {
            FileReader fr = new FileReader(file);
            configurations.load(fr);
            fr.close();

            String host = configurations.getProperty("dep10.sas.db.shot", "localhost");
            String port = configurations.getProperty("dep10.sas.db.port", "3306");
            String database = configurations.getProperty("dep10.sas.db.name", "dep10_attendance_system");
            String username = configurations.getProperty("dep10.sas.db.username", "root");
            String password = configurations.getProperty("dep10.sas.db.password", ""); // <-

            String querryString = "createDatabaseIfNotExist=true&allowMultiQueries=true";
            String url = String.format("jdbc:mysql://%s:%s/%s?%s", host, port, database, querryString);
            try {
                connection = DriverManager.getConnection(url, username, password);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }


        } catch (FileNotFoundException e) {
            new Alert(Alert.AlertType.ERROR, "Configuration file doesn't exists").showAndWait();
            throw new RuntimeException(e);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to read configurations").showAndWait();
            throw new RuntimeException(e);
        }
    }

    public static DBConnection getInstance() {
        return (dbConnection==null)? dbConnection=new DBConnection(): dbConnection;
    }

    public Connection getConnection() {
        return connection;
    }
}
