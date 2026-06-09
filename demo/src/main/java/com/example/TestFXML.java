package com.example;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class TestFXML {
    public static void main(String[] args) {
        Platform.startup(() -> {
            try {
                System.out.println("Testing detail_destinasi.fxml...");
                FXMLLoader loader = new FXMLLoader(TestFXML.class.getResource("detail_destinasi.fxml"));
                Parent root = loader.load();
                System.out.println("Success detail_destinasi.fxml!");
            } catch (Exception e) {
                System.out.println("Error detail_destinasi.fxml: ");
                e.printStackTrace();
            }

            try {
                System.out.println("Testing dashboard.fxml...");
                FXMLLoader loader = new FXMLLoader(TestFXML.class.getResource("dashboard.fxml"));
                Parent root = loader.load();
                System.out.println("Success dashboard.fxml!");
            } catch (Exception e) {
                System.out.println("Error dashboard.fxml: ");
                e.printStackTrace();
            }
            
            Platform.exit();
        });
    }
}
