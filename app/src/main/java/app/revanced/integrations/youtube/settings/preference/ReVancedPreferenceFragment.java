package app.revanced.integrations.youtube.settings.preference;

import static app.revanced.integrations.shared.settings.preference.AbstractPreferenceFragment.showRestartDialog;
import static app.revanced.integrations.shared.settings.preference.AbstractPreferenceFragment.updateListPreferenceSummary;
import static app.revanced.integrations.shared.utils.ResourceUtils.getIdIdentifier;
import static app.revanced.integrations.shared.utils.ResourceUtils.getXmlIdentifier;
import static app.revanced.integrations.shared.utils.StringRef.str;
import static app.revanced.integrations.shared.utils.Utils.getChildView;
import static app.revanced.integrations.shared.utils.Utils.showToastShort;
import static app.revanced.integrations.youtube.settings.Settings.DEFAULT_PLAYBACK_SPEED;
import static app.revanced.integrations.youtube.settings.Settings.DOUBLE_BACK_TIMEOUT;
import static app.revanced.integrations.youtube.settings.Settings.HIDE_PREVIEW_COMMENT;
import static app.revanced.integrations.youtube.settings.Settings.HIDE_PREVIEW_COMMENT_TYPE;
import static app.revanced.integrations.youtube.settings.preference.ReVancedSettingsPreference.enableDisablePreferences;
import static app.revanced.integrations.youtube.settings.preference.ReVancedSettingsPreference.setPreferenceManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Objects;

import app.revanced.integrations.shared.settings.BooleanSetting;
import app.revanced.integrations.shared.settings.Setting;
import app.revanced.integrations.shared.utils.Logger;
import app.revanced.integrations.youtube.patches.video.CustomPlaybackSpeedPatch;
import app.revanced.integrations.youtube.utils.ExtendedUtils;
import app.revanced.integrations.youtube.utils.ThemeUtils;

/**
 * @noinspection ALL
 */
