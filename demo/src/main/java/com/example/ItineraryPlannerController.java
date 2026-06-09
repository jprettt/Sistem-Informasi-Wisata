package com.example;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ItineraryPlannerController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ItineraryPlannerController.class.getName());

    @FXML private Label namaRencanaLabel;
    @FXML private Label tanggalRencanaLabel;
    @FXML private Label durasiLabel;
    @FXML private Label pesertaLabel;
    @FXML private TextArea deskripsiTextArea;
    @FXML private VBox timelineVBox;
    @FXML private Label totalBiayaLabel;
    @FXML private TextField kodeUndanganField;
    @FXML private Label pesertaDetailLabel;
    @FXML private Label lastUpdateLabel;

    private static int currentItineraryId = 0;
    private static User currentUser;

    public static void setItineraryId(int id) {
        currentItineraryId = id;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (currentUser == null) {
            currentUser = LoginController.getCurrentUser();
        }
        loadItineraryData();
        loadTimeline();
    }

    private void loadItineraryData() {
        if (currentItineraryId <= 0 || currentUser == null) {
            namaRencanaLabel.setText("Belum ada rencana perjalanan");
            tanggalRencanaLabel.setText("-");
            durasiLabel.setText("-");
            pesertaLabel.setText("-");
            deskripsiTextArea.setText("Buat rencana perjalanan dari menu Dashboard atau Itinerary.");
            totalBiayaLabel.setText("Rp 0");
            if (kodeUndanganField != null) {
                kodeUndanganField.setText("-");
            }
            return;
        }

        String sql = "SELECT nama_rencana, tanggal_mulai, tanggal_selesai, deskripsi, total_biaya, updated_at " +
                     "FROM itinerary WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentItineraryId);
                ps.setInt(2, currentUser.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        namaRencanaLabel.setText(rs.getString("nama_rencana"));
                        var start = rs.getDate("tanggal_mulai");
                        var end = rs.getDate("tanggal_selesai");
                        if (start != null && end != null) {
                            tanggalRencanaLabel.setText(
                                    start.toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) +
                                    " – " + end.toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
                            long days = java.time.temporal.ChronoUnit.DAYS.between(
                                    start.toLocalDate(), end.toLocalDate()) + 1;
                            durasiLabel.setText(days + " Hari");
                        } else {
                            tanggalRencanaLabel.setText("Belum ditentukan");
                            durasiLabel.setText("-");
                        }
                        deskripsiTextArea.setText(rs.getString("deskripsi") != null ? rs.getString("deskripsi") : "");
                        totalBiayaLabel.setText(DataService.formatRupiah(rs.getInt("total_biaya")));
                        if (kodeUndanganField != null) {
                            kodeUndanganField.setText("TRIP" + currentItineraryId);
                        }
                        if (lastUpdateLabel != null && rs.getTimestamp("updated_at") != null) {
                            lastUpdateLabel.setText(rs.getTimestamp("updated_at").toLocalDateTime()
                                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Gagal memuat itinerary", e);
        }

        pesertaLabel.setText("1 Orang");
        if (pesertaDetailLabel != null) {
            pesertaDetailLabel.setText(currentUser.getNamaLengkap());
        }
    }

    private void loadTimeline() {
        if (timelineVBox == null) {
            return;
        }
        timelineVBox.getChildren().clear();

        if (currentItineraryId <= 0) {
            Label empty = new Label("Belum ada destinasi dalam itinerary. Tambahkan dari Dashboard.");
            empty.setStyle("-fx-text-fill: #567C8D; -fx-padding: 20;");
            timelineVBox.getChildren().add(empty);
            return;
        }

        String sql = "SELECT id.id, id.urutan, d.nama, d.lokasi, d.harga " +
                     "FROM itinerary_detail id " +
                     "JOIN destinasi d ON d.id = id.destinasi_id " +
                     "WHERE id.itinerary_id = ? ORDER BY id.urutan ASC";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentItineraryId);
                try (ResultSet rs = ps.executeQuery()) {
                    boolean found = false;
                    int total = 0;
                    while (rs.next()) {
                        found = true;
                        int harga = rs.getInt("harga");
                        total += harga;
                        timelineVBox.getChildren().add(createTimelineItem(
                                rs.getInt("urutan"),
                                rs.getString("nama"),
                                rs.getString("lokasi"),
                                harga));
                    }
                    if (!found) {
                        Label empty = new Label("Belum ada destinasi. Buka Dashboard → Tambah ke Itinerary.");
                        empty.setStyle("-fx-text-fill: #567C8D; -fx-padding: 20;");
                        timelineVBox.getChildren().add(empty);
                    } else {
                        totalBiayaLabel.setText(DataService.formatRupiah(total));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Gagal memuat timeline", e);
        }
    }

    private HBox createTimelineItem(int urutan, String nama, String lokasi, int harga) {
        String jam = String.format("%02d:00", 8 + urutan);

        HBox row = new HBox(0);
        row.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        VBox timeCol = new VBox(0);
        timeCol.setPrefWidth(80);
        Label timeLabel = new Label(jam);
        timeLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2F4156; -fx-padding: 10;");
        timeCol.getChildren().add(timeLabel);

        VBox card = new VBox(8);
        card.getStyleClass().add("timeline-card");
        card.setStyle("-fx-padding: 16; -fx-background-color: #1a1a1a; -fx-border-radius: 12; -fx-background-radius: 12;");

        Label namaLabel = new Label(nama);
        namaLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2F4156;");
        Label lokasiLabel = new Label("📍 " + lokasi);
        lokasiLabel.getStyleClass().add("subtitle");
        Label hargaLabel = new Label(DataService.formatRupiah(harga));
        hargaLabel.getStyleClass().add("price-tag");

        card.getChildren().addAll(namaLabel, lokasiLabel, hargaLabel);
        HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
        card.setStyle(card.getStyle() + "; -fx-padding: 0 0 20 20;");

        VBox content = new VBox(card);
        content.setStyle("-fx-padding: 0 0 28 20;");
        HBox.setHgrow(content, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(timeCol, content);
        return row;
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("home");
        } catch (Exception e) {
            showError("Error", "Gagal kembali ke halaman utama.");
        }
    }

    @FXML
    private void handleSimpan() {
        if (currentItineraryId <= 0 || currentUser == null) {
            showError("Perhatian", "Tidak ada itinerary yang dapat disimpan. Buat dari Dashboard terlebih dahulu.");
            return;
        }

        String sql = "UPDATE itinerary SET deskripsi = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                showError("Error", "Gagal terhubung ke database.");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, deskripsiTextArea.getText());
                ps.setInt(2, currentItineraryId);
                ps.setInt(3, currentUser.getId());
                ps.executeUpdate();
                showInfo("Berhasil", "Rencana perjalanan berhasil disimpan.");
                loadItineraryData();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Gagal menyimpan itinerary", e);
            showError("Error", "Gagal menyimpan rencana perjalanan.");
        }
    }

    @FXML
    private void handleBagikan() {
        handleCopyKode();
        showInfo("Bagikan", "Kode undangan telah disalin. Bagikan ke teman Anda!");
    }

    @FXML
    private void handleTambahDestinasi() {
        showInfo("Tambah Destinasi",
                "Buka Dashboard setelah login, pilih destinasi, lalu klik 'Tambah ke Itinerary'.");
        try {
            if (currentUser != null) {
                App.setRoot("dashboard", (DashboardController c) -> {
                    c.setCurrentUser(currentUser);
                    c.loadDestinations();
                });
            } else {
                App.setRoot("login");
            }
        } catch (Exception e) {
            showError("Error", "Gagal membuka halaman.");
        }
    }

    @FXML
    private void handleHapusItinerary() {
        if (currentItineraryId <= 0 || currentUser == null) {
            showError("Perhatian", "Tidak ada itinerary untuk dihapus.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Konfirmasi");
        confirm.setHeaderText("Hapus itinerary ini?");
        confirm.setContentText("Tindakan ini tidak dapat dibatalkan.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        String sql = "DELETE FROM itinerary WHERE id = ? AND user_id = ?";

        try (Connection conn = DatabaseHelper.getInstance().getConnection()) {
            if (conn == null) {
                showError("Error", "Gagal terhubung ke database.");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, currentItineraryId);
                ps.setInt(2, currentUser.getId());
                ps.executeUpdate();
                currentItineraryId = 0;
                showInfo("Berhasil", "Itinerary berhasil dihapus.");
                loadItineraryData();
                loadTimeline();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Gagal menghapus itinerary", e);
            showError("Error", "Gagal menghapus itinerary.");
        }
    }

    @FXML
    private void handleCopyKode() {
        if (kodeUndanganField != null && kodeUndanganField.getText() != null) {
            ClipboardContent content = new ClipboardContent();
            content.putString(kodeUndanganField.getText());
            Clipboard.getSystemClipboard().setContent(content);
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
