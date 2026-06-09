package com.example;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class BookingsController implements Initializable {

    @FXML
    private FlowPane bookingsFlow;
    @FXML
    private Label subtitleLabel;

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadBookings();
    }

    private void loadBookings() {
        bookingsFlow.getChildren().clear();
        User user = LoginController.getCurrentUser();
        if (user == null) {
            subtitleLabel.setText("Silakan login untuk melihat booking Anda.");
            return;
        }

        List<Pesanan> list = DataService.getPesananForUser(user.getId());
        if (list.isEmpty()) {
            Label empty = new Label("Belum ada pesanan. Pesan tiket dari halaman detail destinasi.");
            empty.setStyle("-fx-text-fill: #567C8D; -fx-font-size: 14; -fx-padding: 40;");
            bookingsFlow.getChildren().add(empty);
            return;
        }

        for (Pesanan p : list) {
            bookingsFlow.getChildren().add(createCard(p));
        }
    }

    private VBox createCard(Pesanan p) {
        VBox card = new VBox();
        card.getStyleClass().addAll("destination-card", "booking-card");
        card.setPrefWidth(350);
        card.setMaxWidth(350);
        card.setSpacing(10);

        Label title = new Label(p.getDestinasiNama());
        title.getStyleClass().add("title");
        title.setWrapText(true);

        Label badge = new Label("#" + p.getId());
        badge.getStyleClass().addAll("booking-id-badge");

        Label status = new Label(p.getStatus());
        status.getStyleClass().addAll("booking-status-badge");
        applyStatusStyle(status, p.getStatus());

        VBox header = new VBox(6);
        header.getChildren().addAll(badge, title, status);

        Label when = new Label(
                "Tanggal: " + (p.getTanggalKunjungan() != null ? p.getTanggalKunjungan().format(DF) : "—"));
        when.getStyleClass().add("subtitle");

        Label price = new Label("Total: " + DataService.formatRupiah(p.getTotalHarga()));
        price.getStyleClass().add("price-tag");

        Button details = new Button("Lihat Destinasi →");
        details.getStyleClass().add("secondary");
        details.setMaxWidth(Double.MAX_VALUE);
        details.setOnAction(e -> {
            DetailDestinasiController.setDestinasiId(p.getDestinasiId());
            try {
                App.setRoot("detail_destinasi");
            } catch (Exception ex) {
                showInfo("Error", "Gagal membuka detail.");
            }
        });

        Button cancel = new Button("Batalkan");
        cancel.getStyleClass().add("danger");
        cancel.setMaxWidth(Double.MAX_VALUE);
        cancel.setOnAction(e -> {
            User user = LoginController.getCurrentUser();
            if (user == null) {
                showInfo("Login Diperlukan", "Silakan login terlebih dahulu.");
                try {
                    App.setRoot("login");
                } catch (Exception ex) {
                }
                return;
            }
            boolean ok = DataService.cancelPesanan(p.getId(), user.getId());
            if (ok) {
                showInfo("Booking", "Pesanan dibatalkan.");
                loadBookings();
            } else {
                showInfo("Booking", "Gagal membatalkan pesanan (mungkin sudah dibatalkan).");
            }
        });

        Region spacer = new Region();
        spacer.setPrefHeight(8);

        VBox actions = new VBox(8);
        actions.getChildren().addAll(details, cancel);

        card.getChildren().addAll(header, when, price, spacer, actions);
        return card;
    }

    private void applyStatusStyle(Label status, String value) {
        String normalized = value == null ? "" : value.toLowerCase();
        if (normalized.contains("pending")) {
            status.setStyle(
                    "-fx-background-color: rgba(47,65,86,0.1); -fx-text-fill: #2F4156; -fx-padding: 5 10; -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-color: rgba(47,65,86,0.2); -fx-border-width: 1;");
        } else if (normalized.contains("confirm")) {
            status.setStyle(
                    "-fx-background-color: rgba(46,204,113,0.15); -fx-text-fill: #27AE60; -fx-padding: 5 10; -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-color: rgba(46,204,113,0.25); -fx-border-width: 1;");
        } else if (normalized.contains("cancel")) {
            status.setStyle(
                    "-fx-background-color: rgba(192,57,43,0.1); -fx-text-fill: #C0392B; -fx-padding: 5 10; -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-color: rgba(192,57,43,0.2); -fx-border-width: 1;");
        } else {
            status.setStyle(
                    "-fx-background-color: rgba(47,65,86,0.05); -fx-text-fill: #567C8D; -fx-padding: 5 10; -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-color: rgba(47,65,86,0.1); -fx-border-width: 1;");
        }
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("home");
        } catch (Exception e) {
            showInfo("Error", "Gagal kembali ke home.");
        }
    }

    private void showInfo(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
