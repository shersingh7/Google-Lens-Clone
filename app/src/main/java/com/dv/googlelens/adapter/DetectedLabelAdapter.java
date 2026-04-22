package com.dv.googlelens.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dv.googlelens.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.mlkit.vision.label.ImageLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetectedLabelAdapter extends RecyclerView.Adapter<DetectedLabelAdapter.ViewHolder> {

    private final Context context;
    private final List<ImageLabel> labels = new ArrayList<>();
    private OnLabelClickListener listener;

    public interface OnLabelClickListener {
        void onLabelClick(String labelText);
    }

    public DetectedLabelAdapter(Context context) {
        this.context = context;
    }

    public void setOnLabelClickListener(OnLabelClickListener listener) {
        this.listener = listener;
    }

    public void setLabels(List<ImageLabel> newLabels) {
        labels.clear();
        labels.addAll(newLabels);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_detected_label, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageLabel label = labels.get(position);
        holder.name.setText(label.getText());
        int confidence = (int) (label.getConfidence() * 100);
        holder.confidence.setText(String.format(Locale.getDefault(), "%d%%", confidence));
        holder.bar.setProgress(confidence);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLabelClick(label.getText());
        });
    }

    @Override
    public int getItemCount() {
        return labels.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, confidence;
        LinearProgressIndicator bar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.labelName);
            confidence = itemView.findViewById(R.id.labelConfidence);
            bar = itemView.findViewById(R.id.confidenceBar);
        }
    }
}