public class ReVancedPreferenceFragment extends PreferenceFragment {
    public static boolean settingImportInProgress = false;
    private final int READ_REQUEST_CODE = 42;
    private final int WRITE_REQUEST_CODE = 43;
    @SuppressLint("SuspiciousIndentation")
    private final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, str) -> {
        try {
            Setting<?> setting = Setting.getSettingFromPath(str);
            if (setting == null) {
                return;
            }
            Preference mPreference = findPreference(str);
            if (mPreference == null) {
                return;
            }

            if (mPreference instanceof SwitchPreference switchPreference) {
                BooleanSetting boolSetting = (BooleanSetting) setting;
                if (settingImportInProgress) {
                    switchPreference.setChecked(boolSetting.get());
                } else {
                    BooleanSetting.privateSetValue(boolSetting, switchPreference.isChecked());
                }

                if (ExtendedUtils.anyMatchSetting(setting)) {
                    ExtendedUtils.setPlayerFlyoutPanelAdditionalSettings();
                } else if (setting.equals(HIDE_PREVIEW_COMMENT) || setting.equals(HIDE_PREVIEW_COMMENT_TYPE)) {
                    ExtendedUtils.setCommentPreviewSettings();
                }
            } else if (mPreference instanceof EditTextPreference editTextPreference) {
                if (settingImportInProgress) {
                    editTextPreference.setText(setting.get().toString());
                } else {
                    Setting.privateSetValueFromString(setting, editTextPreference.getText());
                }
            } else if (mPreference instanceof ListPreference listPreference) {
                if (settingImportInProgress) {
                    listPreference.setValue(setting.get().toString());
                } else {
                    Setting.privateSetValueFromString(setting, listPreference.getValue());
                }

                if (setting.equals(DEFAULT_PLAYBACK_SPEED)) {
                    listPreference.setEntries(CustomPlaybackSpeedPatch.getListEntries());
                    listPreference.setEntryValues(CustomPlaybackSpeedPatch.getListEntryValues());
                    updateListPreferenceSummary(listPreference, setting);
                } else if (setting.equals(DOUBLE_BACK_TIMEOUT)) {
                    updateListPreferenceSummary(listPreference, setting, false);
                } else {
                    updateListPreferenceSummary(listPreference, setting);
                }
            } else {
                Logger.printException(() -> "Setting cannot be handled: " + mPreference.getClass() + " " + mPreference);
                return;
            }

            enableDisablePreferences();

            if (settingImportInProgress) {
                return;
            }

            if (setting.rebootApp)
                showRestartDialog(getActivity());
        } catch (Exception ex) {
            Logger.printException(() -> "OnSharedPreferenceChangeListener failure", ex);
        }
    };
    private SharedPreferences mSharedPreferences;

    public ReVancedPreferenceFragment() {
    }

    public void setPreferenceFragmentToolbar(final PreferenceManager mPreferenceManager, final String key) {
        PreferenceFragment fragment;
        switch (key) {
            case "revanced_ryd_settings" -> {
                fragment = new ReturnYouTubeDislikePreferenceFragment();
            }
            case "revanced_sb_settings" -> {
                fragment = new SponsorBlockPreferenceFragment();
            }
            default -> {
                Logger.printException(() -> "Unknown key: " + key);
                return;
            }
        }

        final Preference mPreference = mPreferenceManager.findPreference(key);
        if (mPreference == null) {
            return;
        }
        mPreference.setOnPreferenceClickListener(pref -> {
            final int fragmentId = getIdIdentifier("revanced_settings_fragments");
            final ViewGroup toolBarParent = Objects.requireNonNull(getActivity().findViewById(getIdIdentifier("revanced_toolbar_parent")));
            Toolbar toolbar = (Toolbar) toolBarParent.getChildAt(0);
            TextView toolbarTextView = Objects.requireNonNull(getChildView(toolbar, view -> view instanceof TextView));
            toolbarTextView.setText(pref.getTitle());

            getFragmentManager()
                    .beginTransaction()
                    .replace(fragmentId, fragment)
                    .addToBackStack(null)
                    .setReorderingAllowed(true)
                    .commitAllowingStateLoss();
            return false;
        });
    }

    public void setPreferenceScreenToolbar(final PreferenceManager mPreferenceManager) {
        final String[] preferenceScreenKey = {
                "ads",
                "alt_thumbnails",
                "backup",
                "bottom_player",
                "external_downloader",
                "flyout_menu",
                "fullscreen",
                "general",
                "misc",
                "navigation",
                "overlay_button",
                "patches_information",
                "player",
                "seekbar",
                "shorts",
                "swipe_controls",
                "video"
        };
        for (String key : preferenceScreenKey) {
            if (mPreferenceManager.findPreference(key) instanceof PreferenceScreen mPreferenceScreen) {
                mPreferenceScreen.setOnPreferenceClickListener(preferenceScreen -> {
                    Dialog preferenceScreenDialog = mPreferenceScreen.getDialog();
                    ViewGroup rootView = (ViewGroup) preferenceScreenDialog.findViewById(android.R.id.content).getParent();
                    Toolbar toolbar = new Toolbar(preferenceScreen.getContext());
                    toolbar.setTitle(preferenceScreen.getTitle());
                    toolbar.setNavigationIcon(ThemeUtils.getBackButtonDrawable());
                    toolbar.setNavigationOnClickListener(view -> preferenceScreenDialog.dismiss());
                    int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
                    toolbar.setTitleMargin(margin, 0, margin, 0);
                    TextView toolbarTextView = getChildView(toolbar, view -> view instanceof TextView);
                    toolbarTextView.setTextColor(ThemeUtils.getTextColor());
                    rootView.addView(toolbar, 0);
                    return false;
                });
            }
        }
    }

    @SuppressLint("ResourceType")
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        try {
            final PreferenceManager mPreferenceManager = getPreferenceManager();
            mPreferenceManager.setSharedPreferencesName(Setting.preferences.name);
            mSharedPreferences = mPreferenceManager.getSharedPreferences();
            addPreferencesFromResource(getXmlIdentifier("revanced_prefs"));

            setPreferenceFragmentToolbar(mPreferenceManager, "revanced_ryd_settings");
            setPreferenceFragmentToolbar(mPreferenceManager, "revanced_sb_settings");
            setPreferenceScreenToolbar(mPreferenceManager);

            setPreferenceManager(mPreferenceManager);
            enableDisablePreferences();

            setBackupRestorePreference();
            ReVancedSettingsPreference.initializeReVancedSettings(getActivity());

            for (Setting<?> setting : Setting.allLoadedSettings()) {
                final Preference preference = mPreferenceManager.findPreference(setting.key);

                if (preference instanceof SwitchPreference switchPreference) {
                    BooleanSetting boolSetting = (BooleanSetting) setting;
                    switchPreference.setChecked(boolSetting.get());
                } else if (preference instanceof EditTextPreference editTextPreference) {
                    editTextPreference.setText(setting.get().toString());
                } else if (preference instanceof ListPreference listPreference) {
                    if (setting.equals(DEFAULT_PLAYBACK_SPEED)) {
                        listPreference.setEntries(CustomPlaybackSpeedPatch.getListEntries());
                        listPreference.setEntryValues(CustomPlaybackSpeedPatch.getListEntryValues());
                        updateListPreferenceSummary(listPreference, setting);
                    } else if (setting.equals(DOUBLE_BACK_TIMEOUT)) {
                        updateListPreferenceSummary(listPreference, setting, false);
                    } else {
                        updateListPreferenceSummary(listPreference, setting);
                    }
                }
            }

            mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        } catch (Throwable th) {
            Logger.printException(() -> "Error during onCreate()", th);
        }
    }

    @Override
    public void onDestroy() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        super.onDestroy();
    }

    /**
     * Add Preference to Import/Export settings submenu
     */
    private void setBackupRestorePreference() {
        findPreference("revanced_extended_settings_import").setOnPreferenceClickListener(pref -> {
            importActivity();
            return false;
        });

        findPreference("revanced_extended_settings_export").setOnPreferenceClickListener(pref -> {
            exportActivity();
            return false;
        });
    }

    /**
     * Invoke the SAF(Storage Access Framework) to export settings
     */
    private void exportActivity() {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        var appName = ExtendedUtils.applicationLabel;
        var versionName = ExtendedUtils.appVersionName;
        var formatDate = dateFormat.format(new Date(System.currentTimeMillis()));
        var fileName = String.format("%s_v%s_%s.txt", appName, versionName, formatDate);

        var intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }

    /**
     * Invoke the SAF(Storage Access Framework) to import settings
     */
    private void importActivity() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(Build.VERSION.SDK_INT <= 28 ? "*/*" : "text/plain");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    /**
     * Activity should be done within the lifecycle of PreferenceFragment
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            exportText(data.getData());
        } else if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            importText(data.getData());
        }
    }

    private void exportText(Uri uri) {
        final Context context = this.getContext();

        try {
            @SuppressLint("Recycle")
            FileWriter jsonFileWriter =
                    new FileWriter(
                            Objects.requireNonNull(context.getApplicationContext()
                                            .getContentResolver()
                                            .openFileDescriptor(uri, "w"))
                                    .getFileDescriptor()
                    );
            PrintWriter printWriter = new PrintWriter(jsonFileWriter);
            printWriter.write(Setting.exportToJson(context));
            printWriter.close();
            jsonFileWriter.close();

            showToastShort(str("revanced_extended_settings_export_success"));
        } catch (IOException e) {
            showToastShort(str("revanced_extended_settings_export_failed"));
        }
    }

    private void importText(Uri uri) {
        final Context context = this.getContext();
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            settingImportInProgress = true;

            @SuppressLint("Recycle")
            FileReader fileReader =
                    new FileReader(
                            Objects.requireNonNull(context.getApplicationContext()
                                            .getContentResolver()
                                            .openFileDescriptor(uri, "r"))
                                    .getFileDescriptor()
                    );
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            bufferedReader.close();
            fileReader.close();

            final boolean restartNeeded = Setting.importFromJSON(sb.toString(), true);
            if (restartNeeded) {
                showRestartDialog(context);
            }
        } catch (IOException e) {
            showToastShort(str("revanced_extended_settings_import_failed"));
            throw new RuntimeException(e);
        } finally {
            settingImportInProgress = false;
        }
    }
}