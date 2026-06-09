package com.example;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LoginController - Menghandle proses login pengguna
 * 
 * Fitur:
 * - Validasi username dan password dari database
 * - Pengecekan role pengguna (Admin, Pengelola, Wisatawan)
 * - Error handling untuk koneksi database
 * - Session management (menyimpan user yang login)
 * 
 * @author Sistem Eksplorasi Wisata B6
 */
public class LoginController {

    // ============================================================
    // FXML COMPONENTS
    // ============================================================

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Button loginButton;
    @FXML
    private Button exitButton;
    @FXML
    private CheckBox rememberCheckBox;
    @FXML
    private Hyperlink forgotPasswordLink;

    // ============================================================
    // LOGGER
    // ============================================================

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    // ============================================================
    // SESSION - Menyimpan user yang sedang login
    // ============================================================

    private static User currentUser = null;

    /**
     * Get user yang sedang login
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Set user yang sedang login
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    // ============================================================
    // INITIALIZE
    // ============================================================

    /**
     * Initialize controller - dipanggil otomatis saat FXML di-load
     */
    @FXML
    public void initialize() {
        // Tambahkan opsi role ke ComboBox
        roleComboBox.getItems().addAll("Admin", "Pengelola", "Wisatawan");
        roleComboBox.setValue("Wisatawan");

        // Set event listener untuk Enter key
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
    }

    // ============================================================
    // LOGIN HANDLER
    // ============================================================

    /**
     * Handle tombol Login
     * 
     * ERROR HANDLING:
     * - Validasi input kosong
     * - Try-catch untuk database connection
     * - Pengecekan kredensial dari database
     * - Helpful error messages
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        // ========== VALIDASI INPUT ==========
        // ERROR HANDLING: Cek input kosong
        if (username.isEmpty() || password.isEmpty()) {
            showError("Validasi Gagal",
                    "Username dan password tidak boleh kosong!");
            LOGGER.log(Level.WARNING, "Login attempt dengan input kosong");
            return;
        }

        if (username.length() < 3) {
            showError("Validasi Gagal",
                    "Username minimal 3 karakter");
            return;
        }

        if (password.length() < 1) {
            showError("Validasi Gagal",
                    "Password tidak boleh kosong");
            return;
        }

        // ========== AUTHENTICATE ==========
        User authenticatedUser = authenticateUser(username, password, role);

        if (authenticatedUser != null) {
            // Login berhasil
            LOGGER.log(Level.INFO, "User berhasil login: " + username);
            setCurrentUser(authenticatedUser);
            showInfo("Berhasil",
                    "Selamat datang " + authenticatedUser.getNamaLengkap() + "!");
            // Jika role Wisatawan: kembali ke tampilan awal (home)
            try {
                if (authenticatedUser.getRole() != null && authenticatedUser.getRole().equalsIgnoreCase("Wisatawan")) {
                    App.setRoot("home");
                } else {
                    // Pindah ke halaman dashboard untuk role lain
                    openDashboard(authenticatedUser);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error membuka halaman setelah login", e);
                showError("Error", "Gagal membuka halaman setelah login: " + e.getMessage());
            }
        } else {
            // Login gagal
            LOGGER.log(Level.WARNING, "Percobaan login gagal untuk user: " + username);
            statusLabel.setText("❌ Login gagal - username atau password salah");
            statusLabel.setStyle("-fx-text-fill: #FF6B6B;");
            showError("Login Gagal", "Username atau password salah.");
        }
    }

    // ============================================================
    // AUTHENTICATE USER FROM DATABASE
    // ============================================================

    /**
     * Authenticate user dari database
     * 
     * ERROR HANDLING:
     * - Try-catch untuk SQLException
     * - Pengecekan koneksi database
     * - Validasi kredensial
     * - Resource cleanup (close ResultSet, Statement, Connection)
     * 
     * @param username Username yang diinput
     * @param password Password yang diinput
     * @param role     Role yang dipilih
     * @return User object jika berhasil, null jika gagal
     */
    private User authenticateUser(String username, String password, String role) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // ========== KONEKSI DATABASE ==========
            // ERROR HANDLING: Cek koneksi database
            conn = DatabaseHelper.getInstance().getConnection();
            if (conn == null) {
                showError("Database Error",
                        "Gagal terhubung ke database.\n" +
                                "Pastikan PostgreSQL sudah berjalan dan database 'wisata_db' sudah dibuat.");
                LOGGER.log(Level.SEVERE, "Database connection failed during login");
                return null;
            }

