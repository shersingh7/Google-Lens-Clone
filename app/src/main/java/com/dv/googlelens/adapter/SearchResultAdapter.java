package com.dv.googlelens.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.dv.googlelens.R;
import com.dv.googlelens.SearchRVModal;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private final Context context;
    private final List<SearchRVModal> items = new ArrayList<>();

    public SearchResultAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<SearchRVModal> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchRVModal item = items.get(position);
        holder.title.setText(item.getTitle());
        holder.link.setText(item.getDisplayed_link());
        holder.snippet.setText(item.getSnippet());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getLink()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, link, snippet;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.resultTitle);
            link = itemView.findViewById(R.id.resultLink);
            snippet = itemView.findViewById(R.id.resultSnippet);
        }
    }
}
