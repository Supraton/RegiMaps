package com.example.regimaps;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

/**
 * Entitat Room que representa un punt guardat a la base de dades.
 * Emmagatzema la informació bàsica d'un punt geogràfic amb dades addicionals.
 */
@Entity(tableName = "saved_points")
public class SavedPointEntity {

    // Identificador únic autogenerat per Room
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Latitud del punt
    @ColumnInfo(name = "latitude")
    public double latitude;

    // Longitud del punt
    @ColumnInfo(name = "longitude")
    public double longitude;

    // Nom descriptiu del punt
    @ColumnInfo(name = "name")
    public String name;

    // Data d'emmagatzematge o registre del punt
    @ColumnInfo(name = "date")
    public String date;

    // Codi identificador associat al punt
    @ColumnInfo(name = "code")
    public String code;

    // Ruta a la foto associada al punt
    @ColumnInfo(name = "photoPath", defaultValue = "")
    public String photoPath;

    // Constructor complet
    public SavedPointEntity(double latitude, double longitude, String name, String date, String code, String photoPath) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.date = date;
        this.code = code;
        this.photoPath = photoPath;
    }

    // Constructor buit necessari per Room
    public SavedPointEntity() {}

    // Mètode per obtenir un objecte LatLng
    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    // Mètode per convertir un SavedPointEntity a un SavedPoint
    public SavedPoint toSavedPoint() {
        return new SavedPoint(new LatLng(latitude, longitude), name, date, code, photoPath);
    }

    // Mètode estàtic per convertir un SavedPoint a un SavedPointEntity
    public static SavedPointEntity fromSavedPoint(SavedPoint savedPoint) {
        return new SavedPointEntity(
                savedPoint.getLocation().latitude,
                savedPoint.getLocation().longitude,
                savedPoint.getName(),
                savedPoint.getDate(),
                savedPoint.getCode(),
                savedPoint.getPhotoPath()
        );
    }
}