package com.example.noteapp.activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.noteapp.R;
import com.example.noteapp.database.NotesDatabase;
import com.example.noteapp.entities.Note;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText inputNoteTitle, inputNoteText;
    private TextView textDateTime;
    private ImageView imageNote;
    private ImageView imageAddImage;

    private String selectedImagePath;
    private String selectedNoteColor;

    public static final int REQUEST_CODE_STORAGE_PERMISSION = 1;

    private Note alreadyAvailableNote;

    private AlertDialog dialogDeleteNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        ImageView imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteText = findViewById(R.id.inputNote);
        textDateTime = findViewById(R.id.textDateTime);
        imageNote = findViewById(R.id.imageNote);
        imageAddImage = findViewById(R.id.imageAddImage);

        textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date())
        );

        ImageView imageSave = findViewById(R.id.imageSave);
        imageSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveNote();
            }
        });

        selectedImagePath = "";
        selectedNoteColor = "#333333";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)){
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        findViewById(R.id.imageRemoveImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageNote.setImageBitmap(null);
                imageNote.setVisibility(View.GONE);
                findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
                selectedImagePath = "";
            }
        });

        addImage();
        initBackgroundColor();
    }

    private void setViewOrUpdateNote() {
        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        textDateTime.setText(alreadyAvailableNote.getDateTime());

        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }

        if (alreadyAvailableNote.getColor() != null && alreadyAvailableNote.getColor().trim().isEmpty()) {
            selectedNoteColor = alreadyAvailableNote.getColor();
        }
    }

    private void saveNote(){
        if (inputNoteTitle.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }
        else if (inputNoteText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Note note = new Note();
        note.setTitle(inputNoteTitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDateTime(textDateTime.getText().toString());
        note.setImagePath(selectedImagePath);
        note.setColor(selectedNoteColor);


        if (alreadyAvailableNote != null){
            note.setId(alreadyAvailableNote.getId());
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                //Background work here

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //UI Thread work here
                        Intent intent = new Intent();
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });
            }
        });
    }

    private void selectImage(){
        Log.d("DEBUG","mo anh");
        selectImageResultLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());;
    }

    ActivityResultLauncher<PickVisualMediaRequest> selectImageResultLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    try{
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                        selectedImagePath = getPathFromUri(uri);
                    }
                    catch (Exception e){
                        Toast.makeText(CreateNoteActivity.this, e.getMessage(), Toast.LENGTH_SHORT);
                    }
                }
            });

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length > 0){
            Log.d("DEBUG", "" + grantResults[0]);
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                selectImage();
            }
            else {
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getPathFromUri(Uri contentUri){
        String filePath;
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        if (cursor == null){
            filePath = contentUri.getPath();
        }
        else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    private void addImage(){
        imageAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int requestPermission = ContextCompat.checkSelfPermission(
                        getApplicationContext(), Manifest.permission.READ_MEDIA_IMAGES
                );
                if (requestPermission != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(
                            CreateNoteActivity.this,
                            new String[] {Manifest.permission.READ_MEDIA_IMAGES},
                            REQUEST_CODE_STORAGE_PERMISSION
                    );
                }
                else {
                    selectImage();
                }
            }
        });
    }

    private void initBackgroundColor(){
        final LinearLayout layoutBackgroundColor = findViewById(R.id.layoutBackgroundColor);
        final CoordinatorLayout layoutCreateNote = findViewById(R.id.layoutCreateNote);
        ImageView imageAddBackgroundColor = findViewById(R.id.imageAddBackgroundColor);
        imageAddBackgroundColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutBackgroundColor.getVisibility() == View.GONE){
                    layoutBackgroundColor.setVisibility(View.VISIBLE);
                }
                else {
                    layoutBackgroundColor.setVisibility(View.GONE);
                }
            }
        });

        List<ImageView> imageColors = new ArrayList<>();
        imageColors.add(layoutBackgroundColor.findViewById(R.id.imageColor1));
        imageColors.add(layoutBackgroundColor.findViewById(R.id.imageColor2));
        imageColors.add(layoutBackgroundColor.findViewById(R.id.imageColor3));
        imageColors.add(layoutBackgroundColor.findViewById(R.id.imageColor4));
        imageColors.add(layoutBackgroundColor.findViewById(R.id.imageColor5));
        imageColors.add(layoutBackgroundColor.findViewById(R.id.imageColor6));
        imageColors.add(layoutBackgroundColor.findViewById(R.id.imageColor7));
        imageColors.add(layoutBackgroundColor.findViewById(R.id.imageColor8));
        imageColors.add(layoutBackgroundColor.findViewById(R.id.imageColor9));
        imageColors.add(layoutBackgroundColor.findViewById(R.id.imageColor10));

        for (int i = 1; i <= imageColors.size(); ++i){
            String viewId = "viewColor" + i;
            String colorName = "colorNoteColor" + i;
            int resID = getResources().getIdentifier(viewId, "id", getPackageName());
//            Log.d("DEBUG", layoutBackgroundColor.findViewById(resID).toString());

            View viewColor = layoutBackgroundColor.findViewById(resID);
            ImageView imageViewColor= imageColors.get(i - 1);
            viewColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int colorId = getResources().getIdentifier(colorName, "color", getPackageName());
                    layoutCreateNote.setBackgroundColor(ContextCompat.getColor(CreateNoteActivity.this, colorId));
                    //Log.d("DEBUG", layoutCreateNote.getBackground().toString());
                    selectedNoteColor = getResources().getString(colorId);
                    imageViewColor.setImageResource(R.drawable.ic_done);
                    for (ImageView color : imageColors) {
                        if (color != imageViewColor) {
                            color.setImageResource(0);
                        }
                    }
                    layoutBackgroundColor.setVisibility(View.GONE);
                }
            });
        }

        if (alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()){
            for (int i = 1; i <= imageColors.size(); ++i){
                String viewId = "viewColor" + i;
                String colorName = "colorNoteColor" + i;
                int resID = getResources().getIdentifier(viewId, "id", getPackageName());
                int colorId = getResources().getIdentifier(colorName, "color", getPackageName());
                if (getResources().getString(colorId).equals(alreadyAvailableNote.getColor())){
                    layoutBackgroundColor.findViewById(resID).performClick();
                }
            }
        }

        if (alreadyAvailableNote != null){
            layoutCreateNote.findViewById(R.id.imageDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteNoteDialog();
                }
            });
        }
    }

    private void showDeleteNoteDialog(){
        if (dialogDeleteNote == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_note,
                    (ViewGroup) findViewById(R.id.layoutDeleteNoteContainer)
            );
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if (dialogDeleteNote.getWindow() != null){
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());

                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            //Background work here
                            NotesDatabase.getDatabase(getApplicationContext()).noteDao().deleteNote(alreadyAvailableNote);

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //UI Thread work here
                                    Intent intent = new Intent();
                                    intent.putExtra("isNoteDeleted", true);
                                    setResult(RESULT_OK, intent);
                                    finish();
                                }
                            });
                        }
                    });
                }
            });
            view.findViewById(R.id.textCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialogDeleteNote.dismiss();
                }
            });
        }
        dialogDeleteNote.show();
    }
}