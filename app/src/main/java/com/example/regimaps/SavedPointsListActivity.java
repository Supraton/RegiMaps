package com.example.regimaps;

import android.content.Intent;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Activitat que mostra la llista de punts guardats.
 * Permet veure, exportar o eliminar cada punt.
 */
public class SavedPointsListActivity extends BaseListActivity<SavedPointEntity> {

    // Accés a la base de dades per als punts guardats.
    private SavedPointDao dao;
    @Override
    protected int getLayoutResource() { return R.layout.activity_saved_points_list; }
    @Override
    protected int getListViewId() { return R.id.savedPointsList; }

    /**
     * Carrega els punts guardats de la base de dades de forma asíncrona.
     * Un cop recuperats, actualitza la llista a la interfície.
     */
    @Override
    protected void loadItems() {
        dao = db.savedPointDao();
        executorService.execute(() -> {
            List<SavedPointEntity> points = dao.getAllSavedPoints();
            updateList(points);
        });
    }

    /**
     * Dona format a la llista de punts per mostrar-los a la interfície.
     * Mostra el nom i les coordenades de cada punt.
     */
    @Override
    protected List<String> formatData(List<SavedPointEntity> items) {
        List<String> formatted = new ArrayList<>();
        for (SavedPointEntity p : items) {
            formatted.add(String.format("Nom: %s - Lat: %.5f, Lng: %.5f",
                    p.name, p.latitude, p.longitude));
        }
        return formatted;
    }

    /**
     * Mostra un diàleg amb opcions per al punt seleccionat:
     * veure al mapa, exportar a KML o eliminar.
     */
    @Override
    protected void showOptionsDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Opcions del punt")
                .setPositiveButton("Veure al mapa", (d, w) -> viewPoint(position))
                .setNeutralButton("Exportar a KML", (d, w) -> exportToKML(items))
                .setNegativeButton("Eliminar", (d, w) -> deleteItem(position))
                .show();
    }

    /* @Override
     protected String generateKMLContent(List<SavedPointEntity> points) {
         StringBuilder kml = new StringBuilder();

         for (SavedPointEntity point : points) {
             kml.append("<Placemark>\n");
             kml.append("<name>").append(point.name).append("</name>\n");
             kml.append("<description>").append("Data: ").append(point.date).append(", Codi: ").append(point.code).append("</description>\n");
             kml.append("<Point>\n");
             kml.append("<coordinates>").append(point.longitude).append(",").append(point.latitude).append("</coordinates>\n");
             kml.append("</Point>\n");
             kml.append("</Placemark>\n");
         }

         return kml.toString();
     }*/

    /**
     * Obre el MapsActivity i mostra el punt seleccionat.
     */
    private void viewPoint(int position) {
        SavedPointEntity point = items.get(position);
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("latitude", point.latitude);
        intent.putExtra("longitude", point.longitude);
        startActivity(intent);
    }

    /**
     * Elimina el punt seleccionat de la base de dades i actualitza la llista.
     */
    private void deleteItem(int position) {
        executorService.execute(() -> {
            dao.delete(items.get(position));
            loadItems(); // Actualitza la llista després d'eliminar
        });
    }
}
