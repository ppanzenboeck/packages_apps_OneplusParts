/*
 * Copyright (C) 2016 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.oneplusparts;

import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;

import java.io.IOException;

public class DisplayColorModeSwitch implements OnPreferenceChangeListener {
    private static final String LOG_TAG = "DeviceSettings";
    private final Context mContext;

    public DisplayColorModeSwitch(Context context) {
        mContext = context;
    }

    public static void updateDisplayColorMode(boolean enabled) {
        // Define saturation value
        String colorValue = "1.0";
        if (enabled) colorValue = "0.8";

        // Set new color
        try {
            Runtime.getRuntime().exec("service call SurfaceFlinger 1022 f " + colorValue);
            Log.d(LOG_TAG, "Applying new display saturation: " + colorValue);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Failed to apply new display saturation");
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Boolean enabled = (Boolean) newValue;

        if (preference == DeviceSettings.mDisplayColorSwitch) {
            Utils.setBooleanProp(DeviceSettings.DISPLAY_COLOR_SYSTEM_PROPERTY, enabled);
            updateDisplayColorMode(enabled);
        }

        return true;
    }

}
