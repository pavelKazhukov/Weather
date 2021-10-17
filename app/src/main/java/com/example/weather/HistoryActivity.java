package com.example.weather;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.*;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.w3c.dom.Text;

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

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemClick(AdapterView<?> parent, View itemClicked, int position, long id) {
                TextView textItem = (TextView) itemClicked;
                String strItem = textItem.getText().toString();
                Intent intent = new Intent(HistoryActivity.this, MainActivity.class);
                intent.putExtra("clickedCity", strItem);
                startActivity(intent);
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