package com.example.regimaps;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Classe principal de la base de dades Room per l'aplicació.
 * Defineix les entitats i la versió de la base de dades.
 */
@Database(
        entities = {SavedPointEntity.class, SavedPolygonEntity.class},
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    //Retorna l'objecte DAO per gestionar els punts guardats.
    public abstract SavedPointDao savedPointDao();

    //Retorna l'objecte DAO per gestionar els polígons guardats.
    public abstract SavedPolygonDao savedPolygonDao();

    // Instància per a la base de dades.
    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
        // Crea la base de dades amb el nom "app_database" i permet migracions destructives si cal.

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "app_database"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
