package com.example.weather;

import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private Button clear_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Bundle arguments = getIntent().getExtras();
        File internalStorageHistory = (File) arguments.get("file");

        ArrayList<String> citiesList = new ArrayList<>();
        String currentLine;
        try (BufferedReader reader = new BufferedReader(new FileReader(internalStorageHistory))){
            for (int i = 0; (currentLine = reader.readLine()) != null; i++) {
               citiesList.add(currentLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ListView listView = findViewById(R.id.history_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(HistoryActivity.this, R.layout.list_item, citiesList);
        listView.setAdapter(adapter);

        clear_button = findViewById(R.id.clear_button);

        clear_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                clearFileInfo(internalStorageHistory);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void clearFileInfo(File file) {
        try(BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(String.valueOf(file)), StandardOpenOption.TRUNCATE_EXISTING)) {
            recreate();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}