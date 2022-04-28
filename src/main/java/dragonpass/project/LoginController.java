package dragonpass.project;

import model.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import monitor.FileObserver;
import monitor.Observer;
import monitor.SQLiteObserver;
import monitor.Subject;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import static dragonpass.project.RegisterController.getSHA;
import static dragonpass.project.RegisterController.toHexString;

public class LoginController {

    String username = "";

    Subject monitor = new Subject();
    Observer observe1 = new SQLiteObserver(monitor);
    Observer observe2 = new FileObserver(monitor);

    @FXML
    private TextField usernameTF;

    @FXML
    private TextField passTF;

    @FXML
    private PasswordField pinTF;

    @FXML
    private TextField tfaTF;

    @FXML
    private Label invalidLabel;

    @FXML
    private Button exitB;

    @FXML
    private Button loginBtn;


    //Login button action
    public void loginBtnAction(ActionEvent event) throws SQLException, NoSuchAlgorithmException, IOException {
        Connection conn = null;

        DatabaseConnection connect = new DatabaseConnection();
        conn = connect.getVerification();


        String query = "UPDATE user_account SET session = 0 WHERE session = 1";
        PreparedStatement stmt = conn.prepareStatement(query);

        stmt.executeUpdate();

        //If/else statements to prevent user from putting in wrong or empty details
        if(usernameTF.getText().isBlank() && passTF.getText().isBlank() && pinTF.getText().isBlank()){

            invalidLabel.setText("Please enter your account details!");

        }

        //Username field blank
        else if(usernameTF.getText().isBlank()){

            invalidLabel.setText("Please enter your username!");

        }

        //Password field blank
        else if(passTF.getText().isBlank()){

            invalidLabel.setText("Please enter your password!");

        }

        //Pin field blank
        else if(pinTF.getText().isBlank()){

            invalidLabel.setText("Please enter your pin!");

        }

        //2FA field blank
        else if(tfaTF.getText().isBlank()){

            invalidLabel.setText("Please enter your TFA key!");

        }
        else{

            //If all text fields are filled, proceed to log in verification

            /* Note: Unlike registration, there is no need for further parameters in this process as the use of
            prepared statements will only accept credentials that match existing users from the database in the verification process */

            loginVerification();

        }


    }


    //Obtaining a TOTP code using the Time based one time password algorithm
    public static String getTOTPCode(String secretKey) {

        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);

        String hexKey = Hex.encodeHexString(bytes);
        return de.taimos.totp.TOTP.getOTP(hexKey);

    }


    //Validates submission and prevents SQL Injection
    public void loginVerification() throws NoSuchAlgorithmException, SQLException, IOException {

        //Variables
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs;
        String secretKey = "";

        //setting user input to username
        String username = usernameTF.getText();

        //setting user input to password
        String password = "";
        password = toHexString(getSHA(passTF.getText()));

        //setting user input to pin
        String pin = "";
        pin=toHexString(getSHA(pinTF.getText()));

        //setting user input to 2FA key
        String key = "";
        key=tfaTF.getText();

        try{

            monitor.setLog(new Logger("info", "Verification Requested"));

            //Creating a prepared statement to search and verify existence of user based on user input
            String query = "select * from user_account where username = ? and password = ? and pin = ?";

            //connecting to database
            DatabaseConnection connect = new DatabaseConnection();
            conn = connect.getVerification();
            stmt = conn.prepareStatement(query);

            //Parameters for query
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, pin);


            //Executing prepared statement
            rs = stmt.executeQuery();

            //If statement was successful continue
            if(rs.next()){

                //Obtaining users unique secret key for two-factor authentication
                secretKey = rs.getString("tfaKey");


                int i =0;
                do{

                    //Setting users unique key to a String variable using the TOTP algorithm.
                    //This code will change every 30 seconds when the user tries to log in
                    String code = getTOTPCode(secretKey);

                    /* If the code from the user input matches the TOTP code variable then proceed. Based on the registration process,
                     the user should have the same code being generated on their phone via Google Authenticator */
                    if(Objects.equals(code, key)){

                        //Setting user session to active via prepared statement
                        String query2 = "UPDATE user_account\n" +
                                "SET session = 1 WHERE username = ?;";

                        stmt = conn.prepareStatement(query2);

                        stmt.setString(1, username);

                        //Execute update of query
                        stmt.executeUpdate();

                        monitor.setLog(new Logger("info", "Session Opened"));

                        monitor.setLog(new Logger("info", "Account verified"));


                        try {

                            //Close stage
                            Stage stage = (Stage) exitB.getScene().getWindow();
                            stage.close();

                            //Loads FXML for menu
                            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("menu.fxml"));
                            Scene scene = new Scene(fxmlLoader.load(), 800, 500);
                            stage.setTitle("Dragon Pass");
                            stage.setScene(scene);
                            stage.show();
                            monitor.setLog(new Logger("info", "Menu accessed"));

                        } catch (Exception e) {

                            monitor.setLog(new Logger("warn", e.getMessage()));

                        }
                    }

                    //If 2FA details were incorrect
                    else{

                        invalidLabel.setText("Account not found under these details!");

                        monitor.setLog(new Logger("info", "Invalid account details"));

                    }

                    i++;
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {

                    }


                }
                while(i<1);

            }

            //If any details except 2FA were incorrect
            else{

                invalidLabel.setText("Account not found under these details!");

                monitor.setLog(new Logger("info", "Invalid account details"));

            }

            rs.close();
        }
        catch(Exception e){

            monitor.setLog(new Logger("warn", e.getMessage()));

        }

        //Close connection to database
        finally{
            try{

                stmt.close();
                conn.close();
            }
            catch(Exception e){
                monitor.setLog(new Logger("warn", e.getMessage()));
            }
        }

    }

        //Register button action
        public void submitBtnAction(ActionEvent event){

            Stage stage = (Stage) exitB.getScene().getWindow();
            stage.close();

            try {

                //Loads FXML for Registration tab
                FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("register.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 897, 651);
                stage.setTitle("Dragon Pass");
                stage.setScene(scene);
                stage.show();
                monitor.setLog(new Logger("info", "Register controller accessed"));

            } catch (Exception e) {

                monitor.setLog(new Logger("warn", e.getMessage()));

            }

        }


        //Exit button action
        public void exitBAction (ActionEvent event){
            Stage stage = (Stage) exitB.getScene().getWindow();
            stage.close();
            monitor.setLog(new Logger("info", "Exit application"));
        }

    }


