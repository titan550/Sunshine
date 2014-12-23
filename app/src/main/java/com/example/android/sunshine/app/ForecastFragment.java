package com.example.android.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

import java.util.ArrayList;
import java.util.Date;


public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    public static final int COL_WEATHER_ID = 6;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_DESC = 2;
    public static final int COL_WEATHER_MAX_TEMP = 3;
    public static final int COL_WEATHER_MIN_TEMP = 4;
    public static final int COL_LOCATION_SETTING = 5;
    private static final int FORECAST_LOADER = 0; //A Fragment can have multiple loaders and you specify a number for each of them.
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATETEXT,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID
    };
    private String mLocation;
    private ForecastAdapter mForecastAdapter;


    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(FORECAST_LOADER, null, this); //start loader
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // if not indicated the options will not be added to menu
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // every time the Fragment is resumed, it checks if location setting
        // is changed, because it might had been paused after user goes to settings
        // activity and after user comes back, they might have changed location settings
        if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity())))
        {
            getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    // runs the fetchWeatherTask to get data from API and insert into db, has nothing to do with data shown to user via adapter and listview
    private Void updateWeather()
    {
        String zipCode = Utility.getPreferredLocation(getActivity());
        FetchWeatherTask fetch = new FetchWeatherTask(getActivity());
        // runs onBackground() to get json data from API and then pass to json-parser to send to db through URI
        fetch.execute(zipCode);
        return null;
    }

    // controls the menu items action(top right corner menu)
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == R.id.action_refresh)
        {
            updateWeather();
        }
        return super.onOptionsItemSelected(item);
    }

    // ?? what view is meant here?
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ArrayList<String> weekForecast = new ArrayList<String>();

        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);


        ListView myList = (ListView) rootView.findViewById(R.id.listview_forecast);
        myList.setAdapter(mForecastAdapter);

        myList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l)
            {
                Cursor cursor = mForecastAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position))
                {
                    String dateString = cursor.getString(COL_WEATHER_DATE);
                    Intent intent = new Intent(getActivity(), DetailActivity.class).putExtra(DetailActivity.DATE_KEY, dateString);
                    startActivity(intent);
                }
            }
        });

        return rootView;
    }

    // ?? creates the right URI to fetch weather through content provider and ?? then does that
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args)
    {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.

        // To only show current and future dates, get the String representation for today,
        // and filter the query to return weather only for dates after or including today.
        // Only return data after today.
        String startDate = WeatherContract.getDbDateString(new Date());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
                mLocation, startDate);

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(
                getActivity(),
                weatherForLocationUri,
                FORECAST_COLUMNS,
                null,
                null,
                sortOrder
        );
    }

    // when the new data loaded through content provider, it swaps new data to mForecastFragment,
    // so fresh data will be shown in listView(list shown to user)
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data)
    {
        mForecastAdapter.swapCursor(data);
    }

    // ?? when is is called? adds null data to mForecastFragment
    @Override
    public void onLoaderReset(Loader<Cursor> loader)
    {
        mForecastAdapter.swapCursor(null);
    }

}