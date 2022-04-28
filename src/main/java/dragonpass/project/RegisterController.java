package dragonpass.project;
import javafx.scene.control.*;
import model.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import monitor.FileObserver;
import monitor.Observer;
import monitor.SQLiteObserver;
import monitor.Subject;
import org.apache.commons.codec.binary.Base32;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


import java.security.SecureRandom;
import java.sql.*;


public class RegisterController {

    Subject monitor = new Subject();
    Observer observe1 = new SQLiteObserver(monitor);
    Observer observe2 = new FileObserver(monitor);

    @FXML
    private Button closeBtn;

    @FXML
    private Button twoFacBtn;

    @FXML
    private PasswordField passTF;

    @FXML
    private PasswordField confirmPassTF;

    @FXML
    private PasswordField pinTF;

    @FXML
    private PasswordField confirmPinTF;

    @FXML
    private Label pinMatch;

    @FXML
    private TextField usernameTF;

    @FXML
    private TextField emailTF;

    @FXML
    private Label usernameError;

    @FXML
    private Label emailError;

    @FXML
    private Label passwordError;

    @FXML
    private Label tfaError;

    @FXML
    private TextArea secretKey;


    public void closeBtnAction(ActionEvent event) {

        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();

        try {

            FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 500);
            stage.setTitle("Dragon Pass");
            stage.setScene(scene);
            stage.show();
            monitor.setLog(new Logger("info", "login controller accessed"));
        } catch (Exception e) {

            monitor.setLog(new Logger("warn", e.getMessage()));

        }

    }

    //Email parameters verification
    public static boolean emailV(String email) {
        String regex = "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+.+$";
        return email.matches(regex);
    }

    //Submission Button
        public void registerBtnAction(ActionEvent event) throws SQLException, NoSuchAlgorithmException {
            String userEmail = emailTF.getText();
            Boolean validUsername = null;
            Boolean validEmail = null;
            Boolean validPass = null;
            Boolean validPin = null;
            boolean emailV = RegisterController.emailV(userEmail);
            Boolean validKey = null;

            //Creates database on button click if not already existing
            DatabaseConnection connect = new DatabaseConnection();
            Connection conn = connect.getConnection();

            conn.close();

            //username errors
            if(usernameTF.getText().isBlank()){

                usernameError.setText("Please enter your username!");

            }
            else if(usernameTF.getText().length() > 15){

                usernameError.setText("Username must be less than 15 characters!");

            }
            else{

                usernameError.setText("✔");
                validUsername = true;

            }

            //Email errors
            if(emailTF.getText().isBlank()){

                emailError.setText("Please enter your email!");

            }
            else if(emailTF.getText().length() > 18){

                emailError.setText("Email must be 18 characters or less!");
            }
            else if(!emailV){
                emailError.setText("invalid email!");
            }
            else{

                emailError.setText("✔");
                validEmail = true;

            }

            //Password errors
            if(passTF.getText().isBlank() || confirmPassTF.getText().isBlank()){

                passwordError.setText("Please enter your password in both fields!");

            }
            else if(!(passTF.getText().length() <= 20)) {

                passwordError.setText("Password must be 20 characters or less");

            }
            else if(!passTF.getText().equals(confirmPassTF.getText())){

                passwordError.setText("Passwords do not match!");

            }
            else{

                passwordError.setText("✔");
                validPass = true;

            }


            //Pin errors
            //Pin match

            if(pinTF.getText().isBlank() || confirmPinTF.getText().isBlank()){

                pinMatch.setText("Please enter your pin in both fields!");

            }
            else if(!(pinTF.getText().length() == 4)){

                pinMatch.setText("Pin must be 4 digits");

            }
           else if(pinTF.getText().equals(confirmPinTF.getText()) && !pinTF.getText().isBlank()) {

                String input = pinTF.getText();
                boolean flag = true;
                for (int a = 0; a < input.length(); a++) {
                    if (a == 0 && input.charAt(a) == '-')
                        continue;
                    if (!Character.isDigit(input.charAt(a)))
                        flag = false;
                    pinMatch.setText("Pin must contain 4 digits!");
                }
                if (flag) {
                    pinMatch.setText("✔");
                    validPin = true;
                }
            }

            else{

                pinMatch.setText("pins do not match!");

            }

            if(secretKey.getText().isBlank()){

                validKey = false;

                tfaError.setText("Please generate a 2FA key and follow the below steps!");

            }
            else{

                validKey = true;
                tfaError.setText("✔");

            }

            if(Boolean.TRUE.equals(validUsername)
            && Boolean.TRUE.equals(validEmail)
            && Boolean.TRUE.equals(validPass)
            && Boolean.TRUE.equals(validPin)
            && Boolean.TRUE.equals(validKey)){
                register();
            }

            else{

                monitor.setLog(new Logger("info", "Submission details invalid"));

            }



        }

    //SHA-256 hashing
    public static byte[] getSHA(String input) throws NoSuchAlgorithmException{

        MessageDigest md = MessageDigest.getInstance("SHA-256");

        return md.digest(input.getBytes(StandardCharsets.UTF_8));

    }

    public static String toHexString(byte[] hash){

        BigInteger number = new BigInteger(1, hash);

        StringBuilder hexString = new StringBuilder(number.toString(16));

        while(hexString.length() < 32){

            hexString.insert(0, '0');

        }

        return hexString.toString();

    }

    public static String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];

        random.nextBytes(bytes);

        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);

    }

    public void genBtnAction(ActionEvent event){

        secretKey.setText(generateSecretKey());

    }




        //Prepared statement on submission
    private void register() throws SQLException, NoSuchAlgorithmException {
        Connection conn;

        String username = usernameTF.getText();
        String email = emailTF.getText();
        String password = toHexString(getSHA(passTF.getText()));
        String pin = toHexString(getSHA(pinTF.getText()));
        String key = secretKey.getText();


        String query = "INSERT INTO user_account(username, email, password, pin, tfaKey, session) VALUES(?,?,?,?,?,0)";

        DatabaseConnection connect = new DatabaseConnection();
        conn = connect.getVerification();
        PreparedStatement stmt = conn.prepareStatement(query);

        stmt.setString(1, username);
        stmt.setString(2, email);
        stmt.setString(3, password);
        stmt.setString(4, pin);
        stmt.setString(5, key);


        try{
            stmt.execute();
            monitor.setLog(new Logger("info", "New account added"));
            Stage stage = (Stage) closeBtn.getScene().getWindow();
            stage.close();


                FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login.fxml"));
                Scene scene = new Scene(fxmlLoader.load(), 800, 500);
                stage.setTitle("Dragon Pass");
                stage.setScene(scene);
                stage.show();

            } catch (Exception e) {

                monitor.setLog(new Logger("warn", e.getMessage()));

            }

        finally{
            //Close connection
            try{
                conn.close();
            }
            catch(Exception e){
                monitor.setLog(new Logger("warn", e.getMessage()));
            }
        }




        }




}



