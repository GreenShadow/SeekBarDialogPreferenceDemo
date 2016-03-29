package com.greenshadow.seekbardialogpreference;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * @author GreenShadow
 */
public class TestPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.test_preference);
    }
}
