package com.example;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class WishlistController implements Initializable {

    @FXML
    private FlowPane wishlistFlow;
    @FXML
    private Label subtitleLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadWishlist();
    }

    private void loadWishlist() {
        wishlistFlow.getChildren().clear();
        User user = LoginController.getCurrentUser();
        if (user == null) {
            subtitleLabel.setText("Silakan login untuk melihat wishlist Anda.");
            return;
        }

        List<Destinasi> list = DataService.getWishlistForUser(user.getId());
        if (list.isEmpty()) {
            Label empty = new Label("Belum ada destinasi di wishlist Anda.");
            empty.setStyle("-fx-text-fill: #567C8D; -fx-font-size: 14; -fx-padding: 40;");
            wishlistFlow.getChildren().add(empty);
            return;
        }

        for (Destinasi d : list) {
            wishlistFlow.getChildren().add(createCard(d));
        }
    }

    private VBox createCard(Destinasi d) {
        VBox card = new VBox();
        card.getStyleClass().add("destination-card");
        card.setPrefWidth(320);
        card.setSpacing(10);

        Label name = new Label(d.getNama());
        name.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #2F4156;");
        name.setWrapText(true);

        Label lokasi = new Label(d.getLokasi() != null ? d.getLokasi() : "-");
        lokasi.getStyleClass().add("subtitle");

        Button lihat = new Button("Lihat Detail →");
        lihat.getStyleClass().add("secondary");
        lihat.setMaxWidth(Double.MAX_VALUE);
        lihat.setOnAction(e -> {
            DetailDestinasiController.setDestinasiId(d.getId());
            try {
                App.setRoot("detail_destinasi");
            } catch (Exception ex) {
                showInfo("Error", "Gagal membuka detail destinasi: " + ex.getMessage());
            }
        });

        Button hapus = new Button("Hapus");
        hapus.getStyleClass().add("danger");
        hapus.setMaxWidth(Double.MAX_VALUE);
        hapus.setOnAction(e -> {
            User user = LoginController.getCurrentUser();
            if (user == null) {
                showInfo("Login Diperlukan", "Silakan login terlebih dahulu.");
                try {
                    App.setRoot("login");
                } catch (Exception ex) {
                }
                return;
            }
            boolean ok = DataService.removeFromWishlist(user.getId(), d.getId());
            if (ok) {
                showInfo("Wishlist", "Destinasi dihapus dari wishlist.");
                loadWishlist();
            } else {
                showInfo("Error", "Gagal menghapus wishlist.");
            }
        });

        Region spacer = new Region();
        spacer.setPrefHeight(8);

        VBox actions = new VBox(8);
        actions.getChildren().addAll(lihat, hapus);

        card.getChildren().addAll(name, lokasi, spacer, actions);
        return card;
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("home");
        } catch (Exception e) {
            showInfo("Error", "Gagal kembali ke halaman utama.");
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
