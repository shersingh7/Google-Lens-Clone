package com.dv.googlelens;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dv.googlelens.adapter.HistoryAdapter;
import com.dv.googlelens.data.AppDatabase;
import com.google.android.material.appbar.MaterialToolbar;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private View emptyView;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        toolbar = findViewById(R.id.historyToolbar);
        recyclerView = findViewById(R.id.historyRecycler);
        emptyView = findViewById(R.id.emptyHistory);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new HistoryAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        AppDatabase.getInstance(this).historyDao().getAllHistory().observe(this, items -> {
            adapter.setItems(items);
            emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(items.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }
}
