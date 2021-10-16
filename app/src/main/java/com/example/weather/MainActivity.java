package com.example.weather;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(main_button.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

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

                    ArrayList<String> data = new ArrayList<>();
                    data.add("Coordinates:       " + jsonObject.getJSONObject("coord").getDouble("lon") + "°, " +
                            jsonObject.getJSONObject("coord").getDouble("lat") + "°");
                    data.add("Description:       " + jsonObject.getJSONArray("weather").getJSONObject(0).getString("description"));
                    data.add("Temperature:       " + jsonObject.getJSONObject("main").getDouble("temp") + " C°");
                    data.add("Feels like:        " + jsonObject.getJSONObject("main").getDouble("feels_like") + " C°");
                    data.add("Temperature Min:   " + jsonObject.getJSONObject("main").getDouble("temp_min") + " C°");
                    data.add("Temperature Max:   " + jsonObject.getJSONObject("main").getDouble("temp_max") + " C°");
                    data.add("Pressure:          " + jsonObject.getJSONObject("main").getInt("pressure") + " hPa");
                    data.add("Humidity:          " + jsonObject.getJSONObject("main").getInt("humidity") + " %");
                    data.add("Sea Level:         " + jsonObject.getJSONObject("main").getInt("sea_level") + " m");
                    data.add("Ground Level:      " + jsonObject.getJSONObject("main").getInt("grnd_level") + " m");
                    data.add("Visibility:        " + jsonObject.getInt("visibility") + " m");
                    data.add("Wind speed:        " + jsonObject.getJSONObject("wind").getDouble("speed") + " m/s");
                    data.add("Wind degree:       " + jsonObject.getJSONObject("wind").getInt("deg") + " °");
                    data.add("Gusts:             " + jsonObject.getJSONObject("wind").getDouble("gust") + " m/s");

                    StringBuilder info = new StringBuilder();
                    for (String datum : data) {
                        info.append(datum).append("\n");
                    }
                    MainActivity.this.result.setText(info);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}