package com.example.regimaps;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entitat Room que representa un polígon o línia guardada a la base de dades.
 * Emmagatzema la informació essencial per a la reconstrucció i identificació d'un polígon o línia.
 */
@Entity(tableName = "saved_polygons")
public class SavedPolygonEntity {
    // Identificador únic autogenerat per Room.
    @PrimaryKey(autoGenerate = true)
    public int id;

    // Punts del polígon o línia, codificats com a String.
    @ColumnInfo(name = "points")
    public String points;

    // Àrea del polígon (en metres quadrats).
    @ColumnInfo(name = "area")
    public double area;

    // Tipus de figura: pot ser "linia" o "poligon".
    @ColumnInfo(name = "tipus")
    public String tipus; // "linia" o "poligon"

    // Distància total de la línia (en metres). Només aplicable si tipus és "linia".
    @ColumnInfo(name = "distancia")
    public double distancia;

    // Nom assignat pel l'usuari al polígon o línia.
    @ColumnInfo(name = "name")
    public String name;

    // Constructor buit necessari per a Room.
    public SavedPolygonEntity() {}

    // Setters
    public void setArea(double area) {
        this.area = area;
    }
    public void setPoints(String points) { this.points = points; }
    public void setTipus(String tipus) { this.tipus = tipus; }
    public void setDistancia(double distancia) { this.distancia = distancia;
    }
}