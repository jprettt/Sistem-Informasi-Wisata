package com.example;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.MessageDigest;

/**
 * DatabaseHelper - Kelas untuk mengelola koneksi database PostgreSQL
 * 
 * Fitur:
 * - Koneksi JDBC ke PostgreSQL
 * - Error handling yang kuat dengan try-catch
 * - Logging untuk debugging
 * - Connection pooling (opsional)
 * 
 * @author Sistem Eksplorasi Wisata B6
 * @version 1.0
 */
public class DatabaseHelper {

    // ============================================================
    // KONFIGURASI DATABASE
    // ============================================================

    public static String hashPasswordMD5(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(password.getBytes());

            StringBuilder sb = new StringBuilder();

            for (byte b : messageDigest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/wisata_db";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "vghky2155";
    private static final String DB_DRIVER = "org.postgresql.Driver";

    // ============================================================
    // LOGGER
    // ============================================================
    private static final Logger LOGGER = Logger.getLogger(DatabaseHelper.class.getName());

    // ============================================================
    // SINGLETON INSTANCE
    // ============================================================
    private static DatabaseHelper instance = null;

    /**
     * Private constructor untuk singleton pattern
     */
    private DatabaseHelper() {
        initializeDriver();
    }

    /**
     * Get instance dari DatabaseHelper (Singleton)
     * 
     * @return DatabaseHelper instance
     */
    public static synchronized DatabaseHelper getInstance() {
        if (instance == null) {
            instance = new DatabaseHelper();
        }
        return instance;
    }

    // ============================================================
    // INISIALISASI DRIVER
    // ============================================================

    /**
     * Inisialisasi PostgreSQL JDBC driver
     * ERROR HANDLING: Try-catch untuk menangkap ClassNotFoundException
     */
    private void initializeDriver() {
        try {
            Class.forName(DB_DRIVER);
            LOGGER.log(Level.INFO, "PostgreSQL Driver berhasil dimuat");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "ERROR: PostgreSQL Driver tidak ditemukan!", e);
            System.out.println("FATAL ERROR: Driver PostgreSQL tidak terinstall!");
            System.out.println("Message: " + e.getMessage());
        }
    }

    // ============================================================
    // KONEKSI DATABASE
    // ============================================================

    /**
     * Mendapatkan koneksi baru ke database
     * 
     * ERROR HANDLING:
     * - SQLException untuk error koneksi database
     * - Menampilkan pesan error yang detail
     * 
     * @return Connection object jika berhasil, null jika gagal
     */
    public Connection getConnection() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            LOGGER.log(Level.INFO, "Koneksi database berhasil");
            return connection;
        } catch (SQLException e) {
            // ERROR HANDLING: Menangani SQL Exception
            LOGGER.log(Level.SEVERE, "ERROR: Gagal terhubung ke database", e);

            // Menampilkan pesan error yang user-friendly
            System.out.println("====== DATABASE CONNECTION ERROR ======");
            System.out.println("Error Code: " + e.getErrorCode());
            System.out.println("SQL State: " + e.getSQLState());
            System.out.println("Message: " + e.getMessage());
            System.out.println("========================================");

            // Cek tipe error untuk debugging
            if (e.getMessage().contains("Connection refused")) {
                System.out.println("SOLUSI: PostgreSQL server tidak berjalan atau tidak dapat diakses");
                System.out.println("Pastikan PostgreSQL sudah dijalankan dan berjalan di port 5432");
            } else if (e.getMessage().contains("authentication failed")) {
                System.out.println("SOLUSI: Username atau password salah");
                System.out.println("Periksa konfigurasi DB_USER dan DB_PASSWORD di DatabaseHelper.java");
            } else if (e.getMessage().contains("database") && e.getMessage().contains("does not exist")) {
                System.out.println("SOLUSI: Database 'wisata_db' belum dibuat");
                System.out.println("Buat database terlebih dahulu dengan menjalankan schema.sql");
            }

            return null;
        }
    }

    /**
     * Membuat tabel yang dibutuhkan aplikasi jika belum ada.
     */
    private void ensureRequiredTables(Connection conn) {
        // 1. TENTUKAN KOLOM STATUS PADA TABEL USERS
        try {
            String checkSql = "SELECT column_name FROM information_schema.columns WHERE table_name='users' AND column_name='status'";
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql); ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    String alterSql = "ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'Active'";
                    try (PreparedStatement alterStmt = conn.prepareStatement(alterSql)) {
                        alterStmt.execute();
                        LOGGER.log(Level.INFO, "Kolom status berhasil ditambahkan ke tabel users");
                    }
                    // Tambahkan contoh user pengelola baru yang statusnya pending untuk verifikasi admin
                    String insertPending = "INSERT INTO users (username, password, role, nama_lengkap, email, no_telepon, status) " +
                            "VALUES ('pengelola_baru', 'admin', 'Pengelola', 'Vendor Kopi Kitab', 'vendor_kopi@mail.com', '08987654321', 'Pending') " +
                            "ON CONFLICT DO NOTHING";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertPending)) {
                        insertStmt.execute();
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Gagal memverifikasi kolom status pada tabel users: " + e.getMessage());
        }

        // 2. TABEL PESANAN
        String sql = "CREATE TABLE IF NOT EXISTS pesanan (" +
                "id SERIAL PRIMARY KEY, " +
                "user_id INT REFERENCES users(id) ON DELETE SET NULL, " +
                "destinasi_id INT NOT NULL REFERENCES destinasi(id) ON DELETE CASCADE, " +
                "tanggal_kunjungan DATE NOT NULL, " +
                "tipe_tiket VARCHAR(50) NOT NULL, " +
                "jumlah_dewasa INT NOT NULL DEFAULT 1, " +
                "jumlah_anak INT NOT NULL DEFAULT 0, " +
                "nama_pemesan VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL, " +
                "telepon VARCHAR(15) NOT NULL, " +
                "catatan TEXT, " +
                "total_harga INT NOT NULL, " +
                "metode_pembayaran VARCHAR(50) NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'Pending', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
            LOGGER.log(Level.INFO, "Tabel pesanan siap digunakan");
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Gagal membuat tabel pesanan: " + e.getMessage());
        }

        // 3. TABEL ULASAN
        String sqlUlasan = "CREATE TABLE IF NOT EXISTS ulasan (" +
                "id SERIAL PRIMARY KEY, " +
                "user_id INT REFERENCES users(id) ON DELETE CASCADE, " +
                "destinasi_id INT REFERENCES destinasi(id) ON DELETE CASCADE, " +
                "rating INT CHECK (rating >= 1 AND rating <= 5), " +
                "komentar TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlUlasan)) {
            pstmt.execute();
            LOGGER.log(Level.INFO, "Tabel ulasan siap digunakan");

            // Insert sample ulasan jika kosong
            String checkEmpty = "SELECT COUNT(*) FROM ulasan";
            try (PreparedStatement ps = conn.prepareStatement(checkEmpty); ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSampleUlasan = "INSERT INTO ulasan (user_id, destinasi_id, rating, komentar) VALUES " +
                            "(3, 1, 5, 'Luar biasa sekali Candi Borobudur ini, sangat megah dan rapi!'), " +
                            "(4, 2, 4, 'Pantainya sangat indah, sayang sedikit ramai pengunjung.'), " +
                            "(3, 3, 5, 'Pemandangan Bromo pagi hari sangat menakjubkan! Sangat disarankan.'), " +
                            "(4, 1, 2, 'Spam ulasan! Klik link ini untuk mendapatkan hadiah gratis www.spamlink.com');"; // ulasan spam untuk dicoba dihapus oleh admin
                    try (PreparedStatement psInsert = conn.prepareStatement(insertSampleUlasan)) {
                        psInsert.execute();
                        LOGGER.log(Level.INFO, "Sample ulasan berhasil di-insert");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Gagal membuat tabel ulasan: " + e.getMessage());
        }

        // 4. TABEL AUDIT LOG
        String sqlAuditLog = "CREATE TABLE IF NOT EXISTS audit_log (" +
                "id SERIAL PRIMARY KEY, " +
                "user_id INT REFERENCES users(id) ON DELETE SET NULL, " +
                "username VARCHAR(50), " +
                "aksi VARCHAR(255) NOT NULL, " +
                "details TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (PreparedStatement pstmt = conn.prepareStatement(sqlAuditLog)) {
            pstmt.execute();
            LOGGER.log(Level.INFO, "Tabel audit_log siap digunakan");

            // Insert sample log jika kosong
            String checkEmpty = "SELECT COUNT(*) FROM audit_log";
            try (PreparedStatement ps = conn.prepareStatement(checkEmpty); ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSampleLog = "INSERT INTO audit_log (username, aksi, details) VALUES " +
                            "('admin', 'LOGIN', 'Admin masuk ke dashboard utama'), " +
                            "('pengelola01', 'UPDATE_PRICE', 'Mengubah harga tiket Candi Borobudur menjadi Rp 750.000'), " +
                            "('wisatawan01', 'CREATE_BOOKING', 'Melakukan pemesanan tiket Raja Ampat dengan ID Pesanan #12');";
                    try (PreparedStatement psInsert = conn.prepareStatement(insertSampleLog)) {
                        psInsert.execute();
                        LOGGER.log(Level.INFO, "Sample audit log berhasil di-insert");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Gagal membuat tabel audit_log: " + e.getMessage());
        }
    }

    // ============================================================
    // UTILITY METHODS
    // ============================================================

    /**
     * Test koneksi ke database
     * Berguna untuk memvalidasi konfigurasi database saat startup
     * 
     * @return true jika koneksi berhasil, false jika gagal
     */
    public boolean testConnection() {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn != null) {
                ensureRequiredTables(conn);
                System.out.println("✓ Koneksi database berhasil!");
                return true;
            } else {
                System.out.println("✗ Koneksi database gagal!");
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Test koneksi mengalami error", e);
            return false;
        } finally {
            // ERROR HANDLING: Close connection jika ada
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error menutup koneksi", e);
                }
            }
        }
    }

    /**
     * Tutup koneksi database dengan aman
     * 
     * ERROR HANDLING: Try-catch untuk SQLException saat menutup koneksi
     * 
     * @param connection Connection object yang akan ditutup
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                LOGGER.log(Level.INFO, "Koneksi database berhasil ditutup");
            } catch (SQLException e) {
                // ERROR HANDLING: Log warning jika error saat close
                LOGGER.log(Level.WARNING, "ERROR: Gagal menutup koneksi database", e);
                System.out.println("WARNING: Gagal menutup koneksi - " + e.getMessage());
            }
        }
    }

    /**
     * Tutup ResultSet dengan aman
     * 
     * ERROR HANDLING: Try-catch untuk SQLException
     * 
     * @param resultSet ResultSet object yang akan ditutup
     */
    public static void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "ERROR: Gagal menutup ResultSet", e);
            }
        }
    }

    /**
     * Tutup PreparedStatement dengan aman
     * 
     * ERROR HANDLING: Try-catch untuk SQLException
     * 
     * @param statement PreparedStatement object yang akan ditutup
     */
    public static void closeStatement(PreparedStatement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "ERROR: Gagal menutup PreparedStatement", e);
            }
        }
    }

    /**
     * Tutup semua resource database (Connection, Statement, ResultSet)
     * 
     * ERROR HANDLING: Try-catch untuk setiap resource yang ditutup
     * 
     * @param connection Connection object
     * @param statement  PreparedStatement object
     * @param resultSet  ResultSet object
     */
    public static void closeAllResources(Connection connection, PreparedStatement statement, ResultSet resultSet) {
        closeResultSet(resultSet);
        closeStatement(statement);
        closeConnection(connection);
    }

    // ============================================================
    // DATABASE QUERY HELPERS
    // ============================================================

    /**
     * Execute query SELECT dan mengembalikan ResultSet
     * 
     * ERROR HANDLING:
     * - Menangani SQLException dari PreparedStatement
     * - Menampilkan pesan error untuk debugging
     * 
     * @param sql    Query SQL (bisa menggunakan parameter ?)
     * @param params Parameter untuk PreparedStatement (opsional)
     * @return ResultSet dari query, null jika error
     */
    public ResultSet executeQuery(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = getConnection();
            if (conn == null) {
                System.out.println("ERROR: Koneksi database gagal");
                return null;
            }

            pstmt = conn.prepareStatement(sql);

            // ERROR HANDLING: Set parameter dengan type checking
            for (int i = 0; i < params.length; i++) {
                try {
                    pstmt.setObject(i + 1, params[i]);
                } catch (SQLException e) {
                    System.out.println("ERROR: Parameter ke-" + (i + 1) + " tidak valid - " + e.getMessage());
                    return null;
                }
            }

            return pstmt.executeQuery();
        } catch (SQLException e) {
            // ERROR HANDLING: SQL Exception
            LOGGER.log(Level.SEVERE, "ERROR: Gagal execute query - " + sql, e);
            System.out.println("ERROR Database: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException ignored) {
                }
            }
            return null;
        }
    }

    /**
     * Execute INSERT, UPDATE, DELETE query
     * 
     * ERROR HANDLING:
     * - SQLException untuk operasi database
     * - Validasi koneksi sebelum execute
     * 
     * @param sql    Query SQL (bisa menggunakan parameter ?)
     * @param params Parameter untuk PreparedStatement (opsional)
     * @return Jumlah baris yang terpengaruh, -1 jika error
     */
    public int executeUpdate(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        int result = -1;

        try {
            conn = getConnection();
            if (conn == null) {
                System.out.println("ERROR: Koneksi database gagal");
                return -1;
            }

            pstmt = conn.prepareStatement(sql);

            // ERROR HANDLING: Set parameter
            for (int i = 0; i < params.length; i++) {
                try {
                    pstmt.setObject(i + 1, params[i]);
                } catch (SQLException e) {
                    System.out.println("ERROR: Parameter ke-" + (i + 1) + " tidak valid");
                    return -1;
                }
            }

            result = pstmt.executeUpdate();
            LOGGER.log(Level.INFO, "Query berhasil dijalankan, " + result + " baris terpengaruh");
            return result;

        } catch (SQLException e) {
            // ERROR HANDLING: Log dan tampilkan error
            LOGGER.log(Level.SEVERE, "ERROR: Gagal execute update - " + sql, e);
            System.out.println("ERROR Database: " + e.getMessage());

            // Helpful error messages
            if (e.getMessage().contains("violation")) {
                System.out.println("Catatan: Kemungkinan data duplikat atau constraint violation");
            }

            return -1;
        } finally {
            // ERROR HANDLING: Cleanup resources
            closeAllResources(conn, pstmt, null);
        }
    }
}