            // ========== QUERY DATABASE ==========
            // SQL query untuk mencari user berdasarkan username saja.
            // Role dari database akan dipakai sebagai data sesi setelah password cocok.
            String sql = "SELECT id, username, password, role, nama_lengkap, email, no_telepon " +
                    "FROM users WHERE username = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);

            // ERROR HANDLING: Execute query
            rs = pstmt.executeQuery();

            // ========== VALIDASI RESULT ==========
            if (rs.next()) {
                // User ditemukan, validasi password
                String storedPassword = rs.getString("password");

                // Bandingkan password: dukung hashed (MD5) dan legacy plain-text
                String hashedInput = DatabaseHelper.hashPasswordMD5(password);
                if (hashedInput.equals(storedPassword) || password.equals(storedPassword)) {
                    // PASSWORD COCOK - LOGIN BERHASIL
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("nama_lengkap"),
                            rs.getString("email"),
                            rs.getString("no_telepon"));

                    LOGGER.log(Level.INFO, "User authenticated successfully: " + username);
                    return user;
                } else {
                    // PASSWORD SALAH
                    LOGGER.log(Level.WARNING, "Wrong password for user: " + username);
                    return null;
                }
            } else {
                // USER TIDAK DITEMUKAN
                LOGGER.log(Level.WARNING, "User not found: " + username + " with role: " + role);
                return null;
            }

        } catch (Exception e) {
            // ERROR HANDLING: Exception dari database
            LOGGER.log(Level.SEVERE, "Error during authentication", e);
            System.out.println("==== AUTHENTICATION ERROR ====");
            System.out.println("Message: " + e.getMessage());
            System.out.println("Type: " + e.getClass().getSimpleName());
            System.out.println("==============================");

            showError("Error", "Terjadi kesalahan saat proses login.\n" + e.getMessage());
            return null;

        } finally {
            // ERROR HANDLING: Resource cleanup
            // Tutup semua resource dengan aman
            try {
                if (rs != null)
                    rs.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing ResultSet", e);
            }

            try {
                if (pstmt != null)
                    pstmt.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e);
            }

            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing Connection", e);
            }
        }
    }

    // ============================================================
    // PASSWORD HASHING
    // ============================================================

    /**
     * Hash password menggunakan MD5 (untuk sample - gunakan bcrypt di production)
     * 
     * NOTE: MD5 tidak aman untuk production!
     * Gunakan bcrypt, Argon2, atau scrypt untuk aplikasi real.
     * 
     * @param password Password yang akan di-hash
     * @return Hashed password
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error hashing password", e);
            return "";
        }
    }

    // ============================================================
    // NAVIGATION
    // ============================================================

    /**
     * Buka halaman dashboard
     * 
     * ERROR HANDLING:
     * - Try-catch untuk IOException saat load FXML
     * 
     * @param user User yang login
     * @throws IOException jika gagal load FXML
     */
    private void openDashboard(User user) throws IOException {
        App.setRoot("dashboard", (DashboardController controller) -> {
            controller.setCurrentUser(user);
            controller.loadDestinations();
        });
    }

    /**
     * Handle tombol Exit
     */
    @FXML
    private void handleExit() {
        Stage stage = (Stage) exitButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Buka halaman sign up.
     */
    @FXML
    private void handleSignUp() {
        try {
            App.setRoot("signup");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Gagal membuka halaman sign up", e);
            showError("Error", "Gagal membuka halaman sign up: " + e.getMessage());
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Tampilkan error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Tampilkan info dialog
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
