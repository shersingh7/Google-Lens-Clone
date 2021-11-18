package com.dv.googlelens;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageView capture;
    private Button snapBtn, resultBtn;
    private RecyclerView resultsRv;
    private SearchRVAdapter searchRVAdapter;
    private ArrayList<SearchRVModal> searchRVModalsArrayList;
    private Bitmap imageBitmap;
    private String title, link, displayedLink, snippet;

    int REQUEST_CODE = 1;
    private ProgressBar loading;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        capture = findViewById(R.id.image);
        snapBtn = findViewById(R.id.snapButton);
        resultBtn = findViewById(R.id.resultButton);
        resultsRv = findViewById(R.id.rv);
        loading = findViewById(R.id.progress);

        searchRVModalsArrayList = new ArrayList<>();
        searchRVAdapter = new SearchRVAdapter(this, searchRVModalsArrayList);
        resultsRv.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
        resultsRv.setAdapter(searchRVAdapter);


        snapBtn.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                searchRVModalsArrayList.clear();
                searchRVAdapter.notify();
                takePictureIntent();
            }
        });

        resultBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchRVModalsArrayList.clear();
                searchRVAdapter.notifyDataSetChanged();
                loading.setVisibility(View.VISIBLE);
                getResults();
            }
        });
    }

    private void getResults(){
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();

        labeler.processImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
            @Override
            public void onSuccess(List<FirebaseVisionImageLabel> firebaseVisionImageLabels) {
                String searchQuery = firebaseVisionImageLabels.get(0).getText();
                getSearchResults(searchQuery);
            }

        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Failed to detect the image!!!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE && resultCode==RESULT_OK){
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            capture.setImageBitmap(imageBitmap);
        }
    }

    private void getSearchResults(String searchQuery) {
        String url = "https://serpapi.com/search.json?q="+searchQuery+"&location=Toronto,Canada&hl=en&gl=us&google_domain=google.com&api_key=5643f7e22276a34b7e259b38e2acaa9252a6ac7ebcd659b3b25745b07be83518";
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loading.setVisibility(View.GONE);
                try {
                    JSONArray organicArray = response.getJSONArray("organic_results");
                    for (int i = 0; i < organicArray.length(); i++) {
                        JSONObject organicObj = organicArray.getJSONObject(i);
                        if (organicObj.has("title")) {
                            title = organicObj.getString("title");
                        }
                        if (organicObj.has("link")) {
                            link = organicObj.getString("link");
                        }
                        if (organicObj.has("displayed_link")) {
                            displayedLink = organicObj.getString("displayed_link");
                        }
                        if (organicObj.has("snippet")) {
                            snippet = organicObj.getString("snippet");
                        }
                        searchRVModalsArrayList.add(new SearchRVModal(title, link, displayedLink, snippet));
                    }
                    searchRVAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            },new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "No Results Fuund...", Toast.LENGTH_SHORT).show();
            }

        });

        queue.add(jsonArrayRequest);

    }

    private void takePictureIntent() {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(i.resolveActivity(getPackageManager()) != null){
            startActivityForResult(i, REQUEST_CODE);
        }
    }
}