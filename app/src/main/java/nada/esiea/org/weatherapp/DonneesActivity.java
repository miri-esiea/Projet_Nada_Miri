package nada.esiea.org.weatherapp;


import java.util.ArrayList;
import java.util.List;

import nada.esiea.org.weatherapp.WeatherInfoActivity;

/**
 * Created by Medhy.
 * recuperation de la data dans un format precis
 */
public class DonneesActivity {

    protected static List<WeatherInfoActivity> createDummyWeatherData(ArrayList<String> places) {
        List<WeatherInfoActivity> data = new ArrayList<>();

        for (String place : places) {
            data.add(new WeatherInfoActivity(place));
        }

        return data;
    }
}
