package lk.ijse.dep10.app;

import javafx.application.Application;
import javafx.stage.Stage;
import lk.ijse.dep10.app.db.DBConnection;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Set;

public class AppInitializer extends Application {

    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            try {
                if ( DBConnection.getInstance().getConnection() !=null &&
                        !DBConnection.getInstance().getConnection().isClosed()) {
                    System.out.println("Database connection is about to close");
                    DBConnection.getInstance().getConnection().close();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));

        launch(args);

    }

    @Override
    public void start(Stage primaryStage) {
        generateSchemaIfNotExists();
    }

    private void generateSchemaIfNotExists() {
        Connection connection = DBConnection.getInstance().getConnection();
        Statement stm = null;
        try {
            stm = connection.createStatement();
            ResultSet rst = stm.executeQuery("SHOW TABLES");

            ArrayList<String> tableNameList = new ArrayList<>();
            while (rst.next()) {
                tableNameList.add(rst.getString(1));
            }

            boolean tableExists = tableNameList.containsAll(Set.of("Attendance", "Picture", "Student", "User"));

            if (!tableExists) {
                stm.execute(readDBScript());
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    private String readDBScript() {
        System.out.println("dbscript read");
        InputStream is = getClass().getResourceAsStream("/schema.sql");

        try(BufferedReader bf = new BufferedReader(new InputStreamReader(is))){
            String line;
            StringBuilder dbScriptBuilder = new StringBuilder();
            while ((line = bf.readLine()) != null ) {
                dbScriptBuilder.append(line);
            }
            return dbScriptBuilder.toString();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
