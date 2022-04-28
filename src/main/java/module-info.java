module dragonpass.project {

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.sql;
    requires mysql.connector.java;
    requires java.desktop;
    requires commons.codec;
    requires totp;


    opens dragonpass.project to javafx.fxml;
    exports dragonpass.project;
}