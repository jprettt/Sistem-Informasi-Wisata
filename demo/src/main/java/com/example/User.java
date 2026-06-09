package com.example;

/**
 * User Model - Merepresentasikan data pengguna dari tabel users
 * 
 * @author Sistem Eksplorasi Wisata B6
 */
public class User {
    private int id;
    private String username;
    private String password;
    private String role; // Admin, Pengelola, Wisatawan
    private String namaLengkap;
    private String email;
    private String noTelepon;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    /**
     * Constructor tanpa parameter (default)
     */
    public User() {
    }

    /**
     * Constructor dengan parameter
     */
    public User(int id, String username, String password, String role, String namaLengkap,
            String email, String noTelepon) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.namaLengkap = namaLengkap;
        this.email = email;
        this.noTelepon = noTelepon;
    }

    /**
     * Constructor login (username, password, role)
     */
    public User(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    // ============================================================
    // GETTER DAN SETTER
    // ============================================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getNamaLengkap() {
        return namaLengkap;
    }

    public void setNamaLengkap(String namaLengkap) {
        this.namaLengkap = namaLengkap;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNoTelepon() {
        return noTelepon;
    }

    public void setNoTelepon(String noTelepon) {
        this.noTelepon = noTelepon;
    }

    // ============================================================
    // OVERRIDE METHOD
    // ============================================================

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", namaLengkap='" + namaLengkap + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
