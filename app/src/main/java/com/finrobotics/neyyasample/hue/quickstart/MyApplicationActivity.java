package com.finrobotics.neyyasample.hue.quickstart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.finrobotics.neyyasample.AppConstants;
import com.finrobotics.neyyasample.ConnectActivity;
import com.finrobotics.neyyasample.MyService;
import com.finrobotics.neyyasample.R;
import com.finrobotics.neyyasdk.core.Gesture;
import com.philips.lighting.hue.listener.PHLightListener;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * MyApplicationActivity - The starting point for creating your own Hue App.
 * Currently contains a simple view with a button to change your lights to random colours.  Remove this and add your own app implementation here! Have fun!
 *
 * @author SteveyO
 */
public class MyApplicationActivity extends AppCompatActivity {
    private PHHueSDK phHueSDK;
    private static final int MAX_HUE = 65535;
    public static final String TAG = "QuickStart";
    private int[] colors = {12750, 25500, 46920, 56100, 65280};
    private int count = 0;
    private int currentBrightness = 254;
    private boolean isLightOn = true;
    private TextView nameTextView, addressTextView, ringStatusTextView, hubStatusTextView, dataTextView;
    private MenuItem connectMenuItem;
    private static int currentState = MyService.STATE_DISCONNECTED;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.app_name);
        setContentView(R.layout.activity_main);
        phHueSDK = PHHueSDK.create();
        registerReceiver(mNeyyaUpdateReceiver, makeNeyyaUpdateIntentFilter());
        nameTextView = (TextView) findViewById(R.id.nameTextView);
        addressTextView = (TextView) findViewById(R.id.addressTextView);
        ringStatusTextView = (TextView) findViewById(R.id.ringStatusTextView);
        hubStatusTextView = (TextView) findViewById(R.id.hubStatusTextView);
        dataTextView = (TextView) findViewById(R.id.dataTextView);

        Button nextButton = (Button) findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeColor(true);
            }
        });

        Button previousButton = (Button) findViewById(R.id.previousButton);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeColor(false);
            }
        });

        Button brightnessPlusButton = (Button) findViewById(R.id.brightnessPlusButton);
        brightnessPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustBrightness(true);
            }
        });

        Button brightnessMinusButton = (Button) findViewById(R.id.brightnessMinusButton);
        brightnessMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adjustBrightness(false);
            }
        });

        Button onOffButton = (Button) findViewById(R.id.onOffButton);
        onOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLightOn) {
                    lightsOn(isLightOn);
                    isLightOn = false;
                } else {
                    lightsOn(isLightOn);
                    isLightOn = true;
                }
            }
        });
        setName(AppConstants.selectedDevice.getName());
        setAddress(AppConstants.selectedDevice.getAddress());
        showHubStatus("Connected");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.connectmenu, menu);
        connectMenuItem = menu.findItem(R.id.action_connect);
        final Intent intent = new Intent(MyService.BROADCAST_COMMAND_GET_STATE);
        sendBroadcast(intent);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_connect) {
            showData("");
            if (currentState == MyService.STATE_DISCONNECTED || currentState == MyService.STATE_SEARCH_FINISHED) {
                //mMyService.connectToDevice(mSelectedDevice);
                final Intent intent = new Intent(MyService.BROADCAST_COMMAND_CONNECT);
                intent.putExtra(MyService.DATA_DEVICE, AppConstants.selectedDevice);
                sendBroadcast(intent);
            } else if (currentState == MyService.STATE_CONNECTED_AND_READY) {
                final Intent intent = new Intent(MyService.BROADCAST_COMMAND_DISCONNECT);
                sendBroadcast(intent);
            }
        }


        return super.onOptionsItemSelected(item);
    }


    private final BroadcastReceiver mNeyyaUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (MyService.BROADCAST_GESTURE.equals(action)) {
                int gesture = intent.getIntExtra(MyService.DATA_GESTURE, 0);
                showData(Gesture.parseGesture(gesture));
                performGesture(gesture);

            } else if (MyService.BROADCAST_STATE.equals(action)) {
                int status = intent.getIntExtra(MyService.DATA_STATE, 0);
                if (status == MyService.STATE_DISCONNECTED) {
                    currentState = MyService.STATE_DISCONNECTED;
                    changeButtonStatus();
                    showRingStatus("Disconnected");
                } else if (status == MyService.STATE_CONNECTING) {
                    currentState = MyService.STATE_CONNECTING;
                    changeButtonStatus();
                    showRingStatus("Connecting");
                } else if (status == MyService.STATE_CONNECTED) {
                    currentState = MyService.STATE_CONNECTED;
                    changeButtonStatus();
                    showRingStatus("Connected");
                } else if (status == MyService.STATE_CONNECTED_AND_READY) {
                    currentState = MyService.STATE_CONNECTED_AND_READY;
                    changeButtonStatus();
                    showRingStatus("Connected and Ready");
                }
            }
            else if (MyService.BROADCAST_ERROR.equals(action)) {
                int errorNo = intent.getIntExtra(MyService.DATA_ERROR_NUMBER, 0);
                String errorMessage = intent.getStringExtra(MyService.DATA_ERROR_MESSAGE);
                showData("Error occurred. Error number - " + errorNo + " Message - " + errorMessage);
            }
        }
    };

    private void performGesture(int gesture) {
        Log.w(TAG, "Gesture - " + Gesture.parseGesture(gesture));
        switch (gesture) {
            case Gesture.SWIPE_DOWN:
                adjustBrightness(false);
                break;
            case Gesture.SWIPE_UP:
                adjustBrightness(true);
                break;
            case Gesture.SWIPE_LEFT:
                changeColor(false);
                break;
            case Gesture.SWIPE_RIGHT:
                changeColor(true);
                break;
            case Gesture.DOUBLE_TAP:
                if (isLightOn) {
                    lightsOn(isLightOn);
                    isLightOn = false;
                } else {
                    lightsOn(isLightOn);
                    isLightOn = true;
                }
                break;
        }
    }

    private void changeColor(boolean increase) {
        int color;
        if (increase) {
            if (count == 4)
                count = 0;
            else
                count++;
            color = colors[count];
        } else {
            if (count == 0)
                count = 4;
            else
                count--;
            color = colors[count];
        }

        PHBridge bridge = phHueSDK.getSelectedBridge();
        List<PHLight> allLights = bridge.getResourceCache().getAllLights();
        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setHue(color);
            bridge.updateLightState(light, lightState, listener);
        }
    }

    private void adjustBrightness(boolean increase) {
        if (increase) {
            currentBrightness += 50;
            if (currentBrightness > 254)
                currentBrightness = 254;

            changeBrightness();
        } else {
            currentBrightness -= 50;
            if (currentBrightness < 0)
                currentBrightness = 0;
            changeBrightness();
        }

    }

    private void changeBrightness() {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        List<PHLight> allLights = bridge.getResourceCache().getAllLights();
        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setBrightness(currentBrightness);
            bridge.updateLightState(light, lightState, listener);
        }
    }

    private void lightsOn(boolean status) {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        List<PHLight> allLights = bridge.getResourceCache().getAllLights();
        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setOn(status);
            bridge.updateLightState(light, lightState, listener);
        }
    }


    public void randomLights() {
        PHBridge bridge = phHueSDK.getSelectedBridge();

        List<PHLight> allLights = bridge.getResourceCache().getAllLights();
        Random rand = new Random();

        for (PHLight light : allLights) {
            PHLightState lightState = new PHLightState();
            lightState.setHue(rand.nextInt(MAX_HUE));

            // To validate your lightstate is valid (before sending to the bridge) you can use:  
            // String validState = lightState.validateState();
            bridge.updateLightState(light, lightState, listener);
            //  bridge.updateLightState(light, lightState);   // If no bridge response is required then use this simpler form.
        }
    }

    // If you want to handle the response from the bridge, create a PHLightListener object.
    PHLightListener listener = new PHLightListener() {

        @Override
        public void onSuccess() {
        }

        @Override
        public void onStateUpdate(Map<String, String> arg0, List<PHHueError> arg1) {
            Log.w(TAG, "Light has updated");
        }

        @Override
        public void onError(int arg0, String arg1) {
        }

        @Override
        public void onReceivingLightDetails(PHLight arg0) {
        }

        @Override
        public void onReceivingLights(List<PHBridgeResource> arg0) {
        }

        @Override
        public void onSearchComplete() {
        }
    };

    @Override
    protected void onDestroy() {
        PHBridge bridge = phHueSDK.getSelectedBridge();
        if (bridge != null) {

            if (phHueSDK.isHeartbeatEnabled(bridge)) {
                phHueSDK.disableHeartbeat(bridge);
            }

            phHueSDK.disconnect(bridge);
            super.onDestroy();
        }
    }

    @Override
    public void onBackPressed() {
      startActivity(new Intent(MyApplicationActivity.this, ConnectActivity.class));
    }

    private IntentFilter makeNeyyaUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyService.BROADCAST_STATE);
        intentFilter.addAction(MyService.BROADCAST_GESTURE);
        intentFilter.addAction(MyService.BROADCAST_ERROR);
        intentFilter.addAction(MyService.BROADCAST_INFO);
        return intentFilter;
    }

    private void changeButtonStatus() {
        if (currentState == MyService.STATE_DISCONNECTED) {
            connectMenuItem.setTitle("Connect");
            connectMenuItem.setEnabled(true);
        } else if (currentState == MyService.STATE_CONNECTING) {
            connectMenuItem.setTitle("Connecting");
            connectMenuItem.setEnabled(false);

        } else if (currentState == MyService.STATE_CONNECTED) {
            connectMenuItem.setTitle("Connecting");
            connectMenuItem.setEnabled(false);

        } else if (currentState == MyService.STATE_CONNECTED_AND_READY) {
            connectMenuItem.setTitle("Disconnect");
            connectMenuItem.setEnabled(true);
        }
    }


    private void setName(String name) {
        nameTextView.setText("Name - " + name);
    }

    private void setAddress(String address) {
        addressTextView.setText("Address - " + address);
    }

    private void showRingStatus(String msg) {
        ringStatusTextView.setText("Status - " + msg);
    }

    private void showHubStatus(String msg) {
        hubStatusTextView.setText("Hub Status - " + msg);
    }


    private void showData(String msg) {
        dataTextView.setText("Data - " + msg);
    }

}
