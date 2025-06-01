package com.example.regimaps;

import android.content.Intent;


import androidx.appcompat.app.AlertDialog;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Activitat que mostra la llista de polígons o línies guardades per l'usuari.
 * Permet visualitzar, exportar o eliminar cada element.
 */
public class PolygonsListActivity extends BaseListActivity<SavedPolygonEntity> {
    private SavedPolygonDao dao;

    /**
     * Retorna el layout associat a aquesta activitat.
     */
    @Override
    protected int getLayoutResource() { return R.layout.activity_polygons_list; }

    /**
     * Retorna l'identificador del ListView on es mostrarà la llista d'elements.
     */
    @Override
    protected int getListViewId() { return R.id.polygonsList; }

    /**
     * Carrega els polígons guardats de la base de dades de manera asíncrona.
     * Un cop recuperats, actualitza la llista a la UI.
     */
    @Override
    protected void loadItems() {
        dao = db.savedPolygonDao();
        executorService.execute(() -> {
            List<SavedPolygonEntity> polygons = dao.getAllSavedPolygons();
            updateList(polygons);
        });
    }

    /**
     * Dona format a les dades dels polígons per mostrar-les a la llista.
     * Mostra el nom, el tipus (Polígon/Línia) i l'àrea o distància segons correspongui.
     */
    @Override
    protected List<String> formatData(List<SavedPolygonEntity> items) {
        DecimalFormat df = new DecimalFormat("#.##");
        List<String> formatted = new ArrayList<>();
        for (SavedPolygonEntity p : items) {
            String name = (p.name != null && !p.name.isEmpty()) ? p.name : "Sense nom"; // Utilitzem el nom o "Sense nom" si està buit
            String typeInfo = p.tipus.substring(0, 1).toUpperCase() + p.tipus.substring(1); // Capitalitzem el tipus: "Poligon" o "Linia"

            String details;
            if (p.tipus.equals("poligon")) {
                details = "Àrea: " + df.format(p.area) + " m²";
            } else {
                details = "Distància: " + df.format(p.distancia) + " m";
            }
            // Afegim la cadena formatada a la llista
            formatted.add(name + " (" + typeInfo + ") - " + details);
        }
        return formatted;
    }
    /**
     * Mostra un diàleg amb les opcions disponibles per a un element de la llista:
     * veure al mapa, exportar a KML o eliminar.
     */
    @Override
    protected void showOptionsDialog(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Opcions del polígon")
                .setPositiveButton("Veure al mapa", (d, w) -> viewPolygon(position))
                .setNeutralButton("Exportar a KML", (d, w) -> exportToKML(items))
                .setNegativeButton("Eliminar", (d, w) -> deleteItem(position))
                .show();
    }
   /* @Override
    protected String generateKMLContent(List<SavedPolygonEntity> polygons) {
        StringBuilder kml = new StringBuilder();
        kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        kml.append("<Document>\n");
        kml.append("<name>Polígons Guardats</name>\n");

        for (SavedPolygonEntity polygon : polygons) {
            kml.append("<Placemark>\n");
            kml.append("<name>").append(polygon.tipus).append("</name>\n");
            kml.append("<description>");
            if (polygon.tipus.equals("poligon")) {
                kml.append("Àrea: ").append(polygon.area).append(" m²");
            } else {
                kml.append("Distància: ").append(polygon.distancia).append(" m");
            }
            kml.append("</description>\n");

            String[] coords = polygon.points.split(";");
            if (polygon.tipus.equals("poligon")) {
                kml.append("<Polygon><outerBoundaryIs><LinearRing><coordinates>\n");
                for (String coord : coords) {
                    String[] parts = coord.split(",");
                    if (parts.length == 2) {
                        kml.append(parts[1]).append(",").append(parts[0]).append(" ");
                    }
                }
                kml.append("\n</coordinates></LinearRing></outerBoundaryIs></Polygon>\n");
            } else { // linia
                kml.append("<LineString><coordinates>\n");
                for (String coord : coords) {
                    String[] parts = coord.split(",");
                    if (parts.length == 2) {
                        kml.append(parts[1]).append(",").append(parts[0]).append(" ");
                    }
                }
                kml.append("\n</coordinates></LineString>\n");
            }
            kml.append("</Placemark>\n");
        }

        kml.append("</Document>\n");
        kml.append("</kml>\n");
        return kml.toString();
    }*/

    /**
     * Obre l'activitat del mapa i mostra el polígon o línia seleccionat.
     */

    private void viewPolygon(int position) {
        SavedPolygonEntity polygon = items.get(position);
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("polygon_points", polygon.points);
        intent.putExtra("polygon_type", polygon.tipus); // "poligon" o "linia"
        if (polygon.tipus.equals("poligon")) {
            intent.putExtra("polygon_info", polygon.name + " - Àrea: " + polygon.area + " m²");
        } else {
            intent.putExtra("polygon_info", polygon.name + " - Distància: " + polygon.distancia + " m");
        }
        startActivity(intent);

    }

    /**
     * Elimina l'element seleccionat de la base de dades i actualitza la llista.
     */
    private void deleteItem(int position) {
        executorService.execute(() -> {
            dao.delete(items.get(position));
            loadItems(); // Actualitza la llista després d'eliminar
        });
    }
}