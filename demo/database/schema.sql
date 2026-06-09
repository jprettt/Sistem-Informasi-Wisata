-- ============================================================
-- SCRIPT DATABASE: Sistem Informasi Eksplorasi Wisata
-- Database: wisata_db
-- DBMS: PostgreSQL
-- ============================================================

-- Drop existing tables if they exist
DROP TABLE IF EXISTS pesanan CASCADE;
DROP TABLE IF EXISTS itinerary_detail CASCADE;
DROP TABLE IF EXISTS itinerary CASCADE;
DROP TABLE IF EXISTS destinasi CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS wishlist CASCADE;

-- ============================================================
-- TABEL USERS - Menyimpan data pengguna aplikasi
-- ============================================================
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('Admin', 'Pengelola', 'Wisatawan')),
    nama_lengkap VARCHAR(100),
    email VARCHAR(100),
    no_telepon VARCHAR(15),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABEL DESTINASI - Menyimpan data wisata
-- ============================================================
CREATE TABLE destinasi (
    id SERIAL PRIMARY KEY,
    nama VARCHAR(100) NOT NULL,
    kategori VARCHAR(50) NOT NULL,
    harga INT NOT NULL,
    deskripsi TEXT,
    koordinat VARCHAR(100),
    lokasi VARCHAR(200),
    rating DECIMAL(3, 2) DEFAULT 0,
    gambar_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABEL PESANAN - Menyimpan data pemesanan tiket
-- ============================================================
CREATE TABLE pesanan (
    id SERIAL PRIMARY KEY,
    user_id INT,
    destinasi_id INT NOT NULL,
    tanggal_kunjungan DATE NOT NULL,
    tipe_tiket VARCHAR(50) NOT NULL,
    jumlah_dewasa INT NOT NULL DEFAULT 1,
    jumlah_anak INT NOT NULL DEFAULT 0,
    nama_pemesan VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    telepon VARCHAR(15) NOT NULL,
    catatan TEXT,
    total_harga INT NOT NULL,
    metode_pembayaran VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'Pending' CHECK (status IN ('Pending', 'Confirmed', 'Cancelled')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (destinasi_id) REFERENCES destinasi(id) ON DELETE CASCADE
);

-- ============================================================
-- TABEL ITINERARY - Menyimpan rencana perjalanan pengguna
-- ============================================================
CREATE TABLE itinerary (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    nama_rencana VARCHAR(150) NOT NULL,
    total_biaya INT DEFAULT 0,
    tanggal_mulai DATE,
    tanggal_selesai DATE,
    deskripsi TEXT,
    status VARCHAR(20) DEFAULT 'Draft' CHECK (status IN ('Draft', 'Finalized', 'Completed')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- TABEL ITINERARY_DETAIL - Detail destinasi dalam rencana perjalanan
-- ============================================================
CREATE TABLE itinerary_detail (
    id SERIAL PRIMARY KEY,
    itinerary_id INT NOT NULL,
    destinasi_id INT NOT NULL,
    urutan INT NOT NULL,
    tanggal_kunjungan DATE,
    catatan TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (itinerary_id) REFERENCES itinerary(id) ON DELETE CASCADE,
    FOREIGN KEY (destinasi_id) REFERENCES destinasi(id) ON DELETE CASCADE,
    UNIQUE(itinerary_id, urutan)
);

-- ============================================================
-- TABEL WISHLIST - Menyimpan daftar favorit pengguna
-- ============================================================
CREATE TABLE wishlist (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    destinasi_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (destinasi_id) REFERENCES destinasi(id) ON DELETE CASCADE,
    UNIQUE(user_id, destinasi_id)
);

-- ============================================================
-- CREATE INDEXES untuk optimasi query
-- ============================================================
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_destinasi_kategori ON destinasi(kategori);
CREATE INDEX idx_itinerary_user_id ON itinerary(user_id);
CREATE INDEX idx_itinerary_detail_itinerary_id ON itinerary_detail(itinerary_id);
CREATE INDEX idx_itinerary_detail_destinasi_id ON itinerary_detail(destinasi_id);

-- ============================================================
-- INSERT DATA SAMPLE
-- ============================================================

-- Sample Users
INSERT INTO users (username, password, role, nama_lengkap, email, no_telepon) VALUES
('admin', 'admin', 'Admin', 'Administrator', 'admin@wisata.com', '081234567890'),
('pengelola01', 'admin', 'Pengelola', 'Pengelola Wisata', 'pengelola@wisata.com', '082345678901'),
('wisatawan01', 'admin', 'Wisatawan', 'Budi Santoso', 'budi@email.com', '083456789012'),
('wisatawan02', 'admin', 'Wisatawan', 'Siti Nurhaliza', 'siti@email.com', '084567890123');

-- Sample Destinasi (Wisata Indonesia)
INSERT INTO destinasi (nama, kategori, harga, deskripsi, koordinat, lokasi, rating) VALUES
('Candi Borobudur', 'Budaya Bersejarah', 750000, 'Candi Budha terbesar di dunia yang dibangun pada abad ke-9', '-7.6087,110.2031', 'Magelang, Jawa Tengah', 4.8),
('Pantai Bali', 'Pantai', 0, 'Pantai indah dengan pasir putih dan pemandangan sunset yang menakjubkan', '-8.6500,115.2167', 'Bali', 4.7),
('Gunung Bromo', 'Alam', 350000, 'Gunung berapi aktif dengan pemandangan yang spektakuler', '-7.9427,112.9520', 'Surabaya, Jawa Timur', 4.9),
('Raja Ampat', 'Pantai', 500000, 'Kepulauan dengan kekayaan laut dan diving spot terbaik', '-0.2192,130.1846', 'Papua Barat', 5.0),
('Danau Toba', 'Alam', 200000, 'Danau vulkanik terbesar di dunia dengan pemandangan alam yang menawan', '2.3000,98.8667', 'Sumatra Utara', 4.6),
('Monumen Nasional', 'Budaya Bersejarah', 50000, 'Monumen peringatan kemerdekaan Indonesia', '-6.1751,106.8249', 'Jakarta', 4.5),
('Taman Nasional Komodo', 'Fauna', 300000, 'Habitat alami komodo dengan keindahan alam yang luar biasa', '-8.6500,119.1667', 'Nusa Tenggara Timur', 4.8),
('Kawah Putih', 'Alam', 100000, 'Danau kawah dengan air berwarna putih yang unik', '-6.8970,107.3570', 'Bandung, Jawa Barat', 4.4);

-- ============================================================
-- KETERANGAN PASSWORD SAMPLE (Plain Text):
-- Username: admin, pengelola01, wisatawan01, wisatawan02
-- Password: 'admin' (Plain Text)
-- ============================================================
