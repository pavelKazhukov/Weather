package com.example.weather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.common.util.JsonUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 0;
    private EditText user_field;
    private ImageButton main_button;
    private ImageButton history_button;
    private TextView result;
    private TextView temperature;
    private File internalStorageHistory;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        result = findViewById(R.id.result);
        temperature = findViewById(R.id.temperature);
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
        if (intent.hasExtra("clickedCity")) {
            Bundle arguments = intent.getExtras();
            user_field.setText((String) arguments.get("clickedCity"));
            sendURL(user_field);
        }

        LocationInterpreter locationInterpreter = new LocationInterpreter();
        getPermissions();

        if (MyLocationListener.imHere != null) {
            double lon = MyLocationListener.imHere.getLongitude();
            double lat = MyLocationListener.imHere.getLatitude();
            user_field.setText(locationInterpreter.getCity(lon, lat, this));
            sendURL(user_field);
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
                    saveRequest(user_field.getText().toString());
                    sendURL(user_field);
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

    private void getPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast.makeText(this, "Needed to show the current city", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[] {
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        PERMISSION_REQUEST_CODE);
            }
            return;
        }
        MyLocationListener.SetUpLocationListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                //Здесь мы получаем разрешение и снова вызываем getPermissions()
                getPermissions();
            } else {
                Toast.makeText(this, "Denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendURL(EditText user_field) {
        String city = user_field.getText().toString();
        String key = "e619258fd8103ae3e0b0238be68e63cd";
        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city +
                "&appid=" + key + "&units=metric";

        new GetURLData().execute(url);
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

    class DegreeInterpreter {
        public Direction getDirection(int degree) {
            if (degree == 360 || (degree >= 0 && degree < 45)) return Direction.North;
            else if (degree < 90) return Direction.Northeast;
            else if (degree < 135) return Direction.East;
            else if (degree < 180) return Direction.Southeast;
            else if (degree < 225) return Direction.South;
            else if (degree < 270) return Direction.Southwest;
            else if (degree < 315) return Direction.West;
            else if (degree < 360) return Direction.Northwest;
            return Direction.Error;
        }
    }

    enum Direction {
        North, Northeast, East, Southeast, South, Southwest, West, Northwest, Error
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
                    StringBuilder temperature = new StringBuilder();
                    StringBuilder info = new StringBuilder();
                    DegreeInterpreter interpreter = new DegreeInterpreter();

                    temperature.append(jsonObject.getJSONObject("main").getDouble("temp")).append(" C°").append("\n");
                    info.append("Status: ").append(jsonObject.getJSONArray("weather").getJSONObject(0).getString("description")).append("\n\n");
                    info.append("Feels like:    ").append(jsonObject.getJSONObject("main").getDouble("feels_like")).append(" C°").append("\n");
                    if(jsonObject.getJSONObject("main").has("pressure")) {
                        int pressure = jsonObject.getJSONObject("main").getInt("pressure");
                        info.append("Pressure:      ").append(pressure * 3 / 4).append(" mm Hg").append("\n");
                    }
                    if(jsonObject.getJSONObject("main").has("humidity"))
                        info.append("Humidity:      ").append(jsonObject.getJSONObject("main").getInt("humidity")).append(" %").append("\n");
                    if(jsonObject.has("visibility"))
                        info.append("Visibility:    ").append(jsonObject.getInt("visibility")).append(" m").append("\n");
                    if(jsonObject.getJSONObject("wind").has("speed"))
                        info.append("Wind speed:    ").append(jsonObject.getJSONObject("wind").getDouble("speed")).append(" m/s").append(", ");
                    if(jsonObject.getJSONObject("wind").has("deg")) {
                        int degree = jsonObject.getJSONObject("wind").getInt("deg");
                        switch (interpreter.getDirection(degree)) {
                            case North:
                                info.append("N");
                                break;
                            case Northeast:
                                info.append("NE");
                                break;
                            case East:
                                info.append("E");
                                break;
                            case Southeast:
                                info.append("SE");
                                break;
                            case South:
                                info.append("S");
                                break;
                            case Southwest:
                                info.append("SW");
                                break;
                            case West:
                                info.append("W");
                                break;
                            case Northwest:
                                info.append("NW");
                                break;
                            default:
                                info.append("Error!");
                                break;
                        }
                        info.append("\n");
                    }
                    if(jsonObject.getJSONObject("wind").has("gust"))
                        info.append("Gusts:         ").append(jsonObject.getJSONObject("wind").getDouble("gust")).append(" m/s");

                    MainActivity.this.temperature.setText(temperature);
                    MainActivity.this.result.setText(info);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}