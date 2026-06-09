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
            try {
                System.out.println("Testing bookings.fxml...");
                FXMLLoader loader = new FXMLLoader(TestFXML.class.getResource("bookings.fxml"));
                Parent root = loader.load();
                System.out.println("Success bookings.fxml!");
            } catch (Exception e) {
                System.out.println("Error bookings.fxml: ");
                e.printStackTrace();
            }

            try {
                System.out.println("Testing wishlist.fxml...");
                FXMLLoader loader = new FXMLLoader(TestFXML.class.getResource("wishlist.fxml"));
                Parent root = loader.load();
                System.out.println("Success wishlist.fxml!");
            } catch (Exception e) {
                System.out.println("Error wishlist.fxml: ");
                e.printStackTrace();
            }

            try {
                System.out.println("Testing signup.fxml...");
                FXMLLoader loader = new FXMLLoader(TestFXML.class.getResource("signup.fxml"));
                Parent root = loader.load();
                System.out.println("Success signup.fxml!");
            } catch (Exception e) {
                System.out.println("Error signup.fxml: ");
                e.printStackTrace();
            }
            
            Platform.exit();
        });
    }
}
