package nada.esiea.org.weatherapp;


import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.Time;
import android.transition.Slide;
import android.transition.TransitionInflater;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;

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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener,
        WeatherInfoRecyclerViewActivity.OnRecyclerItemClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, WeatherInfoRecyclerViewActivity.OnRecyclerItemLongClickListener {

    /**
     * code pour autocompletion
     */
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private WeatherInfoRecyclerViewActivity recyclerAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private FetchCurrentWeatherTask db;
    private ArrayList<String> places;
    private SimpleDateFormat sdf;
    private Date lastUpdateTime;

    protected static boolean isMetric;
    protected static boolean isCentimeters;
    protected static boolean isMph;

    private static View view;
    /**
     * generation de l'indexage avec api
     */
    private GoogleApiClient client;

    private GoogleApiClient mLocationClient;
    private double lat;
    private double longg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sdf = new SimpleDateFormat("HH:mm a", Locale.getDefault());

        places = readFromSharedPref();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openAutocompleteActivity();
                }
            });
        }

        //rafraichissement de layout
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        assert swipeRefreshLayout != null;
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent,
                R.color.colorPrimary, R.color.colorAccent);

        //mise en placd du RecyclerView
        recyclerView = (RecyclerView) findViewById(R.id.cardList);
        assert recyclerView != null;
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        //creation et application du RecyclerView

        recyclerAdapter = new WeatherInfoRecyclerViewActivity(DonneesActivity.createDummyWeatherData(places));
        recyclerAdapter.attachRecyclerItemClickListener(this);
        recyclerAdapter.attachRecyclerItemLongClickListener(this);
        recyclerView.setAdapter(recyclerAdapter);

        //mise a jour date et temps
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        lastUpdateTime = cal.getTime();
        recyclerAdapter.setLastUpdateTime(getCurrentTime());

        //emplacement du client grace a son GPS

        mLocationClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mLocationClient.connect();

        getUnitType();

        //requete pou recuperer la position

        db = new FetchCurrentWeatherTask(1);
        startRefreshFromNewThread();
        db.execute(places);

        // indexage api

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    protected void saveToSharedPref(List<String> list) {
        SharedPreferences sharedPref = getSharedPreferences(
                "city_sharePref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.clear();
        for (String city : places) {
            editor.putString(city, city);
        }
        editor.putBoolean("first_time", false);
        editor.commit();
    }








    protected ArrayList<String> readFromSharedPref() {
        ArrayList<String> cities = new ArrayList<>();

        SharedPreferences sharedPref = getSharedPreferences(
                "city_sharePref", Context.MODE_PRIVATE);
        if (sharedPref.getBoolean("first_time", true)) {
            Log.e("sharep", "true");
            cities.add(getString(R.string.cities));
            sharedPref.edit().putBoolean("first_time", false).commit();
        }
        else {
            Log.e("sharep", "false");
            Set<String> allValues= sharedPref.getAll().keySet();
            for (String city : allValues) {
                if (!city.equals("first_time")) {
                    cities.add(city);
                }
            }
        }
        return cities;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupWindowAnimations() {
        Slide slide = (Slide) TransitionInflater.from(this).inflateTransition(R.transition.activity_slide);
        getWindow().setExitTransition(slide);
    }

    //-----------------------------------GPS METHODS----------------------------------------------------
    public void showCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Permissions
            return;
        }
        Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mLocationClient);
        if(currentLocation == null){
            Toast.makeText(this, ""+getString(R.string.gpson)+"", Toast.LENGTH_SHORT).show();
        } else {
            lat = currentLocation.getLatitude();
            longg = currentLocation.getLongitude();
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, longg, 1);
                String city = addresses.get(0).getLocality();
                if (!places.contains(city)) {
                    places.add(0, city);
                    db = new FetchCurrentWeatherTask(3);
                    startRefreshing();
                    db.execute(places);
                } else {
                    Snackbar.make(recyclerView, city + ""+getString(R.string.haup)+"", Snackbar.LENGTH_LONG).show();
                }
                Toast.makeText(this, ""+getString(R.string.ycl)+"" + city, Toast.LENGTH_SHORT).show();
            } catch (IOException e){
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        startRefreshing();
        Log.d("GPS", "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("GPS", "onConnectionFailed");
        db = new FetchCurrentWeatherTask(1);
        db.execute(places);
    }






    private void openAutocompleteActivity() {
        try {
            // The autocomplete activity requires Google Play Services to be available. The intent
            // builder checks this and throws an exception if it is not the case.
            AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                    .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                    .build();
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                    .setFilter(typeFilter).build(this);
            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
        } catch (GooglePlayServicesRepairableException e) {
            // Indicates that Google Play Services is either not installed or not up to date. Prompt
            // the user to correct the issue.
            GoogleApiAvailability.getInstance().getErrorDialog(this, e.getConnectionStatusCode(),
                    0 /* requestCode */).show();
        } catch (GooglePlayServicesNotAvailableException e) {
            // Indicates that Google Play Services is not available and the problem is not easily
            // resolvable.
            String message = ""+getString(R.string.errorgoogle)+"" +
                    GoogleApiAvailability.getInstance().getErrorString(e.errorCode);

            Log.e(LOG_TAG, message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *
     renvoie le result de l'autocompletion

     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check that the result was from the autocomplete widget.
        if (requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            if (resultCode == RESULT_OK) {
                // Get the user's selected place from the Intent.
                Place place = PlaceAutocomplete.getPlace(this, data);
                String cityName = place.getName().toString();
                if (!places.contains(cityName)) {
                    places.add(cityName);
                    db = new FetchCurrentWeatherTask(2);
                    startRefreshing();
                    db.execute(places);
                } else {
                    Snackbar.make(recyclerView, cityName + ""+getString(R.string.haup)+"", Snackbar.LENGTH_LONG).show();
                }

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.e(LOG_TAG, "Error: Status = " + status.toString());
            } else if (resultCode == RESULT_CANCELED) {

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();


        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        String[] langArray = new String[]{""+getString(R.string.fr)+"", ""+getString(R.string.en)+"", ""+getString(R.string.tr)+""};
        view = findViewById(R.id.card_view);


        if (id == R.id.languages) {
            alert.setSingleChoiceItems(langArray, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which == 0) {
                        // Resources res = context.getResources();
// Change locale settings in the app.
                        // DisplayMetrics dm = res.getDisplayMetrics();
                        // android.content.res.Configuration conf = res.getConfiguration();
                        // conf.setLocale(new Locale(language_code.toLowerCase())); // API 17+ only.
// Use conf.locale = new Locale(...) if targeting lower versions
                        //res.updateConfiguration(conf, dm);
                    }
                    if(which == 1){
                        //    Resources res = context.getResources();
// Change locale settings in the app.
                        //  DisplayMetrics dm = res.getDisplayMetrics();
                        // android.content.res.Configuration conf = res.getConfiguration();
                        //    conf.setLocale(new Locale(language_code.toLowerCase())); // API 17+ only.
// Use conf.locale = new Locale(...) if targeting lower versions
                        //  res.updateConfiguration(conf, dm);
                    }
                    if(which == 2){
                        //  Resources res = context.getResources();
// Change locale settings in the app.
                        //    DisplayMetrics dm = res.getDisplayMetrics();
                        //  android.content.res.Configuration conf = res.getConfiguration();
                        //   conf.setLocale(new Locale(language_code.toLowerCase())); // API 17+ only.
// Use conf.locale = new Locale(...) if targeting lower versions
                        //   res.updateConfiguration(conf, dm);
                    }

                }
            }).setCancelable(false).setPositiveButton(""+getString(R.string.next)+"", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).setTitle(""+getString(R.string.chooselang)+"").create().show();
        }


        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_location) {
            showCurrentLocation();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * rafraichissement des données
     */
    @Override
    public void onRefresh() {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        Date updateTime = cal.getTime();

        long timeDifference = TimeUnit.MILLISECONDS.toMinutes(updateTime.getTime() - lastUpdateTime.getTime());

        //excecusion automatique du refresh apres 10 min sans refresh
        if (timeDifference < 1) {
            swipeRefreshLayout.setRefreshing(false);
            Snackbar.make(recyclerView, ""+getString(R.string.snackupdate)+"",
                    Snackbar.LENGTH_SHORT).show();
        } else {
            lastUpdateTime = updateTime;
            recyclerAdapter.setLastUpdateTime(getCurrentTime());
            db = new FetchCurrentWeatherTask(1);
            startRefreshing();
            db.execute(places);
        }
    }

    private void startRefreshFromNewThread() {
        swipeRefreshLayout.post(new Runnable() {

            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });
    }

    private void startRefreshing() {
        swipeRefreshLayout.setRefreshing(true);
    }

    private void stopRefreshing(int source) {
        swipeRefreshLayout.setRefreshing(false);
        if (source == 1) {
            Snackbar.make(recyclerView, ""+getString(R.string.snackupdate)+"", Snackbar.LENGTH_SHORT).show();
        } else if (source == 2) {
            Snackbar.make(recyclerView, places.get(places.size() - 1) +
                    ""+getString(R.string.placeexist)+"", Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(recyclerView, places.get(0) +
                    ""+getString(R.string.placeexist)+"", Snackbar.LENGTH_SHORT).show();
        }
    }

    public String getCurrentTime() {
        String currentTime;
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        currentTime = sdf.format(cal.getTime());
        return currentTime;
    }

    @Override
    public void onRecyclerItemClick(View view, int position) {
        WeatherInfoActivity weatherInfoString = recyclerAdapter.getList().get(position);
        String locationString = weatherInfoString.currentLocation;
        Intent intent = new Intent(this, DetailActivity.class).putExtra(Intent.EXTRA_TEXT, locationString);
        startActivity(intent);
        Log.d("listener", ""+getString(R.string.clickat)+"" + position);
    }

    @Override
    public void onRecyclerItemLongClick(View view, int position) {
        Log.d("listener", ""+getString(R.string.lclickat)+"" + position);
        setUpYesNoDialog(position);
    }

    public void setUpYesNoDialog(final int position) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int choice) {
                switch (choice) {
                    case DialogInterface.BUTTON_POSITIVE:
                        recyclerAdapter.mWeatherLists.remove(position);
                        places.remove(position);
                        recyclerAdapter.notifyItemRemoved(position);
                        recyclerAdapter.notifyItemRangeChanged(position, recyclerAdapter.mWeatherLists.size());
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(""+getString(R.string.deletecity)+"")
                .setPositiveButton(""+getString(R.string.yes)+"", dialogClickListener)
                .setNegativeButton(""+getString(R.string.no)+"", dialogClickListener).show();
    }

    private static String getReadableDateString(long time) {
        // Conversion de la date en miliseconde , l'api prends en mode unix
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    public void getUnitType() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String unitType = sharedPrefs.getString(getString(R.string.pref_units_key), getString(R.string.pref_units_metric));
        if (unitType.equals(getString(R.string.pref_units_imperial))) {
            isMetric = false;
        } else
            isMetric = true;
        String preUnitType = sharedPrefs.getString(getString(R.string.pref_preunits_key), getString(R.string.pref_preunits_cm));
        if (preUnitType.equals(getString(R.string.pref_preunits_inch))) {
            isCentimeters = false;
        } else
            isCentimeters = true;
        String windUnitType = sharedPrefs.getString(getString(R.string.pref_windunits_key), getString(R.string.pref_windunits_mph));
        if (windUnitType.equals(getString(R.string.pref_windunits_kph))) {
            isMph = false;
        } else
            isMph = true;

    }

    public static String formatTemp(double value) {

        if (!isMetric) {
            value = (value * 1.8) + 32;
        }
        long roundedValue = Math.round(value);
        return String.valueOf(roundedValue);
    }


    // recuperation des donnée meteo

    public static List<String> getCurrentWeather(String forecastJsonStr, int numDays) {
        List<String> data = new ArrayList<>();
        try {
            String OWM_LIST = "list";
            String OWM_WEATHER = "weather";
            String OWM_TEMPERATURE = "main";
            String OWM_TEMP = "temp";
            String OWM_MAX = "temp_max";
            String OWM_MIN = "temp_min";
            String OWM_DESCRIPTION = "description";
            ArrayList<Double> highestTemp = new ArrayList<>();
            ArrayList<Double> lowestTemp = new ArrayList<>();
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
            Time dayTime = new Time();
            dayTime.setToNow();

            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
            // mode UTC
            dayTime = new Time();
            Log.v(LOG_TAG, "Test entry: " + getReadableDateString(dayTime.setJulianDay(julianStartDay)));
            Log.v(LOG_TAG, Long.toString(dayTime.setJulianDay(julianStartDay))); //1458205200
            String[] resultStrs = new String[numDays];
            String day;
            String description;
            String currentTemp;
            JSONObject dayForecast = weatherArray.getJSONObject(0);
            long dateTime;
            dateTime = dayTime.setJulianDay(julianStartDay);
            day = getReadableDateString(dateTime);
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double value = temperatureObject.getDouble(OWM_TEMP);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);
            currentTemp = formatTemp(value);
            String temp = currentTemp;
            data.add(description);
            data.add(temp);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return data;
    }

    @Override
    public void onStart() {
        super.onStart();

        // indexage api

        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://nada.esiea.org.weatherapp/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("log", "onDestroy");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("log", "onStop");
        saveToSharedPref(places);
        // indexage api
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://nada.esiea.org.weatherapp/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    // Requete de recherche d'information

    public class FetchCurrentWeatherTask extends AsyncTask<ArrayList<String>, Void, List<WeatherInfoActivity>> {

        private int source;

        public FetchCurrentWeatherTask(int source) {
            this.source = source;
        }

        @Override
        protected List<WeatherInfoActivity> doInBackground(ArrayList<String>... params) {
            if (params.length == 0) {
                return null;
            }
            ArrayList<String> places = params[0];
            List<WeatherInfoActivity> dummyData = new ArrayList<>();
            for (int i = 0; i <= places.size() - 1; i++) {
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;
                String forecastJsonStr = null;
                String format = "json";
                String units = "metric";
                String appid = "13788bc1ccd9918e4e8b917f3ee37daf";
                int numDays = 1;
                try {
                    final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast?";
                    final String QUERY_PARAM = "q";
                    final String APPID_PARAM = "APPID";
                    final String FORMAT_PARAM = "mode";
                    final String UNITS_PARAM = "units";
                    final String DAYS_PARAM = "cnt";
                    Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                            .appendQueryParameter(QUERY_PARAM, String.valueOf(places.get(i)))
                            .appendQueryParameter(APPID_PARAM, appid)
                            .appendQueryParameter(FORMAT_PARAM, format)
                            .appendQueryParameter(UNITS_PARAM, units)
                            .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                            .build();
                    URL url = new URL(builtUri.toString());
                    //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast?q=Paris&APPID=13788bc1ccd9918e4e8b917f3ee37daf&mode=json&units=metric&cnt=7");
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
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

                List<String> info = getCurrentWeather(forecastJsonStr, numDays);
                dummyData.add(new WeatherInfoActivity(places.get(i), info.get(0), info.get(1)));
            }
            return dummyData;
        }

        @Override
        protected void onPostExecute(List<WeatherInfoActivity> dummyData) {
            if (dummyData != null) {
                recyclerAdapter.updateWeatherList(dummyData);
                recyclerAdapter.notifyDataSetChanged();
                stopRefreshing(source);
            }
        }
    }
}