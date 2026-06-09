package com.example;

import java.time.LocalDate;

public class Pesanan {
    private final int id;
    private final int destinasiId;
    private final String destinasiNama;
    private final LocalDate tanggalKunjungan;
    private final int totalHarga;
    private final String status;

    public Pesanan(int id, int destinasiId, String destinasiNama, LocalDate tanggalKunjungan, int totalHarga,
            String status) {
        this.id = id;
        this.destinasiId = destinasiId;
        this.destinasiNama = destinasiNama;
        this.tanggalKunjungan = tanggalKunjungan;
        this.totalHarga = totalHarga;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public int getDestinasiId() {
        return destinasiId;
    }

    public String getDestinasiNama() {
        return destinasiNama;
    }

    public LocalDate getTanggalKunjungan() {
        return tanggalKunjungan;
    }

    public int getTotalHarga() {
        return totalHarga;
    }

    public String getStatus() {
        return status;
    }
}
