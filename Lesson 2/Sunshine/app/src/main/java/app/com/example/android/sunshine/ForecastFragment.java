package app.com.example.android.sunshine;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    ArrayAdapter<String> mForecastAdapter;
    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Now in order to handle menu events we have to add the following line
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater)
    {
//        menuInflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem)
    {
        //Action Bar clicks are handled here. The action bar automatically handles clicks
        //on Home/Up button, as long as it is specified as a parent activity in
        //AndroidManifest.XML
        int id = menuItem.getItemId();
        if (id == R.id.action_refresh)
        {
            FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
            fetchWeatherTask.execute("122017");
            return true;
        }

        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.fragment_main, container, false);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
//        String[] forecastArray = {
//            "Today - Sunny - 88/63",
//            "Tomorrow - Foggy - 70/40",
//            "Weds - Cloudy - 72/63",
//            "Thurs - Asteroids - 75/65",
//            "Fri - Heavy Rain - 65/56",
//            "Sat - Storm - 60/51",
//            "Sun - Sunny - 80/68"
//        };
//
//        List<String> weekForecast  = new ArrayList<String>(Arrays.asList(forecastArray));
        List<String> weekForecast = new ArrayList<>();
        FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
        fetchWeatherTask.execute("122017");
        // To initialize an adapter with the dummy data of the arrayList- weekForecast
        mForecastAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekForecast);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);


        return rootView;
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
    * so for convenience we're breaking it out into its own method now.
    */
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private String getReadableDateString(long time){
      // Because the API returns a unix timestamp (measured in seconds),
      // it must be converted to milliseconds in order to be converted to valid date.
      SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
      return shortenedDateFormat.format(time);
    }

  /**
   * Prepare the weather high/lows for presentation.
   */
  private String formatHighLows(double high, double low) {
    // For presentation, assume the user doesn't care about tenths of a degree.
    long roundedHigh = Math.round(high);
    long roundedLow = Math.round(low);

    String highLowStr = roundedHigh + "/" + roundedLow;
    return highLowStr;
  }

  /**
   * Take the String representing the complete forecast in JSON Format and
   * pull out the data we need to construct the Strings needed for the wireframes.
   *
   * Fortunately parsing is easy:  constructor takes the JSON string and converts it
   * into an Object hierarchy for us.
   */
  private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
      throws JSONException {

    // These are the names of the JSON objects that need to be extracted.
    final String OWM_LIST = "list";
    final String OWM_WEATHER = "weather";
    final String OWM_TEMPERATURE = "temp";
    final String OWM_MAX = "max";
    final String OWM_MIN = "min";
    final String OWM_DESCRIPTION = "main";

    JSONObject forecastJson = new JSONObject(forecastJsonStr);
    JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

    // OWM returns daily forecasts based upon the local time of the city that is being
    // asked for, which means that we need to know the GMT offset to translate this data
    // properly.

    // Since this data is also sent in-order and the first day is always the
    // current day, we're going to take advantage of that to get a nice
    // normalized UTC date for all of our weather.

    Time dayTime = new Time();
    dayTime.setToNow();

    // we start at the day returned by local time. Otherwise this is a mess.
    int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

    // now we work exclusively in UTC
    dayTime = new Time();

    String[] resultStrs = new String[numDays];
    for(int i = 0; i < weatherArray.length(); i++) {
      // For now, using the format "Day, description, hi/low"
      String day;
      String description;
      String highAndLow;

      // Get the JSON object representing the day
      JSONObject dayForecast = weatherArray.getJSONObject(i);

      // The date/time is returned as a long.  We need to convert that
      // into something human-readable, since most people won't read "1400356800" as
      // "this saturday".
      long dateTime;
      // Cheating to convert this to UTC time, which is what we want anyhow
      dateTime = dayTime.setJulianDay(julianStartDay+i);
      day = getReadableDateString(dateTime);

      // description is in a child array called "weather", which is 1 element long.
      JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
      description = weatherObject.getString(OWM_DESCRIPTION);

      // Temperatures are in a child object called "temp".  Try not to name variables
      // "temp" when working with temperature.  It confuses everybody.
      JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
      double high = temperatureObject.getDouble(OWM_MAX);
      double low = temperatureObject.getDouble(OWM_MIN);

      highAndLow = formatHighLows(high, low);
      resultStrs[i] = day + " - " + description + " - " + highAndLow;
    }

    for (String s : resultStrs) {
      Log.v(LOG_TAG, "Forecast entry: " + s);
    }
    return resultStrs;

  }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]>
    {


        @Override
        protected String[] doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader bufferedReader = null;

            //JSON response in here
            String JSONstr = "", numDays="7";
            try {
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=122017&mode=json&units=metric&cnt=7");
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";

                Uri builtURI = Uri.parse(FORECAST_BASE_URL).buildUpon().appendQueryParameter(QUERY_PARAM, params[0]).
                               appendQueryParameter(FORMAT_PARAM, "json").appendQueryParameter(UNITS_PARAM, "metric").
                               appendQueryParameter(DAYS_PARAM, numDays).build();

                URL url = new URL(builtURI.toString());
                // Log.v("HELLO My built URL is", "HELLO My built URL is "+url);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                //Connect is a void method. Don't get confused again!!!
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer stringBuffer = new StringBuffer();
                if (inputStream == null)
                {
                    return null;
                }

                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line ;
                while ((line = bufferedReader.readLine()) != null) {

                    stringBuffer.append(line + "\n");
                }

                if(stringBuffer.length()==0)
                    return null;

                JSONstr = stringBuffer.toString();
                //Log.v(LOG_TAG, "JSON Created is: "+JSONstr);
            }
            catch (Exception e)
            {
                Log.e(LOG_TAG, "IOException Mostly", e);
            }
            finally {
                try
                {
                    bufferedReader.close();
                }
                catch (IOException e)
                {
                    Log.e(LOG_TAG, "For bufferedReader", e);
                }

                urlConnection.disconnect();
            }

            try{
              return getWeatherDataFromJson( JSONstr, Integer.parseInt(numDays));
            }catch (JSONException e)
            {
              Log.e(LOG_TAG, e.getMessage(), e);
              e.printStackTrace();
            }
            return null;
        }

      @Override
      protected void onPostExecute(String[] result)
      {
        if(result != null)
        {
          mForecastAdapter.clear();
          for (String dayForecast : result)
          {
            mForecastAdapter.add(dayForecast);
          }
        }
      }
    }
}