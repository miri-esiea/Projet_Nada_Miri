package nada.esiea.org.weatherapp;


import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**

 */
public class WeatherTaskActivity extends AsyncTask<String, Void, String[]> {
    protected ArrayList<String> detailedWeatherShow = new ArrayList<>();
    protected ArrayAdapter<String> adapter;
    protected ArrayList<String> tempArray = new ArrayList<>();
    protected ArrayList<String> descriptionList = new ArrayList<>();
    protected HashSet<String> removeDuplicate = new HashSet<>();
    protected TextView humidityText;
    public TextView precipitationText;
    protected TextView descriptionText;
    protected TextView windSpeedText;
    protected TextView pressureText;
    protected TextView temperatureText;
    protected TextView cityText;
    protected ImageView icon;
    private final String LOG_TAG = WeatherTaskActivity.class.getSimpleName();

    private String getReadableDateString(long time) {


        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    private String formatHighLows(double high, double low) {

        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    public String translateDegree(double degree) {
        String direction = "";
        if (degree >= 348.75 || degree < 11.25)
            direction = "N";
        if (degree >= 11.25 && degree < 33.75)
            direction = "NNE";
        if (degree >= 33.75 && degree < 56.25)
            direction = "NE";
        if (degree >= 56.25 && degree < 78.75)
            direction = "ENE";
        if (degree >= 78.75 && degree < 101.25)
            direction = "E";
        if (degree >= 101.25 && degree < 123.75)
            direction = "ESE";
        if (degree >= 123.75 && degree < 146.25)
            direction = "SE";
        if (degree >= 146.25 && degree < 168.75)
            direction = "SSE";
        if (degree >= 168.75 && degree < 191.25)
            direction = "S";
        if (degree >= 191.25 && degree < 213.75)
            direction = "SSW";
        if (degree >= 213.75 && degree < 236.25)
            direction = "SW";
        if (degree >= 236.25 && degree < 258.75)
            direction = "WSW";
        if (degree >= 258.75 && degree < 281.25)
            direction = "W";
        if (degree >= 281.25 && degree < 303.75)
            direction = "WNW";
        if (degree >= 303.75 && degree < 326.25)
            direction = "NW";
        if (degree >= 326.25 && degree < 348.75)
            direction = "NNW";
        return direction;

    }

    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "main";
        final String OWM_MAX = "temp_max";
        final String OWM_MIN = "temp_min";
        final String OWM_TEMP = "temp";
        final String OWM_DESCRIPTION = "description";
        final String OWM_PRESSURE = "pressure";
        final String OWM_HUMIDITY = "humidity";
        final String OWM_WIND = "wind";
        final String OWM_WINDSPEED = "speed";
        final String OWM_WIND_DIRECTION = "deg";
        final String OWM_CITY = "city";
        final String OWM_CITY_NAME = "name";
        final String OWM_RAIN = "rain";
        final String OWM_PRECIPITATION = "3h";
        ArrayList<Double> highestTemp = new ArrayList<>();
        ArrayList<Double> lowestTemp = new ArrayList<>();

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);



        Time dayTime = new Time();
        dayTime.setToNow();


        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        //  UTC
        dayTime = new Time();
        Log.v(LOG_TAG, "Test entry: " + getReadableDateString(dayTime.setJulianDay(julianStartDay)));
        Log.v(LOG_TAG, Long.toString(dayTime.setJulianDay(julianStartDay))); //1458205200
        String[] resultStrs = new String[numDays];
        for (int i = 0; i < weatherArray.length(); i++) {

            String day;
            String description;
            String highAndLow;
            JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
            String cityName = cityJson.getString(OWM_CITY_NAME);
            // chopper le JSON
            JSONObject dayForecast = weatherArray.getJSONObject(i);


            long dateTime;



            dateTime = dayTime.setJulianDay(julianStartDay + i);
            day = getReadableDateString(dateTime);

            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);
            double temp = temperatureObject.getDouble(OWM_TEMP);
            if(MainActivity.isMetric == false) {
                temp = convertMetricToImperial(temp);
                high = convertMetricToImperial(high);
                low = convertMetricToImperial(low);
            }
            int humidity = temperatureObject.getInt(OWM_HUMIDITY);
            double pressure = temperatureObject.getDouble(OWM_PRESSURE);
            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
            tempArray.add(Long.toString(Math.round(high)) + " on " + day.split(" ")[0]);
            tempArray.add(Long.toString(Math.round(low)) + " on " + day.split(" ")[0]);
            descriptionList.add(description);
            JSONObject windObject = dayForecast.getJSONObject(OWM_WIND);
            double speed = Math.round(windObject.getDouble(OWM_WINDSPEED) * 2.23693629 * 1000.0)/1000.0;
            if(MainActivity.isMph == false)
                speed = Math.round(speed * 1.609344 * 1000.0)/1000.0;
            String precipitation = "";
            boolean hasPrecipitation = false;
            for (Iterator<String> iter = dayForecast.keys(); iter.hasNext(); ) {
                String key = iter.next();
                if (key.equals(OWM_RAIN))
                    hasPrecipitation = true;
            }
            if (hasPrecipitation) {
                JSONObject rainObject = dayForecast.getJSONObject(OWM_RAIN);
                for (int d = 6; d < String.valueOf(rainObject).toCharArray().length - 1; d++) {
                    precipitation += String.valueOf(rainObject).charAt(d);
                }
                if(!precipitation.equals("")) {
                    precipitation = String.valueOf(Math.round(Double.parseDouble(precipitation) / 10 * 1000.0)/1000.0);
                    if(MainActivity.isCentimeters == false)
                        precipitation = String.valueOf(Math.round(Double.parseDouble(precipitation) * 0.393700787 * 1000.0)/1000.0);
                }
            }
            double degree = windObject.getDouble(OWM_WIND_DIRECTION);
            Collections.addAll(detailedWeatherShow, cityName, description, String.valueOf(Math.round(temp)), String.valueOf(humidity), String.valueOf(pressure), String.valueOf(speed),
                    precipitation, translateDegree(degree));
        }

