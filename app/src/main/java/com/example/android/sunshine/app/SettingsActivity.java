package com.example.android.sunshine.app;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.example.android.sunshine.app.data.WeatherContract;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{
    private boolean mBindingPreference;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Add 'general' preferences, defined in the XML file
        addPreferencesFromResource(R.xml.pref_general);

        // For all preferences, attach an OnPreferenceChangeListener so the UI summary can be
        // updated when the preference changes.
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)));
    }

    /**
     * Attaches a listener so the summary is always updated with the preference value.
     * Also fires the listener once, to initialize the summary (so it shows up before the value
     * is changed.)
     */
    private void bindPreferenceSummaryToValue(Preference preference)
    {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's
        // current value.

        // for onPreferenceChange() to know if a value is changed or it's
        // just called to fill the initial values for details, we load mBindingPreference
        mBindingPreference = true;
        onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
        mBindingPreference = false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value)
    {
        String stringValue = value.toString();

        // if the preference is of list type ( choosing between Metric and Imperial for example )
        if (preference instanceof ListPreference)
        {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list (since they have separate labels/values).
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(stringValue);
            if (prefIndex >= 0)
            {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        }
        // if the preference is of other types, such as in our case "String" for zip code or city name
        else
        {
            // For other preferences, set the summary to the value's simple string representation.
            preference.setSummary(stringValue);
        }

        // are we starting the preference activity? or preference is actually changed/edited with same value??
        if (!mBindingPreference)
        {
            // if it is the location that is changed
            if (preference.getKey().equals(getString(R.string.pref_location_key)))
            {
                // a new FetchWeatherTask is created and data for new location is inserted to db
                FetchWeatherTask weatherTask = new FetchWeatherTask(this);
                String location = value.toString();
                weatherTask.execute(location);
                // the execution process updates the db values and notifies code of  new changes (for content listeners)
            }
            //  other things have changed? ( Imperial or Metric )
            else
            {
                // notify code that the unit is changed so temperature unit should be adjusted
                getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
            }
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent()
    {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

}