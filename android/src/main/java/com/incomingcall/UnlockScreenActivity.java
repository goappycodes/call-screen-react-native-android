package com.incomingcall;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.net.Uri;
import android.os.Vibrator;
import android.content.Context;
import android.media.MediaPlayer;
import android.provider.Settings;
import java.util.List;
import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.squareup.picasso.Picasso;

public class UnlockScreenActivity extends AppCompatActivity implements UnlockScreenActivityInterface {

    private static final String TAG = "MessagingService";
    private static final String MI_TAG = "MIDEBUGPER";
    private TextView tvName;
    private TextView tvInfo;
    private ImageView ivAvatar;
    private String uuid = "";
    static boolean active = false;
    private static Vibrator v = (Vibrator) IncomingCallModule.reactContext.getSystemService(Context.VIBRATOR_SERVICE);
    private long[] pattern = {0, 1000, 800};
    private static MediaPlayer player = MediaPlayer.create(IncomingCallModule.reactContext, Settings.System.DEFAULT_RINGTONE_URI);
    private static Activity fa;
    static boolean permissionGrated = false;

    @Override
    public void onStart() {
        super.onStart();
           Log.d(MI_TAG, "onStart");
        active = true;
    }
        // Call Back method to get the Message form other Activity
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            // check if the request code is same as what is passed here it is 2
            if (requestCode == 2) {
                // String message=data.getStringExtra("MESSAGE");
                // textView1.setText(message);
                Log.d(MI_TAG, "ONACTSUCCESS");
            } else {
                Log.d(MI_TAG, "ONACTFail");
            }
        }
    

    @Override
    public void onStop() {
        super.onStop();
         Log.d(MI_TAG, "onStop");
        active = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fa = this;

        setContentView(R.layout.activity_call_incoming);

        tvName = findViewById(R.id.tvName);
        tvInfo = findViewById(R.id.tvInfo);
        ivAvatar = findViewById(R.id.ivAvatar);
        permissionGrated = true;
        Log.d(MI_TAG, "onCreate");
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("uuid")) {
                Log.d(MI_TAG, "bundle_uuid");
                uuid = bundle.getString("uuid");
            }
            if (bundle.containsKey("name")) {
                String name = bundle.getString("name");
                tvName.setText(name);
            }
            if (bundle.containsKey("info")) {
                String info = bundle.getString("info");
                tvInfo.setText(info);
            }
            if (bundle.containsKey("avatar")) {
                String avatar = bundle.getString("avatar");
                if (avatar != null) {
                    Picasso.get().load(avatar).transform(new CircleTransform()).into(ivAvatar);
                }
            }
        }

        try {
            Log.d(MI_TAG, "TRY");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

            v.vibrate(pattern, 0);
            player.start();
        }catch (Exception e) {
            Log.d(MI_TAG, "CATCH");
            throw e;
        }

        AnimateImage acceptCallBtn = findViewById(R.id.ivAcceptCall);
        acceptCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    v.cancel();
                    player.stop();
                    acceptDialing();
                } catch (Exception e) {
                    WritableMap params = Arguments.createMap();
                    params.putString("message", e.getMessage());
                    sendEvent("error", params);
                    dismissDialing();
                }
            }
        });

        AnimateImage rejectCallBtn = findViewById(R.id.ivDeclineCall);
        rejectCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.cancel();
                player.stop();
                dismissDialing();
            }
        });

    }
    @Override
    protected void  onNewIntent(Intent intent) 
    {
         super.onNewIntent(intent);
         Log.d(MI_TAG, "NEWINTENT");
    }

    @Override
    public void onBackPressed() {
        // Dont back
    }

    public static  boolean dismissIncoming() {
        try{
            v.cancel();
            player.stop();
            fa.finish();
            Log.d(MI_TAG, "DISMISS TRY");
            return true;

        }catch (Exception e) {
            Log.d(MI_TAG, "DISMISS CATCH "+e.getMessage());
            throw e;
            //return false;
        }
        
    }

    public static  boolean checkPermission() {
        // boolean permissionStatus = permissionGrated;
        Log.d(MI_TAG, "CHECKINGPER");
        return permissionGrated;
    }

    private void acceptDialing() {
        WritableMap params = Arguments.createMap();
        params.putBoolean("accept", true);
        params.putString("uuid", uuid);
        if (!IncomingCallModule.reactContext.hasCurrentActivity()) {
            params.putBoolean("isHeadless", true);
        }

        sendEvent("answerCall", params);

        finish();
    }

    private void dismissDialing() {
        WritableMap params = Arguments.createMap();
        params.putBoolean("accept", false);
        params.putString("uuid", uuid);
        if (!IncomingCallModule.reactContext.hasCurrentActivity()) {
            params.putBoolean("isHeadless", true);
        }

        sendEvent("endCall", params);

        finish();
    }

    @Override
    public void onConnected() {
       
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onDisconnected() {
        Log.d(MI_TAG, "onDisconnected: ");

    }

    @Override
    public void onConnectFailure() {
        Log.d(MI_TAG, "onConnectFailure: ");

    }

    @Override
    public void onIncoming(ReadableMap params) {
        Log.d(MI_TAG, "onIncoming: ");
    }

    private void sendEvent(String eventName, WritableMap params) {
         Log.d(MI_TAG, "sendEvent");
        IncomingCallModule.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }
}
