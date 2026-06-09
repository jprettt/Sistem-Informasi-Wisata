package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SignupController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private TextField namaLengkapField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField teleponField;
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Button registerButton;
    @FXML
    private Button backButton;

    private static final Logger LOGGER = Logger.getLogger(SignupController.class.getName());

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("Wisatawan", "Pengelola");
        roleComboBox.setValue("Wisatawan");
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String namaLengkap = namaLengkapField.getText().trim();
        String email = emailField.getText().trim();
        String telepon = teleponField.getText().trim();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
                || namaLengkap.isEmpty() || email.isEmpty() || telepon.isEmpty()) {
            showError("Validasi Gagal", "Semua field harus diisi.");
            return;
        }

        if (username.length() < 3) {
            showError("Validasi Gagal", "Username minimal 3 karakter.");
            return;
        }

        if (password.length() < 4) {
            showError("Validasi Gagal", "Password minimal 4 karakter.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Validasi Gagal", "Password dan konfirmasi password tidak sama.");
            return;
        }

        if (usernameExists(username)) {
            showError("Validasi Gagal", "Username sudah digunakan. Pilih username lain.");
            return;
        }

        String sql = "INSERT INTO users (username, password, role, nama_lengkap, email, no_telepon) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                showError("Database Error", "Gagal terhubung ke database.");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, DatabaseHelper.hashPasswordMD5(password));
                ps.setString(3, role);
                ps.setString(4, namaLengkap);
                ps.setString(5, email);
                ps.setString(6, telepon);

                int inserted = ps.executeUpdate();
                if (inserted > 0) {
                    statusLabel.setText("✅ Akun berhasil dibuat. Silakan login.");
                    statusLabel.setStyle("-fx-text-fill: #567C8D;");
                    showInfo("Berhasil", "Akun berhasil dibuat. Silakan login.");
                    App.setRoot("login");
                } else {
                    showError("Gagal", "Akun tidak berhasil dibuat.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saat registrasi", e);
            showError("Error", "Terjadi kesalahan saat registrasi: " + e.getMessage());
        }
    }

    private boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Gagal memeriksa username", e);
            return false;
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            App.setRoot("login");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Gagal kembali ke login", e);
            showError("Error", "Gagal kembali ke halaman login: " + e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
