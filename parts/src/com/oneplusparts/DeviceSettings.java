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

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;
import androidx.preference.TwoStatePreference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;

public class DeviceSettings extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    public static final String KEY_HBM_SWITCH = "hbm";
    public static final String KEY_OTG_SWITCH = "otg";
    public static final String KEY_VIBRATION_STRENGTH = "vibration_strength";
    public static final String VIB_STRENGTH_SYSTEM_PROPERTY = "persist.vib_strength";
    public static final String KEY_DISPLAY_COLOR_SWITCH = "low_saturation_mode";
    public static final String DISPLAY_COLOR_SYSTEM_PROPERTY = "persist.sys.low_saturation_mode";
    public static final String KEY_CHARGING_SWITCH = "smart_charging";
    public static final String KEY_CHARGING_SPEED = "charging_speed";
    public static final String KEY_RESET_STATS = "reset_stats";
    public static final String KEY_SETTINGS_PREFIX = "device_setting_";
    public static final String TP_DIRECTION = "/proc/touchpanel/oplus_tp_direction";
    private static final String ProductName = Utils.ProductName();
    private static final String KEY_CATEGORY_CHARGING = "charging";
    private static final String KEY_CATEGORY_GRAPHICS = "graphics";
    private static final String KEY_CATEGORY_REFRESH_RATE = "refresh_rate";
    public static SecureSettingListPreference mChargingSpeed;
    public static TwoStatePreference mResetStats;
    public static TwoStatePreference mRefreshRate90Forced;
    public static SeekBarPreference mSeekBarPreference;
    public static TwoStatePreference mDisplayColorSwitch;
    public static DisplayManager mDisplayManager;
    private static NotificationManager mNotificationManager;
    public PreferenceCategory mPreferenceCategory;
    private Vibrator mVibrator;
    private SecureSettingListPreference mVibStrength;
    private TwoStatePreference mHBMModeSwitch;
    private TwoStatePreference mOTGModeSwitch;
    private TwoStatePreference mSmartChargingSwitch;
    private boolean HBM_DeviceMatched;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        prefs.edit().putString("ProductName", ProductName).apply();

        addPreferencesFromResource(R.xml.main);

        mOTGModeSwitch = (TwoStatePreference) findPreference(KEY_OTG_SWITCH);
        mOTGModeSwitch.setEnabled(OTGModeSwitch.isSupported());
        mOTGModeSwitch.setChecked(OTGModeSwitch.isCurrentlyEnabled(this.getContext()));
        mOTGModeSwitch.setOnPreferenceChangeListener(new OTGModeSwitch());

        mSmartChargingSwitch = findPreference(KEY_CHARGING_SWITCH);
        mSmartChargingSwitch.setChecked(prefs.getBoolean(KEY_CHARGING_SWITCH, false));
        mSmartChargingSwitch.setOnPreferenceChangeListener(new SmartChargingSwitch(getContext()));

        mChargingSpeed = findPreference(KEY_CHARGING_SPEED);
        mChargingSpeed.setEnabled(mSmartChargingSwitch.isChecked());
        mChargingSpeed.setOnPreferenceChangeListener(this);

        mResetStats = findPreference(KEY_RESET_STATS);
        mResetStats.setChecked(prefs.getBoolean(KEY_RESET_STATS, false));
        mResetStats.setEnabled(mSmartChargingSwitch.isChecked());
        mResetStats.setOnPreferenceChangeListener(this);

        mSeekBarPreference = findPreference("seek_bar");
        mSeekBarPreference.setEnabled(mSmartChargingSwitch.isChecked());
        SeekBarPreference.mProgress = prefs.getInt("seek_bar", 95);

        mRefreshRate90Forced = findPreference("refresh_rate_90Forced");
        mRefreshRate90Forced.setChecked(prefs.getBoolean("refresh_rate_90Forced", false));
        mRefreshRate90Forced.setOnPreferenceChangeListener(new RefreshRateSwitch(getContext()));

        mVibStrength = (SecureSettingListPreference) findPreference(KEY_VIBRATION_STRENGTH);
        mVibStrength.setValue(Utils.getStringProp(VIB_STRENGTH_SYSTEM_PROPERTY, "0"));
        mVibStrength.setSummary(mVibStrength.getEntry());
        mVibStrength.setOnPreferenceChangeListener(this);

        mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        mDisplayColorSwitch = findPreference(KEY_DISPLAY_COLOR_SWITCH);
        mDisplayColorSwitch.setChecked(Utils.getBooleanProp(DISPLAY_COLOR_SYSTEM_PROPERTY, false));
        mDisplayColorSwitch.setOnPreferenceChangeListener(new DisplayColorModeSwitch(getContext()));

        isCoolDownAvailable();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
    
        if (preference == mChargingSpeed) {
            mChargingSpeed.setValue((String) newValue);
            mChargingSpeed.setSummary(mChargingSpeed.getEntry());
        }

	if (preference == mVibStrength) {
            mVibStrength.setValue((String) newValue);
            mVibStrength.setSummary(mVibStrength.getEntry());
            Utils.setStringProp(VIB_STRENGTH_SYSTEM_PROPERTY, (String) newValue);
            mVibrator.vibrate(VibrationEffect.createOneShot(85, VibrationEffect.DEFAULT_AMPLITUDE));
        }

        return true;
    }

    // Remove Charging Speed preference if cool_down node is unavailable
    private void isCoolDownAvailable() {
        mPreferenceCategory = (PreferenceCategory) findPreference(KEY_CATEGORY_CHARGING);

        if (Utils.fileWritable(SmartChargingService.mmi_charging_enable)) {
            if (!Utils.fileWritable(SmartChargingService.cool_down)) {
                mPreferenceCategory.removePreference(findPreference(KEY_CHARGING_SPEED));
            }
        } else {
            getPreferenceScreen().removePreference(mPreferenceCategory);
        }
    }
}
