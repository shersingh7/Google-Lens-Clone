package com.dv.googlelens.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dv.googlelens.R;
import com.dv.googlelens.data.HistoryItem;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<HistoryItem> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    public HistoryAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<HistoryItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        holder.query.setText(item.getQuery());
        holder.mode.setText(item.getMode().toUpperCase(Locale.getDefault()));
        holder.time.setText(dateFormat.format(new Date(item.getTimestamp())));

        if (item.getImagePath() != null && !item.getImagePath().isEmpty()) {
            File imgFile = new File(item.getImagePath());
            if (imgFile.exists()) {
                Glide.with(context).load(imgFile).placeholder(R.drawable.rounded_image_bg).into(holder.image);
            } else {
                holder.image.setImageResource(R.drawable.rounded_image_bg);
            }
        } else {
            holder.image.setImageResource(R.drawable.rounded_image_bg);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView query, mode, time;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.historyImage);
            query = itemView.findViewById(R.id.historyQuery);
            mode = itemView.findViewById(R.id.historyMode);
            time = itemView.findViewById(R.id.historyTime);
        }
    }
}
