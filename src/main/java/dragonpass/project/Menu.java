package dragonpass.project;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import model.Logger;
import monitor.FileObserver;
import monitor.Observer;
import monitor.SQLiteObserver;
import monitor.Subject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Menu {

    Subject monitor = new Subject();
    Observer observe1 = new SQLiteObserver(monitor);
    Observer observe2 = new FileObserver(monitor);

    @FXML
    private Button exitB;
    @FXML
    private Button LOBtn;

    Connection conn = null;
    PreparedStatement stmt = null;



    public void LOBtnAction(ActionEvent event) throws IOException, SQLException {

        DatabaseConnection connect = new DatabaseConnection();
        conn = connect.getVerification();


        String query = "UPDATE user_account\n" +
                "SET session = 0;";

        stmt = conn.prepareStatement(query);

        stmt.executeUpdate();

        monitor.setLog(new Logger("info", "Session Closed"));

        Stage stage = (Stage) LOBtn.getScene().getWindow();
        stage.close();


        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 500);
        stage.setTitle("Dragon Pass");
        stage.setScene(scene);
        stage.show();
        monitor.setLog(new Logger("info", "LogOut successful"));
        stmt.close();
        conn.close();

    }
    //Exit button action
    public void exitBAction (ActionEvent event) throws SQLException {

        DatabaseConnection connect = new DatabaseConnection();
        conn = connect.getVerification();

        String query = "UPDATE user_account\n" +
                "SET session = 0;";

        stmt = conn.prepareStatement(query);

        stmt.executeUpdate();

        monitor.setLog(new Logger("info", "Session Closed"));

        Stage stage = (Stage) exitB.getScene().getWindow();
        stage.close();
        monitor.setLog(new Logger("info", "Exit application"));
        stmt.close();
        conn.close();
    }
}
