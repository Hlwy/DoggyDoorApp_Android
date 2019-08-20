package com.hunter.doggydoor;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    /** *************    Constants    ****************** */
    public static final String LOGTAG = "MainActivity";
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final long CONNECT_DT = 1000;

    /** *************    Bluetooth Objects    ****************** */
    public static NusService mService = null;
    public static BluetoothDevice mDevice = null;
    public static BluetoothAdapter mBtAdapter = null;

    private ViewPager viewPager;
    private ActionBar actionBar;
    private TabLayout tabLayout;
    private static final String[] tabArray = {"Main", "Control","", ""};
    private static final Integer[] tabIcons = {R.drawable.ic_home_black_36dp, R.drawable.ic_toys_black_36dp,R.drawable.baseline_android_black_18dp, R.drawable.baseline_settings_black_18dp};

    /* App Design Elements */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_main);

        /** Initialize BLE Objects */
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Generate TabLayout tabs
        FrameLayout simpleFrameLayout = (FrameLayout) findViewById(R.id.simpleFrameLayout);
        tabLayout = (TabLayout) findViewById(R.id.simpleTabLayout);
        tabLayout.setupWithViewPager(viewPager);
        for(int i = 0; i < tabArray.length; i++) {
            TextView customTab = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab_layout, null);
            customTab.setText(tabArray[i]);
            customTab.setCompoundDrawablesWithIntrinsicBounds(tabIcons[i],0,0, 0);
            TabLayout.Tab tmpTab = tabLayout.newTab();
            tmpTab.setCustomView(customTab);
            tabLayout.addTab(tmpTab);
        }
        Helper.fragmentFocus(getSupportFragmentManager());

        // perform setOnTabSelectedListener event on TabLayout
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // get the current selected tab's position and replace the fragment accordingly
                Fragment fragment = null;
                switch (tab.getPosition()) {
                    case 0:
                        fragment = new MainFragment();
                        break;
                    case 1:
                        fragment = new DeveloperFragment();
                        break;
                    case 2:
                        fragment = new DeveloperFragment();
                        break;
                    case 3:
                        fragment = new DeveloperFragment();
                        break;
                }
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.replace(R.id.simpleFrameLayout, fragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        service_init();
    }
    /** *******************************************************************************************
    *                                    END onCreate()
    * ******************************************************************************************* */

    /** *******************************************************************************************
    *
    * ******************************************************************************************* */
    private void service_init() {
        Intent bindIntent = new Intent(this, NusService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NusService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(NusService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(NusService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(NusService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(NusService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    public ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((NusService.LocalBinder) rawBinder).getService();
            Log.i(LOGTAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(LOGTAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) { mService = null; }
    };

    public final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(NusService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(LOGTAG, "UART_CONNECT_MSG");
                        MainFragment.btnConnectDisconnect.setText("Disconnect");
                        MainFragment.btnAddDev.setEnabled(true);
                        MainFragment.txtBatLbl.setVisibility(View.VISIBLE);
                        MainFragment.connectedDevice = mDevice.getName();
                        MainFragment.btnConnectDisconnectText = "Disconnect";
                        // ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - ready");
                        MainFragment.mState = UART_PROFILE_CONNECTED;
                        MainFragment.isConnectedToDd = true;
                    }
                });
            }

            if (action.equals(NusService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(LOGTAG, "UART_DISCONNECT_MSG");
                        MainFragment.btnConnectDisconnect.setText("Connect");
                        MainFragment.btnAddDev.setEnabled(false);
                        // ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        MainFragment.mState = UART_PROFILE_DISCONNECTED;
                        MainFragment.connectedDevice = null;
                        MainFragment.btnConnectDisconnectText = "Connect";
                        mService.close();
                        MainFragment.clearTagGui(true);
                        MainFragment.clearDoorGui(true);
                        MainFragment.clearStoredDevices();
                        MainFragment.isConnectedToDd = false;
                    }
                });
            }

            if (action.equals(NusService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
                Helper.nonblockingWait(CONNECT_DT,false);
                MainFragment.requestStoredDevices();
                Helper.nonblockingWait(500,false);
                MainFragment.requestDoorStatus();
                Helper.nonblockingWait(500,false);
                MainFragment.queryDoorEncoder();
                Helper.nonblockingWait(500,false);
                MainFragment.queryMotorSpeed();
                Helper.nonblockingWait(500,false);
                MainFragment.queryDoorBattery();
            }

            if (action.equals(NusService.ACTION_DATA_AVAILABLE)) {
                final byte[] txValue = intent.getByteArrayExtra(NusService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try { MainFragment.parseDdResponse(txValue); }
                        catch (Exception e) { Log.e(LOGTAG, e.toString()); }
                    }
                });
            }

            if (action.equals(NusService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
                MainFragment.isConnectedToDd = false;
            }
            if (action.equals(NusService.BLUETOOTH_NULL)){
                showMessage("Not connected to Doggy Door. Try connecting again.");
                mService.disconnect();
                MainFragment.isConnectedToDd = false;
            }
        }
    };

    public void showMessage(String msg) { Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show(); }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) { return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOGTAG, "onDestroy()");
    }

    @Override
    protected void onStop() {
        Log.d(LOGTAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(LOGTAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(LOGTAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOGTAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(LOGTAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Helper.REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}
