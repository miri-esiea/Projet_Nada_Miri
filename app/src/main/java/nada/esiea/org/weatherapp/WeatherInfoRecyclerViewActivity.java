package nada.esiea.org.weatherapp;



import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by medhy.
 */
public class WeatherInfoRecyclerViewActivity extends RecyclerView.Adapter
        <WeatherInfoRecyclerViewActivity.WeatherInfoViewHolder> {

    protected List<WeatherInfoActivity> mWeatherLists;
    private static OnRecyclerItemClickListener listener;
    private static OnRecyclerItemLongClickListener longClickListener;

    private String lastUpdateTime = "11:02 AM";

    private static final String LOG_TAG = WeatherInfoRecyclerViewActivity.class.getSimpleName();

    public WeatherInfoRecyclerViewActivity(List<WeatherInfoActivity> weatherLists) {
        mWeatherLists = weatherLists;
    }

    /**
     * called when the WeatherInfoViewHolder need to be initialised
     * only called once and reuse it to improve performance
     */
    @Override
    public WeatherInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.v("viewHolder", "onCreateViewHolder called");
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.cardview_item, parent, false);

        return new WeatherInfoViewHolder(itemView);
    }

    /**
     * called when the views bind with data
     * using this ViewHolder pattern avoid looking up UI with findViewById all the time
     */
    @Override
    public void onBindViewHolder(WeatherInfoViewHolder holder, int position) {
        Log.v("viewHolder", "onBindViewHolder called");
        WeatherInfoActivity weatherInfo = mWeatherLists.get(position);
        holder.mLocation.setText(weatherInfo.currentLocation);
        holder.mDescription.setText(weatherInfo.weatherDescription);
        holder.mTemperature.setText(weatherInfo.temperature + "°");
        holder.mUpdateTime.setText(lastUpdateTime);

        setIcon(holder.mIcon, weatherInfo.weatherDescription);
    }



    public void setIcon(ImageView icon, String desc){
        if(desc.contains("clear"))
            icon.setImageResource(R.drawable.clear);
        else if (desc.contains("cloud") ||desc.contains("calm") || desc.contains("clouds"))
            icon.setImageResource(R.drawable.cloud);
        else if (desc.contains("sun") || desc.contains("sleet"))
            icon.setImageResource(R.drawable.sunny);
        else if (desc.contains("rain") || desc.contains("drizzle"))
            icon.setImageResource(R.drawable.rain);
        else if (desc.contains("thunderstorm"))
            icon.setImageResource(R.drawable.thunderstorms);
        else if (desc.contains("snow"))
            icon.setImageResource(R.drawable.snow);
        else if (desc.contains("fog"))
            icon.setImageResource(R.drawable.fog);
        else if (desc.contains("breeze"))
            icon.setImageResource(R.drawable.wind);
        else if (desc.contains("mist"))
            icon.setImageResource(R.drawable.mist);
    }

    /**
     *
     */
    @Override
    public int getItemCount() {
        return mWeatherLists.size();
    }


    public interface OnRecyclerItemClickListener {
        void onRecyclerItemClick(View view, int position);
    }

    public interface OnRecyclerItemLongClickListener {
        void onRecyclerItemLongClick(View view, int position);
    }

    public void attachRecyclerItemClickListener(OnRecyclerItemClickListener listener) {
        WeatherInfoRecyclerViewActivity.listener = listener;
    }

    public void attachRecyclerItemLongClickListener(OnRecyclerItemLongClickListener listener) {
        WeatherInfoRecyclerViewActivity.longClickListener = listener;
    }

    public List<WeatherInfoActivity> getList() {
        return this.mWeatherLists;
    }

    public void updateWeatherList(List<WeatherInfoActivity> weatherLists) {
        mWeatherLists = weatherLists;
    }

    public void setLastUpdateTime(String lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public static class WeatherInfoViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, View.OnLongClickListener{

        protected TextView mLocation;
        protected TextView mDescription;
        protected TextView mTemperature;
        protected TextView mUpdateTime;
        protected ImageView mIcon;
        public WeatherInfoViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            mLocation = (TextView) itemView.findViewById(R.id.tv_location);
            mDescription = (TextView) itemView.findViewById(R.id.tv_description);
            mTemperature = (TextView) itemView.findViewById(R.id.tv_temperature);
            mUpdateTime = (TextView) itemView.findViewById(R.id.tv_updateTime);
            mIcon = (ImageView) itemView.findViewById(R.id.imageView);

        }

        /**
         * start detailActivity
         *
         */
        @Override
        public void onClick(View v) {
            Log.d(LOG_TAG, String.valueOf(getAdapterPosition()));
            listener.onRecyclerItemClick(v, getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            longClickListener.onRecyclerItemLongClick(v, getAdapterPosition());
            return false;
        }
    }

}
