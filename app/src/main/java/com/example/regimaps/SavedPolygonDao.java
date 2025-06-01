package com.example.regimaps;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

/**
 * DAO per gestionar l'accés a la taula de polígons guardats.
 * Proporciona mètodes per inserir, obtenir i eliminar polígons de la base de dades.
 */
@Dao
public interface SavedPolygonDao {

    // Insereix un nou polígon guardat a la base de dades.
    @Insert
    void insert(SavedPolygonEntity polygon);

    // Retorna una llista amb tots els polígons guardats a la base de dades.
    @Query("SELECT * FROM saved_polygons")
    List<SavedPolygonEntity> getAllSavedPolygons();

    // Elimina un polígon específic de la base de dades.
    @Delete
    void delete(SavedPolygonEntity polygon);

    // Elimina tots els polígons guardats de la base de dades.
    @Query("DELETE FROM saved_polygons")
    void deleteAll();
}