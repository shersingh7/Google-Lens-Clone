package com.dv.googlelens;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.dv.googlelens.adapter.DetectedLabelAdapter;
import com.dv.googlelens.adapter.SearchResultAdapter;
import com.dv.googlelens.data.AppDatabase;
import com.dv.googlelens.data.HistoryItem;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_GALLERY = 101;
    private static final int PERMISSION_REQUEST = 200;

    private ImageView imagePreview;
    private TextView emptyStateText;
    private ProgressBar progressBar;
    private ChipGroup modeChipGroup;
    private RecyclerView labelsRecycler, resultsRecycler;
    private TextView detectedTitle, webResultsTitle;
    private CardView textResultCard, barcodeResultCard;
    private TextView extractedText, barcodeText, barcodeType;
    private FrameLayout boundingBoxContainer;
    private FloatingActionButton fabCamera, fabGallery;
    private View btnAnalyze;

    private Bitmap currentBitmap;
    private Uri currentImageUri;
    private String savedImagePath;
    private AnalysisMode currentMode = AnalysisMode.LABELS;

    private DetectedLabelAdapter labelAdapter;
    private SearchResultAdapter resultAdapter;
    private RequestQueue requestQueue;
    private AppDatabase database;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        currentBitmap = (Bitmap) extras.get("data");
                        if (currentBitmap != null) {
                            showImage(currentBitmap);
                            savedImagePath = saveBitmapToDisk(currentBitmap);
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            showImage(currentBitmap);
                            savedImagePath = saveBitmapToDisk(currentBitmap);
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        requestQueue = Volley.newRequestQueue(this);
        database = AppDatabase.getInstance(this);

        labelAdapter = new DetectedLabelAdapter(this);
        labelAdapter.setOnLabelClickListener(this::performWebSearch);
        labelsRecycler.setAdapter(labelAdapter);

        resultAdapter = new SearchResultAdapter(this);
        resultsRecycler.setAdapter(resultAdapter);

        modeChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipLabels)) currentMode = AnalysisMode.LABELS;
            else if (checkedIds.contains(R.id.chipText)) currentMode = AnalysisMode.TEXT;
            else if (checkedIds.contains(R.id.chipBarcode)) currentMode = AnalysisMode.BARCODE;
        });

        fabCamera.setOnClickListener(v -> openCamera());
        fabGallery.setOnClickListener(v -> openGallery());
        btnAnalyze.setOnClickListener(v -> startAnalysis());

        findViewById(R.id.btnCopy).setOnClickListener(v -> copyToClipboard(extractedText.getText().toString()));
        findViewById(R.id.btnShareText).setOnClickListener(v -> shareText(extractedText.getText().toString()));
        findViewById(R.id.btnCopyBarcode).setOnClickListener(v -> copyToClipboard(barcodeText.getText().toString()));
        findViewById(R.id.btnOpenLink).setOnClickListener(v -> {
            String url = barcodeText.getText().toString();
            if (!url.startsWith("http")) url = "https://" + url;
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        handleIncomingShare(getIntent());
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        imagePreview = findViewById(R.id.imagePreview);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);
        modeChipGroup = findViewById(R.id.modeChipGroup);
        labelsRecycler = findViewById(R.id.labelsRecycler);
        resultsRecycler = findViewById(R.id.resultsRecycler);
        detectedTitle = findViewById(R.id.detectedTitle);
        webResultsTitle = findViewById(R.id.webResultsTitle);
        textResultCard = findViewById(R.id.textResultCard);
        barcodeResultCard = findViewById(R.id.barcodeResultCard);
        extractedText = findViewById(R.id.extractedText);
        barcodeText = findViewById(R.id.barcodeText);
        barcodeType = findViewById(R.id.barcodeType);
        boundingBoxContainer = findViewById(R.id.boundingBoxContainer);
        fabCamera = findViewById(R.id.fabCamera);
        fabGallery = findViewById(R.id.fabGallery);
        btnAnalyze = findViewById(R.id.btnAnalyze);
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST);
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void showImage(Bitmap bitmap) {
        currentBitmap = bitmap;
        imagePreview.setImageBitmap(bitmap);
        emptyStateText.setVisibility(View.GONE);
        resetResults();
    }

    private void resetResults() {
        detectedTitle.setVisibility(View.GONE);
        labelsRecycler.setVisibility(View.GONE);
        webResultsTitle.setVisibility(View.GONE);
        resultsRecycler.setVisibility(View.GONE);
        textResultCard.setVisibility(View.GONE);
        barcodeResultCard.setVisibility(View.GONE);
        boundingBoxContainer.setVisibility(View.GONE);
        boundingBoxContainer.removeAllViews();
        resultAdapter.setItems(new ArrayList<>());
        labelAdapter.setLabels(new ArrayList<>());
    }

    private void startAnalysis() {
        if (currentBitmap == null) {
            Toast.makeText(this, R.string.error_no_image, Toast.LENGTH_SHORT).show();
            return;
        }
        resetResults();
        progressBar.setVisibility(View.VISIBLE);

        InputImage image = InputImage.fromBitmap(currentBitmap, 0);

        switch (currentMode) {
            case LABELS:
                runLabelDetection(image);
                break;
            case TEXT:
                runTextRecognition(image);
                break;
            case BARCODE:
                runBarcodeScanning(image);
                break;
        }
    }

    private void runLabelDetection(InputImage image) {
        ImageLabeler labeler = ImageLabeling.getClient(com.google.mlkit.vision.label.defaults.ImageLabelerOptions.DEFAULT_OPTIONS);
        labeler.process(image)
                .addOnSuccessListener(labels -> {
                    progressBar.setVisibility(View.GONE);
                    if (labels.isEmpty()) {
                        Toast.makeText(this, "No objects detected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    labelAdapter.setLabels(labels);
                    detectedTitle.setVisibility(View.VISIBLE);
                    labelsRecycler.setVisibility(View.VISIBLE);

                    // Auto-search top label
                    if (!labels.isEmpty()) {
                        String topLabel = labels.get(0).getText();
                        saveHistory(topLabel, "LABELS");
                        performWebSearch(topLabel);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.error_analysis, Toast.LENGTH_SHORT).show();
                });
    }

    private void runTextRecognition(InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    progressBar.setVisibility(View.GONE);
                    String text = result.getText();
                    if (text.trim().isEmpty()) {
                        Toast.makeText(this, "No text found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    extractedText.setText(text);
                    textResultCard.setVisibility(View.VISIBLE);
                    textResultCard.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
                    saveHistory(text.substring(0, Math.min(text.length(), 40)) + "...", "TEXT");
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.error_analysis, Toast.LENGTH_SHORT).show();
                });
    }

    private void runBarcodeScanning(InputImage image) {
        BarcodeScanner scanner = BarcodeScanning.getClient();
        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    progressBar.setVisibility(View.GONE);
                    if (barcodes.isEmpty()) {
                        Toast.makeText(this, "No barcode found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Barcode code = barcodes.get(0);
                    String value = code.getRawValue() != null ? code.getRawValue() : "";
                    barcodeText.setText(value);
                    barcodeType.setText(formatBarcodeType(code.getFormat()));
                    barcodeResultCard.setVisibility(View.VISIBLE);
                    barcodeResultCard.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

                    boolean isUrl = code.getValueType() == Barcode.TYPE_URL || (value.startsWith("http") || value.startsWith("www"));
                    findViewById(R.id.btnOpenLink).setVisibility(isUrl ? View.VISIBLE : View.GONE);

                    saveHistory(value.substring(0, Math.min(value.length(), 40)), "BARCODE");
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.error_analysis, Toast.LENGTH_SHORT).show();
                });
    }

    private String formatBarcodeType(int format) {
        switch (format) {
            case Barcode.FORMAT_QR_CODE: return "QR Code";
            case Barcode.FORMAT_AZTEC: return "Aztec";
            case Barcode.FORMAT_CODABAR: return "Codabar";
            case Barcode.FORMAT_CODE_39: return "Code 39";
            case Barcode.FORMAT_CODE_93: return "Code 93";
            case Barcode.FORMAT_CODE_128: return "Code 128";
            case Barcode.FORMAT_DATA_MATRIX: return "Data Matrix";
            case Barcode.FORMAT_EAN_8: return "EAN-8";
            case Barcode.FORMAT_EAN_13: return "EAN-13";
            case Barcode.FORMAT_ITF: return "ITF";
            case Barcode.FORMAT_PDF417: return "PDF417";
            case Barcode.FORMAT_UPC_A: return "UPC-A";
            case Barcode.FORMAT_UPC_E: return "UPC-E";
            default: return "Unknown";
        }
    }

    private void performWebSearch(String query) {
        progressBar.setVisibility(View.VISIBLE);
        webResultsTitle.setVisibility(View.GONE);
        resultsRecycler.setVisibility(View.GONE);

        String url = "https://serpapi.com/search.json?q=" + Uri.encode(query)
                + "&location=Toronto,Canada&hl=en&gl=us&google_domain=google.com&api_key=YOUR_SERPAPI_KEY";

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        ArrayList<SearchRVModal> results = new ArrayList<>();
                        if (response.has("organic_results")) {
                            JSONArray array = response.getJSONArray("organic_results");
                            for (int i = 0; i < Math.min(array.length(), 10); i++) {
                                org.json.JSONObject obj = array.getJSONObject(i);
                                results.add(new SearchRVModal(
                                        obj.optString("title", ""),
                                        obj.optString("link", ""),
                                        obj.optString("displayed_link", ""),
                                        obj.optString("snippet", "")
                                ));
                            }
                        }
                        resultAdapter.setItems(results);
                        if (!results.isEmpty()) {
                            webResultsTitle.setVisibility(View.VISIBLE);
                            resultsRecycler.setVisibility(View.VISIBLE);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.error_network, Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void saveHistory(String query, String mode) {
        new Thread(() -> {
            HistoryItem item = new HistoryItem(query, mode, savedImagePath, System.currentTimeMillis());
            database.historyDao().insert(item);
        }).start();
    }

    private void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Lens Result", text));
            Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareText(String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private String saveBitmapToDisk(Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "LENS_" + timeStamp + ".jpg";
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "LensAI");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            return file.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private void handleIncomingShare(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null && intent.getType().startsWith("image/")) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) {
                try {
                    currentBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    showImage(currentBitmap);
                    savedImagePath = saveBitmapToDisk(currentBitmap);
                } catch (IOException e) {
                    Toast.makeText(this, "Failed to load shared image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        }
    }
}
