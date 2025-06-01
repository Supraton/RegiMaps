package com.example.regimaps;

import com.google.android.gms.maps.model.LatLng;

/**
 * Classe que representa un punt guardat al mapa, amb informació associada
 * com el nom, la data, un codi i, opcionalment, el camí d'una foto(no implementat).
 */
public class SavedPoint {
    private LatLng location;
    private String name;
    private String date;
    private String code;
    private String photoPath;

    // Constructor per crear un SavedPoint sense foto
    public SavedPoint(LatLng location, String name, String date, String code) {
        this(location, name, date, code, null); // Crida l'altre constructor
    }

    // Constructor per crear un SavedPoint amb tots els atributs
    public SavedPoint(LatLng location, String name, String date, String code, String photoPath) {
        this.location = location;
        this.name = name;
        this.date = date;
        this.code = code;
        this.photoPath = photoPath;
    }

    // Getters
    public LatLng getLocation() {
        return location;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getCode() {
        return code;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    // Setters
    public void setLocation(LatLng location) {
        this.location = location;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }
}