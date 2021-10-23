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
import androidx.recyclerview.widget.DividerItemDecoration;
import com.google.android.gms.common.util.JsonUtils;
import org.json.JSONArray;
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
    private static final String key = "e619258fd8103ae3e0b0238be68e63cd";
    private EditText user_field;
    private ImageButton main_button;
    private TextView result;
    private TextView temperature;
    private File internalStorageHistory;
    private Button forecastButton1, forecastButton2, forecastButton3, forecastButton4, forecastButton5, currentButton;
    private JSONObject[] forecastInfo1, forecastInfo2, forecastInfo3, forecastInfo4, forecastInfo5;
    private int jsonArrayIndex = 0;

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
        ImageButton history_button = findViewById(R.id.history_button);
        internalStorageHistory = new File(getFilesDir(), "history");
        forecastButton1 = findViewById(R.id.forecast_button1);
        forecastButton2 = findViewById(R.id.forecast_button2);
        forecastButton3 = findViewById(R.id.forecast_button3);
        forecastButton4 = findViewById(R.id.forecast_button4);
        forecastButton5 = findViewById(R.id.forecast_button5);

        forecastButton1.setEnabled(false);
        currentButton = forecastButton1;
        LocationInterpreter locationInterpreter;

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
        } else {
            locationInterpreter = new LocationInterpreter();
            getPermissions();

            if (MyLocationListener.imHere != null) {
                double lon = MyLocationListener.imHere.getLongitude();
                double lat = MyLocationListener.imHere.getLatitude();
                user_field.setText(locationInterpreter.getCity(lon, lat, this));
                sendURL(user_field);
            }
        }

        main_button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(main_button.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

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

        forecastButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getClick(forecastButton1, forecastInfo1);
                currentButton = forecastButton1;
            }
        });

        forecastButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getClick(forecastButton2, forecastInfo2);
                currentButton = forecastButton2;
            }
        });

        forecastButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getClick(forecastButton3, forecastInfo3);
                currentButton = forecastButton3;
            }
        });

        forecastButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getClick(forecastButton4, forecastInfo4);
                currentButton = forecastButton4;
            }
        });

        forecastButton5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getClick(forecastButton5, forecastInfo5);
                currentButton = forecastButton5;
            }
        });
    }

    private void getClick(Button forecastButton, JSONObject[] forecastInfo) {
        forecastButton.setEnabled(false);
        currentButton.setEnabled(true);

        try {
            ConstructForecastList(forecastInfo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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

    private class DegreeInterpreter {
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
    private enum Direction {
        North, Northeast, East, Southeast, South, Southwest, West, Northwest, Error
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendURL(EditText user_field) {
        String city = user_field.getText().toString();
        GetURLData getURLData = new GetURLData();

        String url1 = "https://api.openweathermap.org/data/2.5/weather?q=" + city +
                "&appid=" + key + "&units=metric";
        String url2_1 = "https://api.openweathermap.org/data/2.5/forecast?id=";
        String url2_2 = "&appid=" + key + "&units=metric";

        getURLData.execute(url1, url2_1, url2_2);
    }
    private class GetURLData extends AsyncTask<String, String, String> {
        private int cityId;
        public int getCityId() {
            return cityId;
        }

        protected void onPreExecute() {
            super.onPreExecute();
            result.setText("Stand by...");
            temperature.setText("");
        }

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection = null;
            JSONObject jsonObjectCheck = null;
            BufferedReader reader = null;
            StringBuffer buffer = new StringBuffer(), bufferCheck = new StringBuffer();

            for (int i = 0; i < strings.length - 1; i++) {
                try {
                    if (i == strings.length - 2) {
                        jsonObjectCheck = new JSONObject(buffer.toString());
                        cityId = jsonObjectCheck.getInt("id");
                        strings[strings.length - 2] += cityId + strings[strings.length - 1];
                    }

                    URL url = new URL(strings[i]);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    InputStream stream = connection.getInputStream();
                    reader = new BufferedReader(new InputStreamReader(stream));

                    String line = "";

                    while((line = reader.readLine()) != null)
                        buffer.append(line).append("\n");

                } catch (IOException | JSONException e) {
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
                if (buffer.toString().equals(bufferCheck.toString())) return null;
                else bufferCheck.append(buffer);
            }
            return buffer.toString();
        }

        @SuppressLint("SetTextI18n")
        protected void onPostExecute(String fromURLs_result) {
            super.onPostExecute(fromURLs_result);

            try {
                if (fromURLs_result == null ) {
                    Toast.makeText(MainActivity.this, R.string.city_not_found, Toast.LENGTH_SHORT).show();
                    result.setText("");
                    temperature.setText("");
                }
                else {
                    String[] resultsArray = fromURLs_result.split("\n");
                    JSONObject jsonObjectWeather = new JSONObject(resultsArray[0]);
                    JSONObject jsonObjectForecast = new JSONObject(resultsArray[1]);

                    MainActivity.this.temperature.setText(ParsingWeatherTemperature(jsonObjectWeather));
                    MainActivity.this.result.setText(ParsingJSONObject(jsonObjectWeather));
                    findViewById(R.id.line_below_temperature).setVisibility(View.VISIBLE);

                    int[] dates = ParsingForecastDates(jsonObjectForecast);
                    int index = 0;
                    initializeButton(forecastButton1, dates, index++);
                    initializeButton(forecastButton2, dates, index++);
                    initializeButton(forecastButton3, dates, index++);
                    initializeButton(forecastButton4, dates, index++);
                    initializeButton(forecastButton5, dates, index);

                    ParsingForecastOnJSONObjects(jsonObjectForecast);

                    ConstructForecastList(forecastInfo1);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    private void initializeButton(Button button, int[] dates, int index) {
        button.setText(String.valueOf(dates[index]));
        button.setVisibility(View.VISIBLE);
    }

    private void ConstructForecastList(JSONObject[] jsonObjects) throws JSONException {
        String[] infoList = new String[jsonObjects.length];

        for (int i = 0; i < jsonObjects.length; i++) {
            infoList[i] = ParsingJSONObject(jsonObjects[i]);
        }

        ListView forecastList = findViewById(R.id.forecast_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, R.layout.list_item, infoList);
        forecastList.setAdapter(adapter);
    }
    private String ParsingJSONObject(JSONObject jsonObject) throws JSONException {
        StringBuilder info = new StringBuilder();
        DegreeInterpreter interpreter = new DegreeInterpreter();

        if (jsonObject.has("dt_txt")) {
            String[] date = jsonObject.getString("dt_txt").split(" ")[1].split(":");
                info.append(date[0]).append(":").append(date[1]).append("\n\n");
                info.append("Status:_____________________").append(jsonObject.getJSONArray("weather").getJSONObject(0).getString("description")).append("\n");
                info.append("Feels like:_________________").append(jsonObject.getJSONObject("main").getDouble("feels_like")).append(" C°").append("\n");
            if(jsonObject.getJSONObject("main").has("pressure")) {
                int pressure = jsonObject.getJSONObject("main").getInt("pressure");
                info.append("Pressure:___________________").append(pressure * 3 / 4).append(" mm Hg").append("\n");
            }
            if(jsonObject.getJSONObject("main").has("humidity"))
                info.append("Humidity:___________________").append(jsonObject.getJSONObject("main").getInt("humidity")).append(" %").append("\n");
            if(jsonObject.has("visibility"))
                info.append("Visibility:_________________").append(jsonObject.getInt("visibility")).append(" m").append("\n");
            if(jsonObject.getJSONObject("wind").has("speed"))
                info.append("Wind speed:_________________").append(jsonObject.getJSONObject("wind").getDouble("speed")).append(" m/s").append(", ");
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
                info.append("Gusts:______________________").append(jsonObject.getJSONObject("wind").getDouble("gust")).append(" m/s");
        } else {
            info.append("Status: ").append(jsonObject.getJSONArray("weather").getJSONObject(0).getString("description")).append("\n");
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
        }

        return info.toString();
    }
    private String ParsingWeatherTemperature(JSONObject jsonObject) throws JSONException {
        return jsonObject.getJSONObject("main").getDouble("temp") + " C°" + "\n";
    }
    private int[] ParsingForecastDates(JSONObject jsonObject) throws JSONException {
        String date = jsonObject.getJSONArray("list").getJSONObject(0).getString("dt_txt");
        int[] result = new int[5];
        result[0] = Integer.parseInt(date.split(" ")[0].split("-")[2]);
        for (int i = 1; i < result.length; i++) {
            result[i] = result[0] + i;
        }
        return result;
    }
    private void ParsingForecastOnJSONObjects(JSONObject jsonObject) throws JSONException {
        String date = jsonObject.getJSONArray("list").getJSONObject(0).getString("dt_txt");
        int time = Integer.parseInt(date.split(" ")[1].split(":")[0]);

        final int HOURS_PER_DAY = 24;
        final int HOURS_BEFORE_UPDATE = 3;
        final int UPDATES_PER_DAY = HOURS_PER_DAY / HOURS_BEFORE_UPDATE;

        int sizeOfFirstInfoArray = (HOURS_PER_DAY - time) / HOURS_BEFORE_UPDATE;

        JSONArray jsonArray = jsonObject.getJSONArray("list");

        forecastInfo1 = new JSONObject[sizeOfFirstInfoArray];
        forecastInfo2 = new JSONObject[UPDATES_PER_DAY];
        forecastInfo3 = new JSONObject[UPDATES_PER_DAY];
        forecastInfo4 = new JSONObject[UPDATES_PER_DAY];
        forecastInfo5 = new JSONObject[UPDATES_PER_DAY];

        InitializeJSONArrays(forecastInfo1, jsonArray, sizeOfFirstInfoArray);
        InitializeJSONArrays(forecastInfo2, jsonArray, UPDATES_PER_DAY);
        InitializeJSONArrays(forecastInfo3, jsonArray, UPDATES_PER_DAY);
        InitializeJSONArrays(forecastInfo4, jsonArray, UPDATES_PER_DAY);
        InitializeJSONArrays(forecastInfo5, jsonArray, UPDATES_PER_DAY);
        jsonArrayIndex = 0;
    }

    private void InitializeJSONArrays(JSONObject[] jsonArrayForButton,
                                      JSONArray jsonArray, int sizeOfArray) throws JSONException {
        for (int i = 0; i < sizeOfArray; i++) {
            jsonArrayForButton[i] = jsonArray.getJSONObject(jsonArrayIndex);
            jsonArrayIndex++;
        }
    }
}