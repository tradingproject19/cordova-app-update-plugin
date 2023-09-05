package com.mrspark.cordova.plugin;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.tasks.Task;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.R;
import android.widget.FrameLayout;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import static java.lang.System.out;

public class UpdatePlugin extends CordovaPlugin {
    public int REQUEST_CODE = 7;
    private static String IN_APP_UPDATE_TYPE = "FLEXIBLE";
    private static Integer DAYS_FOR_FLEXIBLE_UPDATE = 0;
    private static Integer DAYS_FOR_IMMEDIATE_UPDATE = 0;
    private static Integer HIGH_PRIORITY_UPDATE = 0;
    private static Integer MEDIUM_PRIORITY_UPDATE = 1;
    private static AppUpdateManager appUpdateManager;
    private static InstallStateUpdatedListener listener;
    private FrameLayout layout;


    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
            super.initialize(cordova, webView);
            layout = (FrameLayout) webView.getView().getParent();
    }

    public void onStateUpdate(final InstallState state) {
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                    // After the update is downloaded, show a notification
                    // and request user confirmation to restart the app.
                    popupSnackbarForCompleteUpdate();
            }
    };

    public void checkForUpdate(final int updateType, final AppUpdateInfo appUpdateInfo) {
            if (updateType == 0) {
                    IN_APP_UPDATE_TYPE = "FLEXIBLE";
                    listener = state -> {
                            onStateUpdate(state);
                    };
                    appUpdateManager.registerListener(listener);
            }else {
                IN_APP_UPDATE_TYPE = "IMMEDIATE";
            }
            try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, updateType, cordova.getActivity(),
                                    REQUEST_CODE);
            } catch (final Exception e) {
                    e.printStackTrace();
            }
    }

    /* Displays the snackbar notification and call to action. */
    private void popupSnackbarForCompleteUpdate() {
            final Snackbar snackbar = Snackbar.make(layout, "An update has just been downloaded.",
                            Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("RESTART", view -> appUpdateManager.completeUpdate());
            snackbar.show();
    }

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) {
            // Verify that the user sent a "show" action
            if (action.equals("update")) {
              try {
                final JSONObject argument = args.getJSONObject(0);
                DAYS_FOR_FLEXIBLE_UPDATE = Integer.parseInt(argument.getString("flexibleUpdateStalenessDays"));
                DAYS_FOR_IMMEDIATE_UPDATE = Integer.parseInt(argument.getString("immediateUpdateStalenessDays"));
              } catch (final JSONException e) {
                e.printStackTrace();
              }
              final Context context = this.cordova.getContext();
              appUpdateManager = AppUpdateManagerFactory.create(context);
              final Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

              try {
                appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                  if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.updatePriority() >= HIGH_PRIORITY_UPDATE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    checkForUpdate(AppUpdateType.IMMEDIATE, appUpdateInfo);
                  } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.updatePriority() >= MEDIUM_PRIORITY_UPDATE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    checkForUpdate(AppUpdateType.FLEXIBLE, appUpdateInfo);
                  } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.clientVersionStalenessDays() != null
                    && appUpdateInfo.clientVersionStalenessDays() >= DAYS_FOR_IMMEDIATE_UPDATE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    checkForUpdate(AppUpdateType.IMMEDIATE, appUpdateInfo);
                  } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.clientVersionStalenessDays() != null
                    && appUpdateInfo.clientVersionStalenessDays() >= DAYS_FOR_FLEXIBLE_UPDATE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    checkForUpdate(AppUpdateType.FLEXIBLE, appUpdateInfo);
                  }

                  final String playStoreVersion = appUpdateInfo.availableVersionCode() + "";
                  final JSONObject result = new JSONObject();
                  try {
                    result.put("availableVersionCode", playStoreVersion);
                    result.put("updateAvailability", appUpdateInfo.updateAvailability());
                    result.put("installStatus", appUpdateInfo.installStatus());
                    result.put("totalBytesToDownload", appUpdateInfo.totalBytesToDownload());
                    result.put("bytesDownloaded", appUpdateInfo.bytesDownloaded());
                    callbackContext.success(result);
                  } catch (JSONException e) {
                    e.printStackTrace();
                    callbackContext.error("Failed to retrieve Play Store version."  + e.getMessage());
                  }
                });
              } catch (final Exception e) {
                e.printStackTrace();
              }
              return true;
          }else if(action.equals("check")){
              appUpdateManager = AppUpdateManagerFactory.create(this.cordova.getContext());
              final Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

              appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                /*if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {*/
                  final String playStoreVersion = appUpdateInfo.availableVersionCode() + "";
                  final JSONObject result = new JSONObject();
                  try {
                    result.put("availableVersionCode", playStoreVersion);
                    result.put("updateAvailability", appUpdateInfo.updateAvailability());
                    result.put("installStatus", appUpdateInfo.installStatus());
                    result.put("totalBytesToDownload", appUpdateInfo.totalBytesToDownload());
                    result.put("bytesDownloaded", appUpdateInfo.bytesDownloaded());
                    callbackContext.success(result);
                  } catch (JSONException e) {
                    e.printStackTrace();
                    callbackContext.error("Failed to retrieve Play Store version."  + e.getMessage());
                  }
                /*} else {
                  callbackContext.error("No updates available in the Play Store.");
                }*/
              });

              appUpdateInfoTask.addOnFailureListener(e -> {
                e.printStackTrace();
                callbackContext.error("Failed to retrieve Play Store version." + e.getMessage());
              });
            return true;
      } else{
            return false;
      }
    }

    @Override
    public void onResume(final boolean multitasking) {
        super.onResume(multitasking);
        appUpdateManager
            .getAppUpdateInfo()
            .addOnSuccessListener(
                appUpdateInfo -> {
                    if (IN_APP_UPDATE_TYPE.equals("FLEXIBLE") && appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate();
                    }

                    if (appUpdateInfo.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        // If an in-app update is already running, resume the update.
                        try {
                            checkForUpdate(AppUpdateType.IMMEDIATE, appUpdateInfo);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }
}
