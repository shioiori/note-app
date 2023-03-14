package com.example.noteapp.entities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.UUID;

public class AudioRecord{
    private static final String LOG_TAG = "AudioRecordTest";
    private static String fileName = null;
    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    public AudioRecord(){
        fileName = UUID.randomUUID().toString();
    }

    public AudioRecord(String fileName){
        this.fileName = fileName;
    }

    public void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    public void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    public void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    public void stopPlaying() {
        player.release();
        player = null;
    }

    public String setFileName(){
        return UUID.randomUUID().toString();
    }

    public String getFileName(){
        return fileName;
    }

    public void startRecording() {
        Log.d("DEBUG", "here");
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        Log.d("DEBUG", "dang o trong record");
        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("DEBUG", e.toString());
        }

        recorder.start();
    }

    public void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }
}
