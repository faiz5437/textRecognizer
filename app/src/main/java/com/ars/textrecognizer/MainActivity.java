package com.ars.textrecognizer;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private MaterialButton inputImageBtn, reconizeTextBtn;
    private ShapeableImageView imageView;
    private EditText recognizedTextEdit;

    //TAG
    private static final String TAG = "MAIN_TAG";

    private Uri imageUri = null;

    //handle resulet of galley/camera permission
    private static final int CAMERA_REQUEST_CODE  = 100;
    private static final int STORAGE_REQUEST_CODE  = 101;

    private String[] cameraPermission;
    private String[] storagePermission;

    private ProgressDialog progressDialog;

    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputImageBtn = findViewById(R.id.inputImageBtn);
        reconizeTextBtn = findViewById(R.id.recognizedTextBtn);
        imageView = findViewById(R.id.imageIv);
        recognizedTextEdit = findViewById(R.id.recognizedTextEt);
        progressDialog = new ProgressDialog(this);

        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog.setTitle("Harap Tunggu");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(true);

        //init text reconizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputImageDialog();
            }


        });

        reconizeTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageUri == null){
                    Toast.makeText(MainActivity.this, "Silahkan Pilih Gambar Terlebih Dahulu", Toast.LENGTH_SHORT).show();
                }else {
                    recognizeTextFromImage();
                }
            }
        });
    }

    private void recognizeTextFromImage() {
        Log.d(TAG, "recognizeTextFromImage: ");
        progressDialog.setMessage("Perisapan Image ...");
        progressDialog.show();

        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);

            progressDialog.setMessage("Proses Pembacaan ...");

            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            progressDialog.dismiss();

                            String recognizedText = text.getText();
                            Log.d(TAG, "onSuccess: recognizedText: "+recognizedText);
                            recognizedTextEdit.setText(recognizedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "onFailure: ", e);
                            Toast.makeText(MainActivity.this, "Failed Recognizeing Text Due To "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });


        } catch (IOException e) {
            progressDialog.dismiss();
            Log.e(TAG, "recognizeTextFromImage: ", e);
            Toast.makeText(this, "Failed Preparing Image Due To "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showInputImageDialog() {
        PopupMenu popupMenu = new PopupMenu(this, inputImageBtn);

        popupMenu.getMenu().add(Menu.NONE, 1, 1,"CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 2,"GALLERY");

        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();

                if (id == 1){
                    Log.d(TAG, "onMenuItemClick: Camera Clicked ...");
                    if (checkCameraPermission()) {

                        pickImageCamera();
                    }
                    else {
                        requestCameraPermissions();
                    }
                }else if (id == 2){
                    Log.d(TAG, "onMenuItemClick: Gallery Clicked ...");
                    if (checkStoragePermission()) {
                        pickImageGallery();

                    }else{
                        requestStoragePermission();
                    }

                }


                return true;
            }
        });


    }
    private void pickImageGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);

        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);

    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //di sini kita menerima image jika di pick
                    if (result.getResultCode() == Activity.RESULT_OK){
                        // gambar di pick
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri: "+ imageUri);
                        // set ke imageviw
                        imageView.setImageURI(imageUri);
                    }else{
                        Log.d(TAG, "galeryOnActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Gagal Mengambil Gambar", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);


    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        // image is taken from camera
                        // we already have the image in imageUri using function pickImageCamera()
                        Log.d(TAG, "onActivityResult: imageUri: " + imageUri);
                        imageView.setImageURI(imageUri);

                    }else{
                        Log.d(TAG, "cameraOnActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Gagal Mengambil Gambar", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission(){

        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return cameraResult && storageResult;

    }

    private void requestCameraPermissions(){
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    //handle permission result


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        switch (requestCode){
            case CAMERA_REQUEST_CODE:{

                if (grantResults.length>0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && storageAccepted){
                        pickImageCamera();
                    }else {
                        Toast.makeText(this, "Izin Kamera Dan Storage belum terbuka", Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Toast.makeText(this, "Izin Camera Dan Storage Gagal", Toast.LENGTH_SHORT).show();
                }



            }
            break;
            case STORAGE_REQUEST_CODE:{

                if (grantResults.length>0){

                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (storageAccepted){
                        pickImageGallery();
                    }else {
                        Toast.makeText(this, "Izin Storage belum Terbuka", Toast.LENGTH_SHORT).show();
                    }

                }else {
                    Toast.makeText(this, "izin Strorage Gagal", Toast.LENGTH_SHORT).show();
                }

            }
            break;
        }
    }
}