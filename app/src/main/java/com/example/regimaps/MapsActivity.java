package com.example.regimaps;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.regimaps.databinding.ActivityMapsBinding; // Importa la classe generada per View Binding
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activitat principal que mostra un mapa de Google Maps i permet als usuaris
 * interactuar-hi per veure la seva ubicació, afegir punts i dibuixar línies i polígons i guardar-los a la base de dades.
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener,GoogleMap.OnPolylineClickListener,
        GoogleMap.OnPolygonClickListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1; // Codi de sol·licitud per a permisos d'ubicació
    private GoogleMap mMap; // Objecte GoogleMap per interactuar amb el mapa
    private ActivityMapsBinding binding; // ViewBinding per accedir als elements de la vista
    private FusedLocationProviderClient fusedLocationClient; // Client per obtenir la ubicació del dispositiu
    // Base de dades i DAOs
    private AppDatabase db;
    private SavedPointDao savedPointDao;
    private SavedPolygonDao savedPolygonDao;

    private ExecutorService executorService; // ExecutorService per realitzar operacions en segon pla

    // Modes de dibuix
    private enum DrawingMode { LINE, POLYGON, NONE }
    private DrawingMode currentDrawingMode = DrawingMode.NONE;


    // Llistes i referències per al dibuix i edició de formes
    private List<Marker> editingVertices = new ArrayList<>(); // Marcadors d'edició
    private Polyline tempPolyline; // Línia temporal durant el dibuix
    private Polygon tempPolygon; // Polígon temporal durant el dibuix
    private List<LatLng> finalShapePoints = new ArrayList<>(); // Punts de la forma final
    private Polygon currentPolygon = null; // Referència al polígon actual (si cal)

    /**
     * Inicialitza l'activitat, la vista, el mapa i els components principals.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Infla la vista amb View Binding
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obté el fragment del mapa i configura el callback
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Inicialitza serveis i DAOs
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = AppDatabase.getDatabase(this);
        savedPointDao = db.savedPointDao();
        savedPolygonDao = db.savedPolygonDao();
        executorService = Executors.newSingleThreadExecutor(); // Inicialitzar ExecutorService per a operacions en segon pla

        // Configura els listeners dels botons de la UI
        binding.btnObtenirUbicacio.setOnClickListener(v -> obtenirUbicacioActual()); // Obtenir i mostrar la ubicació actual
        binding.btnAfegirPunt.setOnClickListener(v -> activarModeAfegirPunt()); // Activar el modus per afegir un punt
        binding.btnDibuixarPoligons.setOnClickListener(v -> toggleDibuixMode()); // Alternar entre els modus de dibuix de línia i polígon
        binding.btnEsborrarTot.setOnClickListener(v -> esborrarTot()); // Esborrar tots els punts i formes del mapa i la base de dades
        binding.btnTornarMenu.setOnClickListener(v -> finish()); // Tornar al menú principal
    }
    /**
     * Es crida quan el mapa està llest. Configura listeners i carrega dades inicials.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap; // Assignar l'objecte GoogleMap
        mMap.setOnMapClickListener(this); // Establir el listener per als clics al mapa
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE); // Establir el tipus de mapa a Satèl·lit
        mMap.setOnPolylineClickListener(this); // Establir el listener per als clics a les línies
        mMap.setOnPolygonClickListener(this); // Establir el listener per als clics als polígons

        // Configura la UI del mapa
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // Processa dades passades per Intent (per mostrar punts o polígons guardats)
        processIntentData();

        // Configura permisos d'ubicació
        setupLocationPermission();

        // Listener per a clics sobre marcadors d'edició
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (currentDrawingMode != DrawingMode.NONE && editingVertices.contains(marker)) {
                    mostrarOpcionsVertex(marker);
                    return true;
                }
                return false;
            }
        });
    }
    /**
     * Processa dades rebudes per Intent per mostrar punts o polígons guardats al mapa.
     */
    private void processIntentData() {
        double lat = getIntent().getDoubleExtra("latitude", Double.NaN);
        double lng = getIntent().getDoubleExtra("longitude", Double.NaN);
        String pointName = getIntent().getStringExtra("name");

        String polygonPointsStr = getIntent().getStringExtra("polygon_points");
        String polygonType = getIntent().getStringExtra("polygon_type");
        String polygonInfo = getIntent().getStringExtra("polygon_info");

        // Mostra un punt si s'han passat coordenades
        if (!Double.isNaN(lat) && !Double.isNaN(lng)) {
            LatLng point = new LatLng(lat, lng);
            mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title(pointName != null ? pointName : "Punt guardat"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 17));
        }
        // Mostra una línia o polígon si s'han passat punts
        else if (polygonPointsStr != null && !polygonPointsStr.isEmpty()) {
            List<LatLng> points = new ArrayList<>();
            String[] coords = polygonPointsStr.split(";");
            for (String coord : coords) {
                String[] latLng = coord.split(",");
                if (latLng.length == 2) {
                    try {
                        double pointLat = Double.parseDouble(latLng[0]);
                        double pointLng = Double.parseDouble(latLng[1]);
                        points.add(new LatLng(pointLat, pointLng));
                    } catch (NumberFormatException e) {
                        Log.e("MapsActivity", "Error parsing coordinates: " + e.getMessage());
                    }
                }
            }
            if (polygonType != null) {
                if (polygonType.equals("poligon") && points.size() >= 3) {
                    PolygonOptions polygonOptions = new PolygonOptions()
                            .addAll(points)
                            .strokeColor(Color.BLUE)
                            .fillColor(Color.argb(70, 0, 0, 255));
                    Polygon polygon = mMap.addPolygon(polygonOptions);
                    polygon.setClickable(true);
                    String info = polygonInfo != null ? polygonInfo : (pointName != null ? pointName : "Polígon guardat");
                    polygon.setTag(info);
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng point : points) builder.include(point);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
                } else if (polygonType.equals("linia") && points.size() >= 2) {
                    PolylineOptions lineOptions = new PolylineOptions()
                            .addAll(points)
                            .color(Color.RED)
                            .width(5);
                    Polyline polyline = mMap.addPolyline(lineOptions);
                    polyline.setClickable(true);
                    String info = polygonInfo != null ? polygonInfo : (pointName != null ? pointName : "Línia guardada");
                    polyline.setTag(info);
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (LatLng point : points) builder.include(point);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
                }
            }
        }
        setupLocationPermission();
    }

    /**
     * Configura els permisos d'ubicació. Si els permisos estan concedits, activa la capa d'ubicació al mapa.
     * Si no, sol·licita els permisos a l'usuari.
     */
    private void setupLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Gestiona la resposta de l'usuari a la sol·licitud de permisos d'ubicació.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "Permís de localització denegat", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Obté la ubicació actual del dispositiu i centra el mapa en aquesta ubicació.
     */
    private void obtenirUbicacioActual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                } else {
                    Toast.makeText(this, "No s'ha pogut obtenir la ubicació actual.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    /**
     * Activa el mode d'afegir punts si no s'està dibuixant una línia o polígon.
     */
    private void activarModeAfegirPunt() {
        if (currentDrawingMode != DrawingMode.NONE) {
            Toast.makeText(this, "Finalitza el modus de dibuix abans d'afegir un punt.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Clica al mapa per afegir un punt.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Mostra un diàleg per seleccionar el mode de dibuix (línia o polígon).
     */
    private void toggleDibuixMode() {
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar modus de dibuix")
                .setItems(new String[]{"Línia", "Polígon"}, (dialog, which) -> {
                    currentDrawingMode = which == 0 ? DrawingMode.LINE : DrawingMode.POLYGON;
                    iniciarModeDibuix();
                })
                .setNegativeButton("Cancel·lar", (dialog, which) -> {
                    currentDrawingMode = DrawingMode.NONE;
                    netejarDibuix();
                })
                .show();
    }

    /**
     * Inicia el mode de dibuix, netejant l'estat anterior i informant l'usuari.
     */
    private void iniciarModeDibuix() {
        clearEditingMarkers();
        finalShapePoints.clear();
        if (currentDrawingMode == DrawingMode.LINE) {
            Toast.makeText(this, "Mode línia activat - Clica per afegir punts", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Mode polígon activat - Clica per afegir punts", Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Mètode onMapClick: es crida quan l'usuari fa clic al mapa.
     * Si no hi ha cap modus de dibuix actiu, mostra el diàleg per afegir un punt normal.
     * Si hi ha un modus de dibuix actiu, afegeix un vèrtex a la forma que s'està dibuixant.
     */
    @Override
    public void onMapClick(LatLng latLng) {
        if (currentDrawingMode == DrawingMode.NONE) {
            showAddPointDialog(latLng);
            return;
        }
        addVertexAndDraw(latLng);
    }
    /**
     * Es crida quan l'usuari fa clic sobre una línia.
     * Mostra un diàleg amb la informació de la línia.
     */
    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {
        String info = polyline.getTag() != null ? polyline.getTag().toString() : "Línia";
        new AlertDialog.Builder(this)
                .setTitle("Informació de la línia")
                .setMessage(info)
                .setPositiveButton("Tancar", null)
                .show();
    }

    /**
     * Es crida quan l'usuari fa clic sobre un polígon.
     * Mostra un diàleg amb la informació del polígon.
     */
    @Override
    public void onPolygonClick(Polygon polygon) {
        String info = polygon.getTag() != null ? polygon.getTag().toString() : "Polígon";
        new AlertDialog.Builder(this)
                .setTitle("Informació del polígon")
                .setMessage(info)
                .setPositiveButton("Tancar", null)
                .show();
    }

    /**
     * Afegeix un vèrtex a la forma que s'està dibuixant i actualitza la visualització.
     */
    private void addVertexAndDraw(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng);
        Marker marker = mMap.addMarker(markerOptions);
        editingVertices.add(marker);
        finalShapePoints.add(latLng);
        updateShape(null);

        // Mostra diàleg per finalitzar si s'ha arribat al mínim de punts
        if (currentDrawingMode == DrawingMode.LINE && finalShapePoints.size() >= 2 ||
                currentDrawingMode == DrawingMode.POLYGON && finalShapePoints.size() >= 3) {
            mostrarOpcionsFinalitzat();
        }
    }

    /**
     * Actualitza la forma (línia o polígon) que s'està dibuixant al mapa.
     */
    private void updateShape(Marker draggedMarker) {
        List<LatLng> updatedPoints = getMarkerPositions();
        if (currentDrawingMode == DrawingMode.LINE) {
            actualitzarLiniaTemporal(updatedPoints);
        } else if (currentDrawingMode == DrawingMode.POLYGON) {
            actualitzarPoligonTemporal(updatedPoints);
        }
    }

    /**
     * Obté la llista de coordenades dels marcadors d'edició actuals.
     */
    private List<LatLng> getMarkerPositions() {
        List<LatLng> points = new ArrayList<>();
        for (Marker marker : editingVertices) {
            points.add(marker.getPosition());
        }
        return points;
    }

    /**
     * Actualitza la línia temporal durant el dibuix.
     */
    private void actualitzarLiniaTemporal(List<LatLng> points) {
        if (tempPolyline != null) tempPolyline.remove();
        PolylineOptions options = new PolylineOptions()
                .addAll(points)
                .color(Color.RED)
                .width(5);
        tempPolyline = mMap.addPolyline(options);
    }

    /**
     * Actualitza el polígon temporal durant el dibuix.
     */
    private void actualitzarPoligonTemporal(List<LatLng> points) {
        if (tempPolygon != null) tempPolygon.remove();
        PolygonOptions options = new PolygonOptions()
                .addAll(points)
                .strokeColor(Color.RED)
                .fillColor(Color.argb(50, 255, 0, 0))
                .strokeWidth(5);
        tempPolygon = mMap.addPolygon(options);
    }

    /**
     * Mostra un diàleg per confirmar la finalització del dibuix.
     */
    private void mostrarOpcionsFinalitzat() {
        String missatge = currentDrawingMode == DrawingMode.LINE ?
                "Vols finalitzar la línia?" : "Vols finalitzar el polígon?";

        new AlertDialog.Builder(this)
                .setTitle("Finalitzar dibuix")
                .setMessage(missatge)
                .setPositiveButton("Finalitzar", (dialog, which) -> finalitzarDibuix())
                .setNegativeButton("Continuar", null)
                .show();
    }

    /**
     * Finalitza el dibuix i guarda la línia o polígon a la base de dades.
            */
    private void finalitzarDibuix() {
        if (currentDrawingMode == DrawingMode.LINE && finalShapePoints.size() >= 2) {
            double distancia = SphericalUtil.computeLength(finalShapePoints);
            showSaveLineDialog(finalShapePoints, distancia);
        } else if (currentDrawingMode == DrawingMode.POLYGON && finalShapePoints.size() >= 3) {
            double area = SphericalUtil.computeArea(finalShapePoints);
            showSavePolygonDialog(finalShapePoints, area);
        }
        else {
            netejarDibuix();
        }
    }

    /**
     * Mostra un diàleg per guardar un polígon amb nom.
     */
    private void showSavePolygonDialog(List<LatLng> points, double area) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guardar Polígon");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_polygon_name, null);
        builder.setView(dialogView);
        EditText polygonNameInput = dialogView.findViewById(R.id.polygonNameInput);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String name = polygonNameInput.getText().toString();
            if (name.isEmpty()) {
                Toast.makeText(this, "El nom del polígon no pot estar buit", Toast.LENGTH_SHORT).show();
                netejarDibuix();
            } else {
                guardarPoligon(points, area, name);
                //netejarDibuix();
            }
        });
        builder.setNegativeButton("Cancel·lar", (dialog, which) -> {
            netejarDibuix();
            dialog.cancel();
        });
        builder.show();
    }

    /**
     * Mostra un diàleg per guardar una línia amb nom.
     */
    private void showSaveLineDialog(List<LatLng> points, double distancia) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guardar Línia");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_polygon_name, null);
        builder.setView(dialogView);
        EditText lineNameInput = dialogView.findViewById(R.id.polygonNameInput);
        lineNameInput.setHint("Nom de la línia");
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String name = lineNameInput.getText().toString();
            if (name.isEmpty()) {
                Toast.makeText(this, "El nom de la línia no pot estar buit", Toast.LENGTH_SHORT).show();
                netejarDibuix();
            } else {
                guardarLinia(points, distancia, name);
               // netejarDibuix();
            }
        });
        builder.setNegativeButton("Cancel·lar", (dialog, which) -> {
            netejarDibuix();
            dialog.cancel();
        });
        builder.show();
    }

    /**
     * Neteja l'estat del dibuix, eliminant marcadors, formes temporals i punts.
     */
    private void netejarDibuix() {
        clearEditingMarkers();
        if (tempPolygon != null) tempPolygon.remove();
        if (tempPolyline != null) tempPolyline.remove();
        finalShapePoints.clear();
        currentDrawingMode = DrawingMode.NONE;
    }

    /**
     * Elimina tots els marcadors d'edició del mapa i de la llista.
     */
    private void clearEditingMarkers() {
        for (Marker marker : editingVertices) {
            marker.remove();
        }
        editingVertices.clear();
    }

    /**
     * Desa un polígon a la base de dades.
     */
    private void guardarPoligon(List<LatLng> points, double area, String name) {
        executorService.execute(() -> {
            try {
                SavedPolygonEntity entity = new SavedPolygonEntity();
                entity.tipus = "poligon";
                entity.area = area;
                entity.name = name;
                StringBuilder sb = new StringBuilder();
                for (LatLng point : points) {
                    sb.append(point.latitude).append(",").append(point.longitude).append(";");
                }
                entity.points = sb.toString();
                savedPolygonDao.insert(entity);
                runOnUiThread(() ->
                        Toast.makeText(this, "Polígon guardat com a '" + name + "'!", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error guardant polígon: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    /**
     * Desa una línia a la base de dades i la mostra al mapa.
     */
    private void guardarLinia(List<LatLng> points, double distancia, String name) {
        executorService.execute(() -> {
            try {
                SavedPolygonEntity entity = new SavedPolygonEntity();
                entity.tipus = "linia";
                entity.name = name;
                StringBuilder sb = new StringBuilder();
                for (LatLng point : points) {
                    sb.append(point.latitude).append(",").append(point.longitude).append(";");
                }
                entity.points = sb.toString();
                entity.distancia = distancia;
                entity.area = 0.0;
                savedPolygonDao.insert(entity);
                runOnUiThread(() -> {
                    PolylineOptions options = new PolylineOptions()
                            .addAll(points)
                            .color(Color.RED)
                            .width(5);
                    Polyline polyline = mMap.addPolyline(options);
                    polyline.setClickable(true);
                    polyline.setTag(name + " - " + new DecimalFormat("#.##").format(distancia) + " m");
                    Toast.makeText(this, "Línia guardada com a '" + name + "'!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                netejarDibuix();
            }
        });
    }

    /**
     * Mostra un diàleg per afegir un punt individual al mapa i guardar-lo.
     */
    private void showAddPointDialog(LatLng point) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Afegir Punt");
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_point, null);
        builder.setView(dialogView);
        EditText inputName = dialogView.findViewById(R.id.inputName);
        EditText inputDate = dialogView.findViewById(R.id.inputDate);
        EditText inputCode = dialogView.findViewById(R.id.inputCode);
        String currentDate = java.text.DateFormat.getDateInstance().format(new java.util.Date());
        inputDate.setText(currentDate);
        inputDate.setEnabled(false);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String name = inputName.getText().toString();
            String code = inputCode.getText().toString();
            if (!name.isEmpty()) {
                executorService.execute(() -> {
                    try {
                        SavedPointEntity savedPointEntity = new SavedPointEntity(
                                point.latitude,
                                point.longitude,
                                name,
                                currentDate,
                                code,
                                ""
                        );
                        savedPointDao.insert(savedPointEntity);
                        runOnUiThread(() -> {
                            mMap.addMarker(new MarkerOptions()
                                    .position(point)
                                    .title(name));
                            Toast.makeText(this, "Punt afegit correctament", Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() ->
                                Toast.makeText(this, "Error guardant punt: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    }
                });
            } else {
                Toast.makeText(this, "El nom és obligatori", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel·lar", null);
        builder.show();
    }

    /**
     * Esborra tots els punts i polígons del mapa i de la base de dades.
     */
    private void esborrarTot() {
        mMap.clear();
        clearEditingMarkers();
        finalShapePoints.clear();
       // executorService.execute(() -> savedPointDao.deleteAll());
        if (tempPolygon != null) tempPolygon.remove();
        if (tempPolyline != null) tempPolyline.remove();
        Toast.makeText(this, "Mapa netejat.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Mostra un diàleg amb opcions per a un vèrtex (marcador d'edició).
     */
    private void mostrarOpcionsVertex(final Marker marker) {
        new AlertDialog.Builder(this)
                .setTitle("Opcions de Vèrtex")
                .setItems(new String[]{"Eliminar"}, (dialog, which) -> {
                    if (which == 0) {
                        eliminarVertex(marker);
                    }
                })
                .setNegativeButton("Cancel·lar", null)
                .show();
    }

    /**
     * Elimina un vèrtex (marcador) del mapa i de les llistes, i actualitza la forma.
     */
    private void eliminarVertex(Marker marker) {
        int index = editingVertices.indexOf(marker); // Obtenir l'índex del marcador a la llista de vèrtexs
        if (index != -1) {
            editingVertices.remove(index);
            finalShapePoints.remove(index);
            marker.remove();
            updateShape(null);
        }
    }
}

