package nada.esiea.org.weatherapp;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Placement des variables dans les zones prevu dans les fragments
 */
public class FragmentDetailActivity extends Fragment {

    public FragmentDetailActivity() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        ArrayList<String> weatherString = new ArrayList<>();

        WeatherTaskActivity weatherTask = new WeatherTaskActivity();

        weatherTask.icon = (ImageView) rootView.findViewById(R.id.imageView2);
        weatherTask.cityText = (TextView) rootView.findViewById(R.id.detailed_location);
        weatherTask.humidityText = (TextView) rootView.findViewById(R.id.detailed_humidity);
        weatherTask.pressureText = (TextView) rootView.findViewById(R.id.detailed_pressure);
        weatherTask.descriptionText = (TextView) rootView.findViewById(R.id.detailed_description);
        weatherTask.windSpeedText = (TextView) rootView.findViewById(R.id.detailed_wind);
        weatherTask.precipitationText = (TextView) rootView.findViewById(R.id.detailed_precipitation);
        weatherTask.temperatureText = (TextView) rootView.findViewById(R.id.detailed_temperature);
        weatherTask.adapter = new ArrayAdapter<>(getActivity(), R.layout.list_item_forecast,
                R.id.list_item_forecast_textview, weatherString);


        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(weatherTask.adapter);
        Intent intent = getActivity().getIntent();
        String foreCastString = intent.getStringExtra(Intent.EXTRA_TEXT);
        weatherTask.execute(foreCastString);
        return rootView;
    }
}