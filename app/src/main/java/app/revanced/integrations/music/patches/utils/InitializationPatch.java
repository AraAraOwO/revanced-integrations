package app.revanced.integrations.music.patches.utils;

import static app.revanced.integrations.music.utils.RestartUtils.showRestartDialog;

import android.app.Activity;

import androidx.annotation.NonNull;

import app.revanced.integrations.music.utils.ExtendedUtils;
import app.revanced.integrations.shared.settings.BaseSettings;
import app.revanced.integrations.shared.utils.Utils;

@SuppressWarnings("unused")
public class InitializationPatch {

    /**
     * The new layout is not loaded normally when the app is first installed.
     * (Also reproduced on unPatched YouTube Music)
     * <p>
     * To fix this, show the reboot dialog when the app is installed for the first time.
     */
    public static void onCreate(@NonNull Activity mActivity) {
        if (BaseSettings.SETTINGS_INITIALIZED.get())
            return;

        showRestartDialog(mActivity, "revanced_restart_first_run", 1000);
        Utils.runOnMainThreadDelayed(() -> BaseSettings.SETTINGS_INITIALIZED.save(true), 1000);
    }

    public static void setDeviceInformation(@NonNull Activity mActivity) {
        ExtendedUtils.setPackageName(mActivity);
        ExtendedUtils.setApplicationLabel(mActivity);
        ExtendedUtils.setVersionName(mActivity);
    }
}