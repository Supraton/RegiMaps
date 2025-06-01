package com.example.regimaps;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adaptador per mostrar una llista de punts guardats en un RecyclerView.
 * Gestiona la visualització i la interacció amb cada element de la llista.
 */
public class SavedPointsAdapter extends RecyclerView.Adapter<SavedPointsAdapter.ViewHolder> {

    private List<SavedPointEntity> points; // Llista de punts guardats
    private Context context; // Context per iniciar intents

    // Constructor de l'adaptador.
    public SavedPointsAdapter(List<SavedPointEntity> points, Context context) {
        this.points = points;
        this.context = context;
    }

    // Actualitza la llista de punts i refresca la vista.
    public void updateData(List<SavedPointEntity> newPoints) {
        this.points = newPoints;
        notifyDataSetChanged();
    }

    // Retorna el punt guardat en una posició concreta.
    public SavedPointEntity getPointAtPosition(int position) {
        return points.get(position);
    }

    // Elimina un element de la llista i actualitza la vista.
    public void removeItem(int position) {
        points.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SavedPointEntity point = points.get(position);
        // Mostra el nom del punt
        holder.nameText.setText(point.name);
        // Mostra les coordenades amb format
        holder.coordinatesText.setText(String.format("Lat: %.5f, Lng: %.5f", point.latitude, point.longitude));

        // Obre el MapsActivity mostrant el punt seleccionat quan es fa clic a l'element
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MapsActivity.class);
            intent.putExtra("latitude", point.latitude);
            intent.putExtra("longitude", point.longitude);
            intent.putExtra("name", point.name);
            context.startActivity(intent);
        });
    }
    @Override
    public int getItemCount() {
        return points.size();
    }

    // ViewHolder que manté les referències als TextView de cada element de la llista.
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, coordinatesText;
        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(android.R.id.text1);
            coordinatesText = itemView.findViewById(android.R.id.text2);
        }
    }
}
