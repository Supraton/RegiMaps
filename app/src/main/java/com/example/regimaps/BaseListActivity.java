package com.example.regimaps;

import android.content.ContextWrapper;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;

/**
 * Classe base abstracta per activitats que mostren una llista d'elements.
 * Proporciona funcionalitats comunes com la gestió de la base de dades,
 * l'exportació a KML i la gestió de permisos.
 */
public abstract class BaseListActivity<T> extends AppCompatActivity {
    // Components de la UI i dades bàsiques.
    protected ListView listView;
    protected AppDatabase db;
    protected ExecutorService executorService;
    protected ArrayAdapter<String> adapter;
    protected List<T> items = new ArrayList<>();
    protected String kmlFileName = "map_data.kml";
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResource());

        listView = findViewById(getListViewId());
        db = AppDatabase.getDatabase(this);
        executorService = Executors.newSingleThreadExecutor();

        setupListViewClickListener();
        loadItems();
    }

    //Retorna el recurs de layout que ha d'utilitzar l'activitat.
    protected abstract int getLayoutResource();
    // Retorna l'identificador del ListView.
    protected abstract int getListViewId();
    //Carrega els elements que s'han de mostrar a la llista.
    protected abstract void loadItems();
    //Dona format a la llista d'elements per mostrar-la a la UI.
    protected abstract List<String> formatData(List<T> items);
    //Mostra el diàleg d'opcions per a l'element seleccionat.
    protected abstract void showOptionsDialog(int position);
    //Configura el listener per gestionar els clics a la llista.
    private void setupListViewClickListener() {
        listView.setOnItemClickListener((parent, view, position, id) -> showOptionsDialog(position));
    }
    //Actualitza la llista d'elements i refresca l'adaptador de la UI.
    protected void updateList(List<T> newItems) {
        items = newItems;
        runOnUiThread(() -> {
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, formatData(items));
            listView.setAdapter(adapter);
        });
    }

    //  Funció per demanar el permís d'escriptura a l'emmagatzematge extern si no està concedit.
    protected void requestWriteExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permís concedit", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permís denegat", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /**
     * Mostra un diàleg per exportar la llista d'elements a un fitxer KML.
     * Permet crear un fitxer nou o afegir a un fitxer existent.
     */
    protected void exportToKML(List<T> items) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exportar a KML");
        builder.setItems(new CharSequence[]{"Crear fitxer nou", "Afegir a fitxer existent"}, (dialog, which) -> {
            if (which == 0) {
                // Crear fitxer nou
                showFileNameDialog(items, false);
            } else {
                // Afegir a fitxer existent
                showKmlFilesDialog(items);
            }
        });
        builder.show();
    }
    /**
     * Mostra un diàleg per introduir el nom del fitxer KML.
     */
    private void showFileNameDialog(List<T> items, boolean append) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nom del fitxer KML");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Desa", (dialog, which) -> {
            String fileName = input.getText().toString();
            if (!fileName.endsWith(".kml")) {
                fileName += ".kml";
            }
            exportToKMLFile(items, fileName, append);
        });
        builder.setNegativeButton("Cancel·la", (dialog, which) -> dialog.cancel());

        builder.show();
    }
    /**
     * Mostra un diàleg per seleccionar un fitxer KML existent.
     */
    private void showKmlFilesDialog(List<T> items) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File documentsDir = cw.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        if (documentsDir == null) {
            Toast.makeText(this, "No s'ha pogut accedir al directori de documents", Toast.LENGTH_SHORT).show();
            return;
        }

        File[] kmlFiles = documentsDir.listFiles((dir, name) -> name.endsWith(".kml"));
        if (kmlFiles == null || kmlFiles.length == 0) {
            Toast.makeText(this, "No hi ha fitxers KML disponibles. Es crearà un de nou.", Toast.LENGTH_SHORT).show();
            showFileNameDialog(items, false); // Procedir com si fos un fitxer nou
            return;
        }

        String[] fileNames = new String[kmlFiles.length];
        for (int i = 0; i < kmlFiles.length; i++) {
            fileNames[i] = kmlFiles[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecciona un fitxer KML");
        builder.setItems(fileNames, (dialog, which) -> {
            exportToKMLFile(items, fileNames[which], true);
        });
        builder.setNegativeButton("Cancel·la", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    /**
     * Exporta els elements a un fitxer KML, gestionant la creació o l'addició segons el paràmetre 'append'.
     * Inclou una comprovació bàsica de duplicats.
     */
    private void exportToKMLFile(List<T> items, String fileName, boolean append) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File documentsDir = cw.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        if (documentsDir == null) {
            Toast.makeText(this, "No s'ha pogut accedir al directori de documents", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(documentsDir, fileName);
        String closingTags = "</Document>\n</kml>";

        try {
            if (!append) {
                // Crea un fitxer nou amb la capçalera i el contingut.
                String allPlacemarkContent = generateKMLContent(items); // Genera tots els placemarks
                try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
                    outputStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
                    outputStream.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n".getBytes("UTF-8"));
                    outputStream.write("<Document>\n".getBytes("UTF-8"));
                    outputStream.write(("<name>" + getKMLDocumentName() + "</name>\n").getBytes("UTF-8"));
                    outputStream.write(allPlacemarkContent.getBytes("UTF-8"));
                    outputStream.write("</Document>\n".getBytes("UTF-8"));
                    outputStream.write("</kml>\n".getBytes("UTF-8"));
                }
            } else {
                // Comprova duplicats i afegeix només els nous elements.
                StringBuilder existingContentBuilder = new StringBuilder();
                if (file.exists()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            existingContentBuilder.append(line).append("\n");
                        }
                    }
                }
                String existingContent = existingContentBuilder.toString();

                List<T> itemsToAppend = new ArrayList<>();
                DecimalFormat df = new DecimalFormat("#.##");

                for (T item : items) {
                    boolean isDuplicate = false;
                    String itemKey = null;
                    if (item instanceof SavedPointEntity) {
                        SavedPointEntity point = (SavedPointEntity) item;
                        itemKey = point.name + "_" + point.longitude + "," + point.latitude;
                    } else if (item instanceof SavedPolygonEntity) {
                        SavedPolygonEntity polygon = (SavedPolygonEntity) item;
                        String info = polygon.tipus.equals("poligon") ?
                                "Àrea: " + df.format(polygon.area) + " m²" :
                                "Distància: " + df.format(polygon.distancia) + " m";
                        itemKey = (polygon.name != null && !polygon.name.isEmpty() ? polygon.name : polygon.tipus)
                                + "_" + info + "_" + getPolygonCoordinateStringForKml(polygon.points);
                    }
                    // Comprovació bàsica de duplicats pel nom
                    if (itemKey != null && existingContent.contains("<name>" +
                            (item instanceof SavedPointEntity ? ((SavedPointEntity)item).name :
                                    ((SavedPolygonEntity)item).name != null && !((SavedPolygonEntity)item).name.isEmpty() ?
                                            ((SavedPolygonEntity)item).name : ((SavedPolygonEntity)item).tipus) + "</name>")) {
                        String generatedSnippet = generateKMLContent(java.util.Collections.singletonList(item));
                        if (existingContent.contains(generatedSnippet.trim())) {
                            isDuplicate = true;
                        }
                    }
                    if (!isDuplicate) {
                        itemsToAppend.add(item);
                    }
                }
                String newPlacemarkContent = generateKMLContent(itemsToAppend);
                int insertPosition = existingContent.lastIndexOf(closingTags);

                if (insertPosition != -1) {
                    String contentBeforeClosing = existingContent.substring(0, insertPosition);
                    String updatedContent = contentBeforeClosing + newPlacemarkContent + closingTags;
                    try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
                        outputStream.write(updatedContent.getBytes("UTF-8"));
                    }
                } else {
                    try (FileOutputStream outputStream = new FileOutputStream(file, true)) {
                        outputStream.write(newPlacemarkContent.getBytes("UTF-8"));
                        outputStream.write(closingTags.getBytes("UTF-8"));
                    }
                }
            }
            Toast.makeText(this, "Exportat a " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e("ExportKML", "Error exportant: " + e.getMessage(), e);
            Toast.makeText(this, "Error exportant: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("ExportKML", "Error inesperat: " + e.getMessage(), e);
            Toast.makeText(this, "Error inesperat durant l'exportació", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Converteix una cadena de punts en el format de coordenades KML.
     */
    private String getPolygonCoordinateStringForKml(String pointsString) {
        StringBuilder sb = new StringBuilder();
        String[] coords = pointsString.split(";");
        for (String coord : coords) {
            String[] parts = coord.split(",");
            if (parts.length == 2) {
                sb.append(parts[1]).append(",").append(parts[0]).append(" ");
            }
        }
        return sb.toString().trim();
    }
    /**
     * Retorna el nom del document KML.
     * Les subclasses poden sobreescriure aquest mètode per personalitzar-lo.
     */
    protected String getKMLDocumentName() {
        return "Dades del Mapa";
    }
    /**
     * Genera el contingut KML per a una llista d'elements.
     */
    protected String generateKMLContent(List<T> itemsToExport) {
        StringBuilder kml = new StringBuilder();
        DecimalFormat df = new DecimalFormat("#.##");

        for (T item : itemsToExport) {
            if (item instanceof SavedPointEntity) {
                SavedPointEntity point = (SavedPointEntity) item;
                kml.append("<Placemark>\n");
                kml.append("<name>").append(point.name).append("</name>\n");
                kml.append("<description>").append("Data: ").append(point.date).append(", Codi: ").append(point.code).append("</description>\n");
                kml.append("<Point>\n");
                kml.append("<coordinates>").append(point.longitude).append(",").append(point.latitude).append("</coordinates>\n");
                kml.append("</Point>\n");
                kml.append("</Placemark>\n");
            } else if (item instanceof SavedPolygonEntity) {
                SavedPolygonEntity polygon = (SavedPolygonEntity) item;
                kml.append("<Placemark>\n");
                kml.append("<name>").append(polygon.name != null && !polygon.name.isEmpty() ? polygon.name : polygon.tipus).append("</name>\n");
                kml.append("<description>");
                if (polygon.tipus.equals("poligon")) {
                    kml.append("Àrea: ").append(df.format(polygon.area)).append(" m²");
                } else {
                    kml.append("Distància: ").append(df.format(polygon.distancia)).append(" m");
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
                } else {
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
        }
        return kml.toString();
    }
}