package dragonpass.project;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import monitor.FileObserver;
import monitor.Observer;
import monitor.SQLiteObserver;
import monitor.Subject;


import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {
    Subject monitor = new Subject();
    Observer observe1 = new SQLiteObserver(monitor);
    Observer observe2 = new FileObserver(monitor);

    Connection conn = null;

    @Override
    public void start(Stage stage) throws IOException, SQLException {

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login.fxml")); //loads login page
        Scene scene = new Scene(fxmlLoader.load(), 750, 436);
        stage.setTitle("Dragon Pass");
        stage.setScene(scene);
        stage.show();
        monitor.setLog(new model.Logger("info", "Application startup successful"));


    }

    public static void main(String[] args) {
        launch();
    }

    public static Logger LOGGER = Logger.getLogger(Main.class.getName());


}

