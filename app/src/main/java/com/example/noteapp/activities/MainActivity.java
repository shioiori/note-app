package com.example.noteapp.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.noteapp.R;
import com.example.noteapp.adapters.NotesAdapter;
import com.example.noteapp.database.NotesDatabase;
import com.example.noteapp.entities.Note;
import com.example.noteapp.listener.NotesListener;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NotesListener {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;

    private RecyclerView notesRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;

    private Note noteSelected;

    private int noteClickedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        imageAddNoteMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                addResultLauncher.launch(intent);
            }
        });

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        notesRecyclerView.setAdapter(notesAdapter);

        getNotes(REQUEST_CODE_SHOW_NOTES, false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (noteList.size() != 0) {
                    notesAdapter.searchNotes(s.toString());
                }
            }
        });
        registerForContextMenu(notesRecyclerView);
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        Log.d("DEBUG", "short");
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        noteClickedResultLauncher.launch(intent);
    }

    @Override
    public void onNoteLongClicked(Note note, int position) {
        Log.d("DEBUG", "long");
        noteSelected = note;
        noteClickedPosition = position;
    }

    ActivityResultLauncher<Intent> noteClickedResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        getNotes(REQUEST_CODE_UPDATE_NOTE, data.getBooleanExtra("isNoteDeleted", false));
                    }
                }
            });

    private void getNotes(final int requestCode, final boolean isNoteDeleted){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(new Runnable() {
            @Override
            public void run() {
                //Background work here
                List<Note> notes = NotesDatabase.getDatabase(getApplicationContext()).noteDao().getAllNotes();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //UI Thread work here
                        Log.d("DEBUG", "getnote " + noteClickedPosition);

                        if (requestCode == REQUEST_CODE_SHOW_NOTES){
                            noteList.addAll(notes);
                            notesAdapter.notifyDataSetChanged();
                        }
                        else if (requestCode == REQUEST_CODE_ADD_NOTE){
                            noteList.add(0, notes.get(0));
                            notesAdapter.notifyItemInserted(0);
                            notesRecyclerView.smoothScrollToPosition(0);
                        }
                        else if (requestCode == REQUEST_CODE_UPDATE_NOTE){
                            Log.d("DEBUG", "getnote " + noteClickedPosition);

                            noteList.remove(noteClickedPosition);

                            if (isNoteDeleted){
                                notesAdapter.notifyItemRemoved(noteClickedPosition);
                            }
                            else {
                                noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                                notesAdapter.notifyItemChanged(noteClickedPosition);
                            }
                        }
                    }
                });
            }
        });
    }


    ActivityResultLauncher<Intent> addResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                            getNotes(REQUEST_CODE_ADD_NOTE, false);
                        }
                    }
            });

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.action_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        Log.d("DEBUG", "click " + noteClickedPosition);
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        switch (item.getItemId()){
            case R.id.menuEdit:
                intent.putExtra("isViewOrUpdate", true);
                intent.putExtra("note", noteSelected);
                noteClickedResultLauncher.launch(intent);
                break;
            case R.id.menuDelete:
                getNotes(REQUEST_CODE_UPDATE_NOTE, true);

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        NotesDatabase.getDatabase(getApplicationContext()).noteDao().deleteNote(noteSelected);
                    }
                });
                break;
        }
        return super.onContextItemSelected(item);
    }
}