        for (String s : resultStrs) {
            Log.v(LOG_TAG, "Forecast entry: " + s);
        }

        return resultStrs;
    }

    @Override
    protected String[] doInBackground(String... params) {
        if (params.length == 0) {
            return null;
        }
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String forecastJsonStr = null;
        String format = "json";
        String units = "metric";
        String appid = "13788bc1ccd9918e4e8b917f3ee37daf";
        int numDays = 7;
        try {
            final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?";
            final String QUERY_PARAM = "q";
            final String APPID_PARAM = "APPID";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";
            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0]) //params[0] refer to the String parameter of AsyncTask
                    .appendQueryParameter(APPID_PARAM, appid)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .build();
            URL url = new URL(builtUri.toString());
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {

                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {

                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {

                return null;
            }
            forecastJsonStr = buffer.toString();
            Log.v(LOG_TAG, "Forecast JSON String: " + forecastJsonStr);
        } catch (IOException e) {
            Log.e("PlaceholderFragment", "Error ", e);

            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }
        try {
            return getWeatherDataFromJson(forecastJsonStr, numDays);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;

    }

    @Override
    protected void onPostExecute(String[] result) {
        if (result != null) {
            adapter.clear();
            adapter.add(findWeeklyDescription());
            cityText.setText(detailedWeatherShow.get(0));
            descriptionText.setText(capitaliseFirstLetter(detailedWeatherShow.get(1)));
            setIcon(icon, descriptionText.getText().toString());
            temperatureText.setText(detailedWeatherShow.get(2) + "°");
            humidityText.append(detailedWeatherShow.get(3) + "%");
            pressureText.append(detailedWeatherShow.get(4) + " hPa");
            windSpeedText.append(detailedWeatherShow.get(7) + " " + detailedWeatherShow.get(5));
            if(MainActivity.isMph)
                windSpeedText.append(" mph");
            else
                windSpeedText.append(" kph");
            if (detailedWeatherShow.get(6).isEmpty()) {
                if(MainActivity.isCentimeters)
                    precipitationText.append(" 0.0 cm");
                else
                    precipitationText.append(" 0.0 in");
            }
            else {
                if(MainActivity.isCentimeters)
                    precipitationText.append(detailedWeatherShow.get(6) + " cm");
                else
                    precipitationText.append(detailedWeatherShow.get(6) + " in");
            }
            for (String dayForecastStr : result) {
                adapter.add(dayForecastStr);
            }
        }
    }

    public void setIcon(ImageView icon, String desc){
        if(desc.contains("clear") || desc.contains("Clear"))
            icon.setImageResource(R.drawable.clear);
        else if (desc.contains("cloud") ||desc.contains("calm") || desc.contains("clouds"))
            icon.setImageResource(R.drawable.cloud);
        else if (desc.contains("sun"))
            icon.setImageResource(R.drawable.sunny);
        else if (desc.contains("rain") || desc.contains("drizzle"))
            icon.setImageResource(R.drawable.rain);
        else if (desc.contains("thunderstorm"))
            icon.setImageResource(R.drawable.thunderstorms);
        else if (desc.contains("snow") || desc.contains("sleet"))
            icon.setImageResource(R.drawable.snow);
        else if (desc.contains("fog"))
            icon.setImageResource(R.drawable.fog);
        else if (desc.contains("breeze"))
            icon.setImageResource(R.drawable.wind);
        else if (desc.contains("mist"))
            icon.setImageResource(R.drawable.mist);
    }

    public String findWeeklyDescription() {
        String weeklyDescription = "";
        String minTemp = tempArray.get(0);
        String maxTemp = tempArray.get(0);
        for (int i = 0; i < tempArray.size(); i++) {
            if (Integer.parseInt(tempArray.get(i).split(" ")[0]) > Integer.parseInt(maxTemp.split(" ")[0]))
                maxTemp = tempArray.get(i);
            if (Integer.parseInt(tempArray.get(i).split(" ")[0]) < Integer.parseInt(minTemp.split(" ")[0]))
                minTemp = tempArray.get(i);
        }
        double chance = Math.random() * 100;
        if (chance < 60)
            weeklyDescription += chosenDescription() + R.string.tempmax + maxTemp + ".";
        else
            weeklyDescription += chosenDescription() + R.string.tempdown + minTemp + ".";
        return weeklyDescription;
    }

    public String chosenDescription() {
        ArrayList<String> newArray = new ArrayList<>();
        String weeklyDescription = "";

        for (int i = 0; i < descriptionList.size(); i++) {
            int count = 0;
            for (int j = 0; j < descriptionList.size(); j++) {
                if (descriptionList.get(i).equals(descriptionList.get(j))) {
                    count++;
                }
            }
            removeDuplicate.add(count + " " + descriptionList.get(i));
        }
        Iterator it = removeDuplicate.iterator();
        while (it.hasNext()) {
            newArray.add((String) it.next());
        }
        String maxCount = newArray.get(0);
        if (newArray.size() >= 5) {
            weeklyDescription += R.string.chweat;
        }
        if (newArray.size() > 2 && newArray.size() < 5) {
            for (int i = 0; i < newArray.size(); i++) {
                if (Integer.parseInt(newArray.get(i).split(" ")[0]) > Integer.parseInt(maxCount.split(" ")[0]))
                    maxCount = newArray.get(i);
            }
            weeklyDescription += maxCount.substring(2) + R.string.nextweek;
        }
        if (newArray.size() < 3) {
            for (int i = 0; i < newArray.size(); i++) {
                if (Integer.parseInt(newArray.get(i).split(" ")[0]) > Integer.parseInt(maxCount.split(" ")[0]))
                    maxCount = newArray.get(i);
            }
            weeklyDescription += maxCount.substring(2) + R.string.sevenday;
        }
        return capitaliseFirstLetter(weeklyDescription);
    }

    public String capitaliseFirstLetter(String string) {
        String result = string.substring(0, 1).toUpperCase() + string.substring(1).toLowerCase();

        return result;
    }
    public double convertMetricToImperial(double metricTemp) {
        metricTemp = (metricTemp * 1.8) + 32;
        return metricTemp;
    }
}
