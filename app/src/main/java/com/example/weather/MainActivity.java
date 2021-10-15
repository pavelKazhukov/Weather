package com.example.weather;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.SharedMemory;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {

    private EditText user_field;
    private Button main_button;
    private TextView result;
    private final File internalStorageHistory = new File(getFilesDir(), "history");
    private final int historyMaxSize = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        user_field = findViewById(R.id.user_field);
        main_button = findViewById(R.id.main_button);
        result = findViewById(R.id.result);

        if (!internalStorageHistory.exists()) {
            try {
                internalStorageHistory.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        main_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user_field_text = user_field.getText().toString().trim();
                if (user_field.getText().toString().trim().equals("")) {
                    Toast.makeText(MainActivity.this, R.string.no_user_input, Toast.LENGTH_SHORT).show();
                } else {
                    String city = user_field.getText().toString();
                    String key = "e619258fd8103ae3e0b0238be68e63cd";
                    String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + key + "&units=metric";

                    try (RandomAccessFile raf = new RandomAccessFile(internalStorageHistory, "rw")){
                        Queue<String> buffer = new LinkedList<>();
                        String currentStr;

                        for (int i = 0; (currentStr = raf.readLine()) != null; i++) {
                            buffer.add(currentStr);
                        }

                        if (buffer.size() >= historyMaxSize) buffer.poll();
                        buffer.add(city);
                        raf.
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    new GetURLData().execute(url);
                }
            }
        });
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