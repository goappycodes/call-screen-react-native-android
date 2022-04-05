package com.incomingcall;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Binder;
import android.app.Activity;
import android.app.AppOpsManager;
import android.provider.Settings;
import java.lang.reflect.Method;
import android.view.WindowManager;
import android.content.Context;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;


public class IncomingCallModule extends ReactContextBaseJavaModule {

    public static ReactApplicationContext reactContext;
    public static Activity mainActivity;

    private static final String TAG = "RNIC:IncomingCallModule";
    private static final String MI_TAG = "MIDEBUGPER_I";
    private WritableMap headlessExtras;
    static boolean permissionGrated = false;

    public IncomingCallModule(ReactApplicationContext context) {
        super(context);
        reactContext = context;
        mainActivity = getCurrentActivity();
    }

    @Override
    public String getName() {
        return "IncomingCall";
    }



    

    @ReactMethod
    public void display(String uuid, String name, String avatar, String info, int timeout, Promise promise) {
        if (UnlockScreenActivity.active) {
            Log.d(MI_TAG, "Active");
            return;
        }

        if (reactContext != null) {
            Log.d(MI_TAG, uuid);
            Bundle bundle = new Bundle();
            bundle.putString("uuid", uuid);
            bundle.putString("name", name);
            bundle.putString("avatar", avatar);
            bundle.putString("info", info);
            Intent i = new Intent(reactContext, UnlockScreenActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            i.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED +
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD +
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            i.putExtras(bundle);

            try {
                PackageManager packageManager = reactContext.getPackageManager();
                if (i.resolveActivity(packageManager) != null) {
                    reactContext.startActivity(i);
                    Log.d(MI_TAG, "TRY");
                } else {
                    Log.d(MI_TAG, "TRY ELSE");
                }

                if (timeout > 0) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // this code will be executed after timeout seconds
                            UnlockScreenActivity.dismissIncoming();
                        }
                    }, timeout);
                }
            } catch (Exception e) {
                Log.d(MI_TAG, "catch");
                throw e;
            }

        }
        promise.resolve(permissionGrated);
    }

    @ReactMethod
    public void dismiss() {
        // final Activity activity = reactContext.getCurrentActivity();

        // assert activity != null;

        // UnlockScreenActivity.dismissIncoming();

        return;
    }

    private Context getAppContext() {
        return this.reactContext.getApplicationContext();
    }

    @ReactMethod
    public void backToForeground() {
        Context context = getAppContext();
        String packageName = context.getApplicationContext().getPackageName();
        Intent focusIntent = context.getPackageManager().getLaunchIntentForPackage(packageName).cloneFilter();
        Activity activity = getCurrentActivity();
        boolean isOpened = activity != null;
        Log.d(MI_TAG, "backToForeground, app isOpened ?" + (isOpened ? "true" : "false"));

        if (isOpened) {
            focusIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(focusIntent);
        }
    }

    @ReactMethod
    public void openAppFromHeadlessMode(String uuid) {
        Context context = getAppContext();
        String packageName = context.getApplicationContext().getPackageName();
        Intent focusIntent = context.getPackageManager().getLaunchIntentForPackage(packageName).cloneFilter();
        Activity activity = getCurrentActivity();
        boolean isOpened = activity != null;

        if (!isOpened) {
            focusIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            final WritableMap response = new WritableNativeMap();
            response.putBoolean("isHeadless", true);
            response.putString("uuid", uuid);

            this.headlessExtras = response;

            getReactApplicationContext().startActivity(focusIntent);
        }
    }

    private void onDisplayPopupPermission() {
        Log.d(MI_TAG, "lets display popup");
    }


    @ReactMethod
    public void canDrawOverlayViews(Promise promise) {
        boolean hasPermission = false;
        if (Build.VERSION.SDK_INT < 21) {
            hasPermission = true;
        }
        Context con = reactContext;
        try {
            Log.d(MI_TAG, "TRYING_PER");
            hasPermission =  Settings.canDrawOverlays(con);
        } catch (NoSuchMethodError e) {
            Log.d(MI_TAG, "CATCH_NO_PER");
            hasPermission =  canDrawOverlaysUsingReflection(con);
        }

        promise.resolve(hasPermission);
    }


    // public static boolean isXiaomi() {
    // return "xiaomi".equalsIgnoreCase(Build.MANUFACTURER);
    // }

    public static boolean canDrawOverlaysUsingReflection(Context context) {
        try {

            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            Class clazz = AppOpsManager.class;
            Method dispatchMethod = clazz.getMethod("checkOp", new Class[] { int.class, int.class, String.class });
            // AppOpsManager.OP_SYSTEM_ALERT_WINDOW = 24
            int mode = (Integer) dispatchMethod.invoke(manager,
                    new Object[] { 24, Binder.getCallingUid(), context.getApplicationContext().getPackageName() });
            Log.d(MI_TAG, "AppOps" + String.valueOf(AppOpsManager.MODE_ALLOWED == mode));
            return AppOpsManager.MODE_ALLOWED == mode;

        } catch (Exception e) {
            return false;
        }
    }


    @ReactMethod
    public void getExtrasFromHeadlessMode(Promise promise) {
        if (this.headlessExtras != null) {
            promise.resolve(this.headlessExtras);

            this.headlessExtras = null;

            return;
        }

        promise.resolve(null);
    }
}
