package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * App - Main Class untuk Sistem Eksplorasi Wisata
 */
public class App extends Application {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    private static Scene scene;
    private static String cssUrl;

    @Override
    public void start(Stage stage) throws IOException {
        LOGGER.log(Level.INFO, "Starting ExploreNusa Application");

        System.out.println("═══════════════════════════════════════════");
        System.out.println("  EXPLORENUSA");
        System.out.println("═══════════════════════════════════════════");
        System.out.println("Checking database connection...");

        DatabaseHelper dbHelper = DatabaseHelper.getInstance();
        if (!dbHelper.testConnection()) {
            System.out.println("⚠️  WARNING: Database connection failed!");
            System.out.println("Aplikasi akan menggunakan data cadangan.");
        }
        System.out.println();

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("home.fxml"));
        Parent root = fxmlLoader.load();

        scene = new Scene(root, 1400, 900);
        cssUrl = getClass().getResource("style.css").toExternalForm();
        scene.getStylesheets().add(cssUrl);

        stage.setTitle("ExploreNusa - Kelompok B6");
        stage.setScene(scene);
        stage.setWidth(1400);
        stage.setHeight(900);
        stage.setResizable(true);
        stage.setOnCloseRequest(e -> {
            LOGGER.log(Level.INFO, "Application closed");
            System.exit(0);
        });

        stage.show();
        LOGGER.log(Level.INFO, "Application started successfully");
    }

    static void setRoot(String fxml) throws IOException {
        setRoot(fxml, null);
    }

    static <T> T setRoot(String fxml, Consumer<T> controllerSetup) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        Parent root = loader.load();
        scene.setRoot(root);
        ensureStylesheet();
        if (controllerSetup != null) {
            controllerSetup.accept(loader.getController());
        }
        return loader.getController();
    }

    private static void ensureStylesheet() {
        if (cssUrl != null && !scene.getStylesheets().contains(cssUrl)) {
            scene.getStylesheets().add(cssUrl);
        }
    }

    public static Scene getScene() {
        return scene;
    }

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════");
        System.out.println("Launching ExploreNusa...");
        System.out.println("═══════════════════════════════════════════");
        launch();
    }
}
