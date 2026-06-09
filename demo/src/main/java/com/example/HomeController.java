package com.example;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());

    @FXML
    private Button navHome;
    @FXML
    private Button navBooking;
    @FXML
    private Button navWishlist;
    @FXML
    private Button navItinerary;
    @FXML
    private Button profileButton;

    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;

    @FXML
    private ComboBox<String> kategoriFilter;
    @FXML
    private ComboBox<String> ratingFilter;
    @FXML
    private ComboBox<String> hargaFilter;
    @FXML
    private Button resetFilterButton;

    @FXML
    private FlowPane destinationFlowPane;
    @FXML
    private FlowPane categoryFlowPane;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        updateProfileButton();
        loadDestinations();

        kategoriFilter.setOnAction(e -> loadDestinations());
        ratingFilter.setOnAction(e -> loadDestinations());
        hargaFilter.setOnAction(e -> loadDestinations());
        searchField.setOnAction(e -> handleSearch());
    }

    private void setupFilters() {
        kategoriFilter.setItems(FXCollections.observableArrayList(DataService.getDistinctKategori()));
        kategoriFilter.getSelectionModel().selectFirst();

        ratingFilter.setItems(FXCollections.observableArrayList(
                "Semua Rating", "4.5+", "4.0+", "3.0+"));
        ratingFilter.getSelectionModel().selectFirst();

        hargaFilter.setItems(FXCollections.observableArrayList(
                "Semua Harga", "< Rp 50.000", "Rp 50.000 – 100.000",
                "Rp 100.000 – 200.000", "> Rp 200.000"));
        hargaFilter.getSelectionModel().selectFirst();
    }

    private void updateProfileButton() {
        User user = LoginController.getCurrentUser();
        if (user != null) {
            profileButton.setText(user.getNamaLengkap());
        } else {
            profileButton.setText("Login");
        }
    }

    private void loadDestinations() {
        destinationFlowPane.getChildren().clear();

        String keyword = searchField.getText() != null ? searchField.getText().trim() : "";
        String kategori = kategoriFilter.getValue();
        double minRating = DataService.parseRatingFilter(ratingFilter.getValue());
        int minHarga = DataService.parseHargaFilter(hargaFilter.getValue());
        int maxHarga = DataService.parseHargaFilterMax(hargaFilter.getValue());

        List<Destinasi> destinasiList = DataService.searchDestinasi(keyword, kategori, minRating, minHarga, maxHarga);

        if (destinasiList.isEmpty()) {
            Label empty = new Label("Tidak ada destinasi yang cocok dengan filter.");
            empty.setStyle("-fx-text-fill: #567C8D; -fx-font-size: 14; -fx-padding: 40;");
            destinationFlowPane.getChildren().add(empty);
            return;
        }

        for (Destinasi d : destinasiList) {
            destinationFlowPane.getChildren().add(createDestinationCard(d));
        }
    }

    private VBox createDestinationCard(Destinasi d) {
        VBox card = new VBox();
        card.getStyleClass().add("destination-card");
        card.setPrefWidth(280);

        VBox imgPlaceholder = new VBox();
        imgPlaceholder.setMinHeight(160);
        imgPlaceholder.setStyle("-fx-background-color: #252525; -fx-alignment: center;");
        Label imgIcon = new Label(d.getKategori() != null ? d.getKategori().toUpperCase() : "WISATA");
        imgIcon.setStyle("-fx-font-size: 14; -fx-text-fill: #2F4156; -fx-font-weight: bold; -fx-padding: 50 0;");
        imgPlaceholder.getChildren().add(imgIcon);

        VBox info = new VBox(8);
        info.setStyle("-fx-padding: 14 15 16 15;");

        Label namaLabel = new Label(d.getNama());
        namaLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #2F4156;");
        namaLabel.setWrapText(true);

        Label lokasiLabel = new Label("📍 " + (d.getLokasi() != null ? d.getLokasi() : "-"));
        lokasiLabel.getStyleClass().add("subtitle");

        HBox ratingRow = new HBox(8);
        ratingRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label ratingLabel = new Label("⭐ " + String.format("%.1f", d.getRating()));
        ratingLabel.getStyleClass().add("rating-badge");
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Label hargaLabel = new Label(d.getHargaFormatted());
        hargaLabel.getStyleClass().add("price-tag");
        ratingRow.getChildren().addAll(ratingLabel, spacer, hargaLabel);

        Button detailBtn = new Button("Lihat Detail →");
        detailBtn.setStyle("-fx-padding: 9 15; -fx-font-size: 12; -fx-border-radius: 10; -fx-background-radius: 10;");
        detailBtn.setMaxWidth(Double.MAX_VALUE);
        detailBtn.setOnAction(e -> handleLihatDetail(d));

        Button bookingBtn = new Button("Booking");
        bookingBtn.setStyle("-fx-padding: 7 12; -fx-font-size: 12; -fx-border-radius: 8; -fx-background-radius: 8;");
        bookingBtn.setOnAction(e -> handleBooking(d));

        info.getChildren().addAll(namaLabel, lokasiLabel, ratingRow, detailBtn, bookingBtn);
        card.getChildren().addAll(imgPlaceholder, info);
        return card;
    }

    private void handleBooking(Destinasi destinasi) {
        User user = LoginController.getCurrentUser();
        if (user == null) {
            showInfo("Login Diperlukan", "Silakan login terlebih dahulu untuk melakukan booking.");
            try {
                App.setRoot("login");
            } catch (Exception e) {
                showError("Error", "Gagal membuka halaman login: " + e.getMessage());
            }
            return;
        }

        BookingController.setDestinasiId(destinasi.getId());
        BookingController.setDestinasiNama(destinasi.getNama());
        BookingController.setBaseHarga(destinasi.getHarga());
        try {
            App.setRoot("booking");
        } catch (Exception e) {
            showError("Error", "Gagal membuka halaman booking: " + e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        loadDestinations();
    }

    @FXML
    private void handleResetFilter() {
        searchField.clear();
        kategoriFilter.getSelectionModel().selectFirst();
        ratingFilter.getSelectionModel().selectFirst();
        hargaFilter.getSelectionModel().selectFirst();
        loadDestinations();
    }

    @FXML
    private void handleLihatSemua() {
        searchField.clear();
        kategoriFilter.getSelectionModel().selectFirst();
        ratingFilter.getSelectionModel().selectFirst();
        hargaFilter.getSelectionModel().selectFirst();
        loadDestinations();
    }

    private void handleLihatDetail(Destinasi destinasi) {
        DetailDestinasiController.setDestinasiId(destinasi.getId());
        try {
            App.setRoot("detail_destinasi");
        } catch (Exception e) {
            showError("Error", "Gagal membuka halaman detail: " + e.getMessage());
        }
    }

    @FXML
    private void handleNavBooking() {
        User user = LoginController.getCurrentUser();
        if (user == null) {
            showInfo("Login Diperlukan", "Silakan login terlebih dahulu untuk melihat pesanan Anda.");
            try {
                App.setRoot("login");
            } catch (Exception e) {
                showError("Error", "Gagal membuka halaman login.");
            }
            return;
        }

        try {
            App.setRoot("bookings");
        } catch (Exception e) {
            showError("Error", "Gagal membuka halaman Booking: " + e.getMessage());
        }
    }

    @FXML
    private void handleNavItinerary() {
        User user = LoginController.getCurrentUser();
        if (user == null) {
            showInfo("Login Diperlukan", "Silakan login untuk mengakses itinerary.");
            try {
                App.setRoot("login");
            } catch (Exception e) {
                showError("Error", "Gagal membuka halaman login.");
            }
            return;
        }

        int itineraryId = DataService.getFirstItineraryIdForUser(user.getId());
        ItineraryPlannerController.setItineraryId(itineraryId > 0 ? itineraryId : 0);
        ItineraryPlannerController.setCurrentUser(user);

        try {
            App.setRoot("itinerary_planner");
        } catch (Exception e) {
            showError("Error", "Gagal membuka itinerary planner.");
        }
    }

    @FXML
    private void handleLogin() {
        System.out.println("HOME: login button clicked");
        User user = LoginController.getCurrentUser();
        if (user != null) {
            try {
                App.setRoot("dashboard", (DashboardController c) -> {
                    c.setCurrentUser(user);
                    c.loadDestinations();
                });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Gagal membuka dashboard", e);
                showError("Error", "Gagal membuka dashboard: " + e.getMessage());
            }
        } else {
            try {
                App.setRoot("login");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Gagal membuka halaman login", e);
                showError("Error", "Gagal membuka halaman login: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleNavHome() {
        try {
            App.setRoot("home");
        } catch (Exception e) {
            showError("Error", "Gagal kembali ke halaman utama.");
        }
    }

    @FXML
    private void handleNavWishlist() {
        User user = LoginController.getCurrentUser();
        if (user == null) {
            showInfo("Login Diperlukan", "Silakan login terlebih dahulu untuk melihat wishlist Anda.");
            try {
                App.setRoot("login");
            } catch (Exception e) {
                showError("Error", "Gagal membuka halaman login.");
            }
            return;
        }

        try {
            App.setRoot("wishlist");
        } catch (Exception e) {
            showError("Error", "Gagal membuka halaman wishlist: " + e.getMessage());
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
