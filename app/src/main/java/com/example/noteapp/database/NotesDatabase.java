package com.example.noteapp.database;

import android.content.Context;
import android.util.Log;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.noteapp.dao.NoteDao;
import com.example.noteapp.entities.Note;

@Database(entities = Note.class, version = 2, exportSchema = false)
public abstract class NotesDatabase extends RoomDatabase {
    private static NotesDatabase notesDatabase;

    static final Migration MIGRATION_1_2 = new Migration(1,2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE notes ADD COLUMN record TEXT");
            Log.d("DEBUG", "Add ok!");
        }
    };

    public static synchronized NotesDatabase getDatabase(Context context){
        notesDatabase = Room.databaseBuilder(
                context,
                NotesDatabase.class,
                "notes_db"
        ).addMigrations(MIGRATION_1_2).build();
        return notesDatabase;
    }

    public abstract NoteDao noteDao();
}

