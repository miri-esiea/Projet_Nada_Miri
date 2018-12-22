package nada.esiea.org.weatherapp;


/**
 * Created by Medhy
 */
public class WeatherInfoActivity {

    protected String currentLocation;
    protected String weatherDescription;
    protected String temperature;

    public WeatherInfoActivity(String currentLocation, String weatherDescription, String temperature) {
        this.currentLocation = currentLocation;
        this.weatherDescription = weatherDescription;
        this.temperature = temperature;
    }

    public WeatherInfoActivity(String currentLocation) {
        this.currentLocation = currentLocation;
        weatherDescription = "clear";
        temperature = "11";
    }
}