package com.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DataService - Akses data terpusat untuk destinasi, pesanan, dan itinerary.
 */
public class DataService {

    private static final Logger LOGGER = Logger.getLogger(DataService.class.getName());

    private DataService() {
    }

    public static List<Destinasi> getAllDestinasi() {
        return queryDestinasi(null, null, 0, -1, -1);
    }

    public static List<Destinasi> searchDestinasi(String keyword, String kategori,
            double minRating, int minHarga, int maxHarga) {
        return queryDestinasi(keyword, kategori, minRating, minHarga, maxHarga);
    }

    private static List<Destinasi> queryDestinasi(String keyword, String kategori,
            double minRating, int minHarga, int maxHarga) {
        List<Destinasi> result = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, nama, kategori, harga, deskripsi, koordinat, lokasi, rating, gambar_url " +
                "FROM destinasi WHERE 1=1");

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (LOWER(nama) LIKE ? OR LOWER(lokasi) LIKE ? OR LOWER(kategori) LIKE ?)");
        }
        if (kategori != null && !kategori.isBlank() && !kategori.equals("Semua Kategori")) {
            sql.append(" AND kategori = ?");
        }
        if (minRating > 0) {
            sql.append(" AND rating >= ?");
        }
        if (minHarga >= 0) {
            sql.append(" AND harga >= ?");
        }
        if (maxHarga >= 0) {
            sql.append(" AND harga <= ?");
        }
        sql.append(" ORDER BY rating DESC, nama ASC");

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                return getFallbackDestinasi();
            }

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                if (keyword != null && !keyword.isBlank()) {
                    String pattern = "%" + keyword.toLowerCase() + "%";
                    ps.setString(idx++, pattern);
                    ps.setString(idx++, pattern);
                    ps.setString(idx++, pattern);
                }
                if (kategori != null && !kategori.isBlank() && !kategori.equals("Semua Kategori")) {
                    ps.setString(idx++, kategori);
                }
                if (minRating > 0) {
                    ps.setDouble(idx++, minRating);
                }
                if (minHarga >= 0) {
                    ps.setInt(idx++, minHarga);
                }
                if (maxHarga >= 0) {
                    ps.setInt(idx, maxHarga);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.add(mapDestinasi(rs));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal memuat destinasi", e);
            return getFallbackDestinasi();
        }

        return result.isEmpty() ? getFallbackDestinasi() : result;
    }

    public static Destinasi getDestinasiById(int id) {
        String sql = "SELECT id, nama, kategori, harga, deskripsi, koordinat, lokasi, rating, gambar_url " +
                     "FROM destinasi WHERE id = ?";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                return findFallbackById(id);
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapDestinasi(rs);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal memuat destinasi id=" + id, e);
        }

        return findFallbackById(id);
    }

    public static List<String> getDistinctKategori() {
        List<String> kategori = new ArrayList<>();
        kategori.add("Semua Kategori");

        String sql = "SELECT DISTINCT kategori FROM destinasi ORDER BY kategori";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                return List.of("Semua Kategori", "Alam", "Pantai", "Budaya Bersejarah", "Fauna");
            }

            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    kategori.add(rs.getString("kategori"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Gagal memuat kategori", e);
            kategori.addAll(List.of("Alam", "Pantai", "Budaya Bersejarah", "Fauna"));
        }

        return kategori;
    }

    public static int savePesanan(int userId, int destinasiId, LocalDate tanggal, String tipeTiket,
            int jumlahDewasa, int jumlahAnak, String nama, String email, String telepon,
            String catatan, int totalHarga, String metodeBayar) {
        String sql = "INSERT INTO pesanan (user_id, destinasi_id, tanggal_kunjungan, tipe_tiket, " +
                     "jumlah_dewasa, jumlah_anak, nama_pemesan, email, telepon, catatan, " +
                     "total_harga, metode_pembayaran, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending') RETURNING id";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                return -1;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setObject(1, userId > 0 ? userId : null);
                ps.setInt(2, destinasiId);
                ps.setDate(3, java.sql.Date.valueOf(tanggal));
                ps.setString(4, tipeTiket);
                ps.setInt(5, jumlahDewasa);
                ps.setInt(6, jumlahAnak);
                ps.setString(7, nama);
                ps.setString(8, email);
                ps.setString(9, telepon);
                ps.setString(10, catatan);
                ps.setInt(11, totalHarga);
                ps.setString(12, metodeBayar);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menyimpan pesanan", e);
        }

        return -1;
    }

    public static String getPesananSummaryForUser(int userId) {
        String sql = "SELECT p.id, d.nama, p.tanggal_kunjungan, p.total_harga, p.status " +
                     "FROM pesanan p JOIN destinasi d ON d.id = p.destinasi_id " +
                     "WHERE p.user_id = ? ORDER BY p.created_at DESC LIMIT 10";

        StringBuilder sb = new StringBuilder();

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                return "Tidak dapat terhubung ke database.";
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean found = false;
                    while (rs.next()) {
                        found = true;
                        sb.append(String.format("#%d - %s (%s) - %s - %s%n",
                                rs.getInt("id"),
                                rs.getString("nama"),
                                rs.getDate("tanggal_kunjungan").toLocalDate(),
                                formatRupiah(rs.getInt("total_harga")),
                                rs.getString("status")));
                    }
                    if (!found) {
                        return "Belum ada pesanan. Pesan tiket dari halaman detail destinasi.";
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal memuat pesanan", e);
            return "Gagal memuat data pesanan.";
        }

        return sb.toString().trim();
    }

    public static List<Itinerary> getItinerariesForUser(int userId) {
        List<Itinerary> list = new ArrayList<>();
        String sql = "SELECT id, nama_rencana, total_biaya, tanggal_mulai, tanggal_selesai, deskripsi, status " +
                     "FROM itinerary WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                return list;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Itinerary(
                                rs.getInt("id"),
                                userId,
                                rs.getString("nama_rencana"),
                                rs.getInt("total_biaya"),
                                rs.getDate("tanggal_mulai") != null
                                        ? rs.getDate("tanggal_mulai").toLocalDate() : null,
                                rs.getDate("tanggal_selesai") != null
                                        ? rs.getDate("tanggal_selesai").toLocalDate() : null,
                                rs.getString("deskripsi"),
                                rs.getString("status")));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal memuat itinerary", e);
        }

        return list;
    }

    public static int getFirstItineraryIdForUser(int userId) {
        List<Itinerary> list = getItinerariesForUser(userId);
        return list.isEmpty() ? -1 : list.get(0).getId();
    }

    private static Destinasi mapDestinasi(ResultSet rs) throws SQLException {
        return new Destinasi(
                rs.getInt("id"),
                rs.getString("nama"),
                rs.getString("kategori"),
                rs.getInt("harga"),
                rs.getString("deskripsi"),
                rs.getString("koordinat"),
                rs.getString("lokasi"),
                rs.getDouble("rating"),
                rs.getString("gambar_url"));
    }

    private static List<Destinasi> getFallbackDestinasi() {
        List<Destinasi> list = new ArrayList<>();
        list.add(new Destinasi(1, "Candi Borobudur", "Budaya Bersejarah", 750000,
                "Candi Buddha terbesar di dunia", "-7.6087,110.2031", "Magelang, Jawa Tengah", 4.8, null));
        list.add(new Destinasi(2, "Pantai Bali", "Pantai", 0,
                "Pantai indah dengan sunset menakjubkan", "-8.6500,115.2167", "Bali", 4.7, null));
        list.add(new Destinasi(3, "Gunung Bromo", "Alam", 350000,
                "Gunung berapi dengan pemandangan spektakuler", "-7.9427,112.9520", "Surabaya, Jawa Timur", 4.9, null));
        return list;
    }

    private static Destinasi findFallbackById(int id) {
        return getFallbackDestinasi().stream()
                .filter(d -> d.getId() == id)
                .findFirst()
                .orElse(getFallbackDestinasi().get(0));
    }

    public static String formatRupiah(int amount) {
        if (amount == 0) {
            return "Gratis";
        }
        return String.format("Rp %,d", amount).replace(",", ".");
    }

    public static int parseHargaFilter(String filter) {
        if (filter == null || filter.equals("Semua Harga")) {
            return -1;
        }
        if (filter.startsWith("<")) {
            return 0;
        }
        if (filter.contains("50.000") && filter.contains("100.000")) {
            return 50000;
        }
        if (filter.contains("100.000") && filter.contains("200.000")) {
            return 100000;
        }
        if (filter.startsWith(">")) {
            return 200001;
        }
        return -1;
    }

    public static int parseHargaFilterMax(String filter) {
        if (filter == null || filter.equals("Semua Harga")) {
            return -1;
        }
        if (filter.startsWith("<")) {
            return 49999;
        }
        if (filter.contains("50.000") && filter.contains("100.000")) {
            return 100000;
        }
        if (filter.contains("100.000") && filter.contains("200.000")) {
            return 200000;
        }
        if (filter.startsWith(">")) {
            return Integer.MAX_VALUE;
        }
        return -1;
    }

    public static double parseRatingFilter(String filter) {
        if (filter == null || filter.equals("Semua Rating")) {
            return 0;
        }
        if (filter.contains("4.5")) {
            return 4.5;
        }
        if (filter.contains("4.0")) {
            return 4.0;
        }
        if (filter.contains("3.0")) {
            return 3.0;
        }
        return 0;
    }

    public static List<Pesanan> getPesananForUser(int userId) {
        List<Pesanan> list = new ArrayList<>();
        String sql = "SELECT p.id, p.destinasi_id, d.nama AS destinasi_nama, p.tanggal_kunjungan, p.total_harga, p.status "
                + "FROM pesanan p JOIN destinasi d ON d.id = p.destinasi_id WHERE p.user_id = ? ORDER BY p.created_at DESC";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null)
                return list;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Pesanan(
                                rs.getInt("id"),
                                rs.getInt("destinasi_id"),
                                rs.getString("destinasi_nama"),
                                rs.getDate("tanggal_kunjungan") != null ? rs.getDate("tanggal_kunjungan").toLocalDate() : null,
                                rs.getInt("total_harga"),
                                rs.getString("status")));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal memuat pesanan user=" + userId, e);
        }

        return list;
    }

    public static boolean cancelPesanan(int pesananId, int userId) {
        String sql = "UPDATE pesanan SET status = 'Cancelled' WHERE id = ? AND user_id = ? AND status <> 'Cancelled'";
        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, pesananId);
                ps.setInt(2, userId);
                int updated = ps.executeUpdate();
                return updated > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal membatalkan pesanan", e);
        }
        return false;
    }

    /* ----------------- Wishlist Methods ----------------- */
    public static boolean addToWishlist(int userId, int destinasiId) {
        String sql = "INSERT INTO wishlist (user_id, destinasi_id) VALUES (?, ?) ON CONFLICT DO NOTHING RETURNING id";
        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, destinasiId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menambahkan wishlist", e);
        }
        return false;
    }

    public static boolean removeFromWishlist(int userId, int destinasiId) {
        String sql = "DELETE FROM wishlist WHERE user_id = ? AND destinasi_id = ?";
        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null)
                return false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, destinasiId);
                int affected = ps.executeUpdate();
                return affected > 0;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal menghapus wishlist", e);
        }
        return false;
    }

    public static List<Destinasi> getWishlistForUser(int userId) {
        List<Destinasi> list = new ArrayList<>();
        String sql = "SELECT d.id, d.nama, d.kategori, d.harga, d.deskripsi, d.koordinat, d.lokasi, d.rating, d.gambar_url "
                + "FROM destinasi d JOIN wishlist w ON d.id = w.destinasi_id WHERE w.user_id = ? ORDER BY w.created_at DESC";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null)
                return list;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapDestinasi(rs));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Gagal memuat wishlist", e);
        }

        return list;
    }
}
