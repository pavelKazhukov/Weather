package com.example.weather;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;
import android.widget.*;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private EditText user_field;
    private Button main_button;
    private Button history_button;
    private TextView result;
    private File internalStorageHistory;
    public static final int historyMaxSize = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = findViewById(R.id.result);
        user_field = findViewById(R.id.user_field);
        main_button = findViewById(R.id.main_button);
        history_button = findViewById(R.id.history_button);
        internalStorageHistory = new File(getFilesDir(), "history");

        if (!internalStorageHistory.exists()) {
            try {
                internalStorageHistory.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        main_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                String user_field_text = user_field.getText().toString().trim();
                if (user_field.getText().toString().trim().equals("")) {
                    Toast.makeText(MainActivity.this, R.string.no_user_input, Toast.LENGTH_SHORT).show();
                } else {
                    String city = user_field.getText().toString();
                    String key = "e619258fd8103ae3e0b0238be68e63cd";
                    String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city +
                            "&appid=" + key + "&units=metric";

                    saveRequest(city);
                    new GetURLData().execute(url);
                }
            }
        });

        history_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
                intent.putExtra("file", internalStorageHistory);
                startActivity(intent);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void saveRequest(String city) {
        Queue<String> queue = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(internalStorageHistory))){
            String currentLine;

            while ((currentLine = reader.readLine()) != null) queue.add(currentLine);
            if (queue.size() >= 50) queue.poll();
            queue.add(city);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try(BufferedWriter writer = Files.newBufferedWriter(
                Paths.get(String.valueOf(internalStorageHistory)), StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String element : queue) {
                writer.write(element + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class GetURLData extends AsyncTask<String, String, String> {
        protected  void onPreExecute() {
            super.onPreExecute();
            result.setText("Stand by...");
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while((line = reader.readLine()) != null)
                    buffer.append(line).append("\n");

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null)
                    connection.disconnect();

                try {
                    if(reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                if (result == null) {
                    Toast.makeText(MainActivity.this, R.string.city_not_found, Toast.LENGTH_SHORT).show();
                    MainActivity.this.result.setText("");
                }
                else {
                    JSONObject jsonObject = new JSONObject(result);
                    String Description = "Description: " + jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
                    String Temperature = "Temperature: " + jsonObject.getJSONObject("main").getDouble("temp") + " C°";
                    String Feels_like = "Feels like: " + jsonObject.getJSONObject("main").getDouble("feels_like") + " C°";
                    String Wind_speed = "Wind speed: " + jsonObject.getJSONObject("wind").getDouble("speed") + " m/s";

                    MainActivity.this.result.setText(Description + "\n" + Temperature + "\n" + Feels_like + "\n"  + Wind_speed);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}