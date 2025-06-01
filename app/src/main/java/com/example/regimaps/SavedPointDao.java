package com.example.regimaps;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Interfície DAO per gestionar l'accés a la taula de punts guardats (saved_points).
 * Proporciona mètodes per inserir, obtenir i eliminar punts de la base de dades.
 */
@Dao
public interface SavedPointDao {

    // Insereix un nou punt guardat a la base de dades.
    @Insert
    void insert(SavedPointEntity savedPointEntity);

    //Retorna una llista amb tots els punts guardats a la base de dades.
    @Query("SELECT * FROM saved_points")
    List<SavedPointEntity> getAllSavedPoints();

    // Elimina un punt específic de la base de dades.
    @Delete
    void delete(SavedPointEntity savedPointEntity);

    //Elimina tots els punts guardats de la base de dades.
    @Query("DELETE FROM saved_points")
    void deleteAll();


}