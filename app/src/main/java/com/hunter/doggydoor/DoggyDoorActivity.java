package com.hunter.doggydoor;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class DoggyDoorActivity extends AppCompatActivity{
    /** *************    Constants    ****************** */
    public static final String LOGTAG = "DoggyDoor";
    private static final int UART_PROFILE_READY = 10;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    private static final long CONNECT_DT = 1000;
    private Drawable picLedInactive;
    private Drawable picLedActive;

    /** *************    State Parameters    ****************** */
    private static boolean isConnectedToDd = false;
    private int mState = UART_PROFILE_DISCONNECTED;
    int mSelectedItem = -1;

    /** *************    Helper Variables    ****************** */
    private Integer ntagTimesSeen = null;
    private Integer ntagDebounceThresh = null;
    private Integer ntagRssiThresh = null;
    private Integer ntagRssi = null;
    private String ntagAlias = null;
    private String ntagId = null;
    private String nChargeStatus = null;
    private Integer nBatVolt = null;
    private Integer nBatPercent = null;
    private Double nMotorSpd = null;
    private Integer nEncLimit = null;
    private Boolean nIsOpen = null;
    private Boolean nIsLocked = null;
    private Boolean nUpLimitSwitchState = null;
    private Boolean nLowLimitSwitchState = null;
    private ArrayList<Integer> ntagAddr;
    private ArrayList<String> deviceStrList;

    /** *************    Bluetooth Objects    ****************** */
    private NusService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothDevice tmpDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ArrayList<BluetoothDevice> deviceList;

    /** ********************************************************************************************
     *                                      GUI Elements
     * ****************************************************************************************** */
    // Buttons
    private Button btnConnectDisconnect;
    private Button btnAddDev;
    private Button btnGetDoorStatus;
    private Button btnLockDoor;
    private Button btnUnockDoor;
    private Button btnCloseDoor;
    private Button btnOpenDoor;
    private Button btnStopDoor;
    private Button btnSetMotorSpd;
    private Button btnGetMotorSpd;
    private Button btnSetEncLimit;
    private Button btnGetEncLimit;

    // Text Input Fields
    private EditText editAlias;
    private EditText editDebounceThresh;
    private EditText editRssiThresh;
    private EditText editEncLimit;
    private EditText editMotorSpd;

    // Text Views
    private TextView txtDevId;
    private TextView txtDevAddr;
    private TextView txtDevAlias;
    private TextView txtDevRssiThresh;
    private TextView txtDevDebounceThresh;
    private TextView txtTimesSeen;
    private TextView txtDevRssi;
    private TextView txtBatLbl;
    private TextView txtBatLife;
    private TextView txtChargeStatus;
    private TextView txtEncLimit;
    private TextView txtMotorSpd;

    // Graphic TextViews
    private TextView ledLockStatus;
    private TextView ledDoorStatus;
    private TextView ledUpLimitSwitch;
    private TextView ledLowLimitSwitch;


    /** *************    List View and GUI Helpers    ****************** */
    ListView newDevicesListView;
    List<BluetoothDevice> deviceLst;
    private TextView txtEmptyList;
    private DeviceAdapter deviceAdapter;
    Map<String, Integer> devRssiValues;
    View updateview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doggy_door_activity);
        picLedInactive = getResources().getDrawable(android.R.drawable.presence_invisible);
        picLedActive = getResources().getDrawable(android.R.drawable.presence_online);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        service_init();
        initGui();

        deviceList = new ArrayList<BluetoothDevice>();
        deviceStrList = new ArrayList<String>();
        populateList();

        clearTagGui(true);
        clearDoorGui(true);
    }

    private void initGui(){
        btnConnectDisconnect = (Button) findViewById(R.id.btn_scan);
        btnAddDev = (Button) findViewById(R.id.btn_add_dev);
        Button btnDelDev = (Button) findViewById(R.id.btn_del_dev);
        Button btnGetRssiThresh = (Button) findViewById(R.id.btn_get_rssi_thresh);
        Button btnGetAlias = (Button) findViewById(R.id.btn_get_alias);
        Button btnGetDebounceThresh = (Button) findViewById(R.id.btn_get_debounce_thresh);
        Button btnGetRssi = (Button) findViewById(R.id.btn_get_rssi);
        Button btnGetStats = (Button) findViewById(R.id.btn_get_stats);
        Button btnSetRssiThresh = (Button) findViewById(R.id.btn_set_rssi_thresh);
        Button btnSetAlias = (Button) findViewById(R.id.btn_set_alias);
        Button btnSetDebounceThresh = (Button) findViewById(R.id.btn_set_debounce_thresh);
        Button btnRefreshTag = (Button) findViewById(R.id.btn_refresh);
        Button btnGetBatInfo = (Button) findViewById(R.id.btn_get_bat);

        btnGetDoorStatus = (Button) findViewById(R.id.btn_get_door_status);
        btnLockDoor = (Button) findViewById(R.id.btn_lock_door);
        btnUnockDoor = (Button) findViewById(R.id.btn_unlock_door);
        btnCloseDoor = (Button) findViewById(R.id.btn_close_door);
        btnOpenDoor = (Button) findViewById(R.id.btn_open_door);
        btnStopDoor = (Button) findViewById(R.id.btn_stop_motor);
        btnSetMotorSpd = (Button) findViewById(R.id.btn_set_spd);
        btnGetMotorSpd = (Button) findViewById(R.id.btn_get_spd);
        btnSetEncLimit = (Button) findViewById(R.id.btn_set_enc_limit);
        btnGetEncLimit = (Button) findViewById(R.id.btn_get_enc_limit);

        editAlias = (EditText) findViewById(R.id.editAlias);
        editDebounceThresh = (EditText) findViewById(R.id.editDebounceThresh);
        editRssiThresh = (EditText) findViewById(R.id.editRssiThresh);
        editEncLimit = (EditText) findViewById(R.id.editEncLimit);
        editMotorSpd = (EditText) findViewById(R.id.editSpd);

        txtDevId  = (TextView) findViewById(R.id.txtId);
        txtDevAddr = (TextView) findViewById(R.id.txtAddr);
        txtDevAlias = (TextView) findViewById(R.id.txtAlias);
        txtDevRssiThresh = (TextView) findViewById(R.id.txtRssiThresh);
        txtDevDebounceThresh = (TextView) findViewById(R.id.txtDebounceThresh);
        txtTimesSeen = (TextView) findViewById(R.id.txtTimesSeen);
        txtDevRssi = (TextView) findViewById(R.id.txtRssi);
        txtEmptyList = (TextView) findViewById(R.id.empty);
        txtBatLbl = (TextView) findViewById(R.id.txtBatLbl);
        txtBatLife = (TextView) findViewById(R.id.txtBatLife);
        txtChargeStatus = (TextView) findViewById(R.id.txtChargeStatus);
        txtEncLimit = (TextView) findViewById(R.id.txtEncLimit);
        txtMotorSpd = (TextView) findViewById(R.id.txtSpd);
        ledLockStatus = (TextView) findViewById(R.id.led_lock_status);
        ledDoorStatus = (TextView) findViewById(R.id.led_open_status);
        ledUpLimitSwitch = (TextView) findViewById(R.id.led_top_swtich);
        ledLowLimitSwitch = (TextView) findViewById(R.id.led_bottom_switch);

        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(LOGTAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, Helper.REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {
                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(DoggyDoorActivity.this, ConnectDoggyDoorActivity.class);
                        startActivityForResult(newIntent, Helper.REQUEST_SELECT_DEVICE);
                    }
                    else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                        }
                    }
                }
            }
        });
        btnAddDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(LOGTAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, Helper.REQUEST_ENABLE_BT);
                }
                else {
                    Intent newIntent2 = new Intent(DoggyDoorActivity.this, AddDoggyTagActivity.class);
                    startActivityForResult(newIntent2, Helper.REQUEST_SELECT_DEVICE);
                }
            }
        });
        btnDelDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSelectedItem >= 0){
                    BluetoothDevice device = deviceList.get(mSelectedItem);

                    delDeviceList(device);
                    deviceLst.remove(device);
                    txtEmptyList.setVisibility(View.GONE);
                    deviceAdapter.notifyDataSetChanged();
                    txtDevAddr.setText("");
                    txtDevId.setText("");
                    txtDevAlias.setText("");
                    editAlias.setText("");
                    editDebounceThresh.setText("");
                    editRssiThresh.setText("");
                    txtDevRssiThresh.setText("");
                    txtDevDebounceThresh.setText("");
                    txtTimesSeen.setText("");
                    txtDevRssi.setText("");
                    mSelectedItem = -1;
                    if (updateview != null)
                        updateview.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });

        btnGetAlias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    Log.d(LOGTAG,"Getting Alias for device [" + txtDevId.getText() + "]...");
//                    showMessage("Getting the alias for the selected pet tag [" + txtDevId.getText().toString() + "]...");
                    sendGetCommand(Helper.DD_CMD_TAG_ALIAS,txtDevId.getText().toString());
                }
                else{
                    showMessage("No target device selected. Please select the pet tag you wish to get the alias of.");
                }
            }
        });
        btnSetAlias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    if(!editAlias.getText().toString().equals("")){
                        Log.d(LOGTAG,"Setting Alias for device [" + txtDevId.getText() + "] to -> " + editAlias.getText() + "...");
                        showMessage("Setting the alias for the selected pet tag [" + txtDevId.getText().toString() + "] to \'" + editAlias.getText().toString() +"\'...");
                        sendSetCommand(Helper.DD_CMD_TAG_ALIAS,txtDevId.getText().toString(),editAlias.getText().toString());
                        editAlias.setText("");
                    }
                    else{
                        Log.d(LOGTAG,"No device alias provided, not setting Alias for device [" + txtDevId.getText() + "].");
                        showMessage("No alias provided. Please input the alias you wish to change for the selected pet tag.");
                    }
                }else{
                    showMessage("No target device selected. Please select the pet tag you wish to change the alias of.");
                }
            }
        });
        btnGetRssiThresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().toString().equals("")){
                    Log.d(LOGTAG,"Getting RSSI Threshold for device [" + txtDevId.getText() + "]...");
//                    showMessage("Getting the RSSI threshold for the selected pet tag [" + txtDevId.getText().toString() + "]...");
                    sendGetCommand(Helper.DD_CMD_TAG_RSSI_THRESHOLD,txtDevId.getText().toString());
                }else{
                    showMessage("No target device selected. Please select the pet tag you wish to get the RSSI threshold of.");
                }
            }
        });
        btnSetRssiThresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    if(!editRssiThresh.getText().toString().equals("")){
                        Log.d(LOGTAG,"Setting RSSI Threshold for device [" + txtDevId.getText() + "] to -> " + editRssiThresh.getText() + "...");
                        showMessage("Setting the RSSI Threshold for the selected pet tag [" + txtDevId.getText().toString() + "] to \'" + editRssiThresh.getText().toString() +"\'...");
                        sendSetCommand(Helper.DD_CMD_TAG_RSSI_THRESHOLD,txtDevId.getText().toString(),editRssiThresh.getText().toString());
                        editRssiThresh.setText("");
                    }
                    else{
                        Log.d(LOGTAG,"No device RSSI threshold provided, not setting RSSI Threshold for device [" + txtDevId.getText() + "].");
                        showMessage("No RSSI threshold provided. Please input the RSSI threshold you wish to change for the selected pet tag.");
                    }
                }else{
                    showMessage("No target device selected. Please select the pet tag you wish to change the RSSI threshold of.");
                }
            }
        });
        btnGetDebounceThresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().toString().equals("")){
                    Log.d(LOGTAG,"Getting Debounce Threshold for device [" + txtDevId.getText() + "]...");
//                    showMessage("Getting the debounce threshold for the selected pet tag [" + txtDevId.getText().toString() + "]...");
                    sendGetCommand(Helper.DD_CMD_TAG_DEBOUNCE_THRESHOLD,txtDevId.getText().toString());
                }else{
                    showMessage("No target device selected. Please select the pet tag you wish to get the debounce threshold of.");
                }
            }
        });
        btnSetDebounceThresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    if(!editDebounceThresh.getText().toString().equals("")){
                        Log.d(LOGTAG,"Setting Debounce Threshold for device [" + txtDevId.getText() + "] to -> " + editDebounceThresh.getText() + "...");
                        showMessage("Setting the debounce Threshold for the selected pet tag [" + txtDevId.getText().toString() + "] to \'" + editDebounceThresh.getText().toString() +"\'...");
                        sendSetCommand(Helper.DD_CMD_TAG_DEBOUNCE_THRESHOLD,txtDevId.getText().toString(),editDebounceThresh.getText().toString());
                        editDebounceThresh.setText("");
                    }
                    else{
                        Log.d(LOGTAG,"No device Debounce threshold provided, not setting Debounce Threshold for device [" + txtDevId.getText() + "].");
                        showMessage("No debounce threshold provided. Please input the debounce threshold you wish to change for the selected pet tag.");
                    }
                }else{
                    showMessage("No target device selected. Please select the pet tag you wish to change the debounce threshold of.");
                }
            }
        });
        btnGetRssi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    Log.d(LOGTAG,"Getting RSSI for device [" + txtDevId.getText() + "]...");
//                    showMessage("Getting the RSSI for the selected pet tag [" + txtDevId.getText().toString() + "]...");
                    sendGetCommand(Helper.DD_CMD_TAG_RSSI,txtDevId.getText().toString());
                }
                else{
                    showMessage("No target device selected. Please select the pet tag you wish to get the RSSI of.");
                }
            }
        });
        btnGetStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    Log.d(LOGTAG,"Getting Statistics for device [" + txtDevId.getText() + "]...");
//                    showMessage("Getting the statistics for the selected pet tag [" + txtDevId.getText().toString() + "]...");
                    sendGetCommand(Helper.DD_CMD_TAG_STATISTICS,txtDevId.getText().toString());
                }
                else{
                    showMessage("No target device selected. Please select the pet tag you wish to get the statistics of.");
                }
            }
        });
        btnRefreshTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    Log.d(LOGTAG,"Refreshing information for device [" + txtDevId.getText() + "]...");
                    showMessage("Refreshing information for pet tag [" + txtDevId.getText().toString() + "]...");
                    sendGetCommand(Helper.DD_CMD_TAG_INFO,txtDevId.getText().toString());
                }
                else{
                    showMessage("No target device selected. Please select the pet tag you wish to see.");
                }
            }
        });

        btnGetBatInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryDoorBattery();
            }
        });
        btnStopDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mState == UART_PROFILE_CONNECTED){
                    Log.d(LOGTAG,"Attempting to Stop DoggyDoor motor...");
//                    showMessage("Attempting to Stop DoggyDoor motor...");
                    sendSetCommand(Helper.DD_CMD_STOP_MOTOR,"dd");
                }
                else{
                    showMessage("Not connected to Doggy Door. Try connecting again.");
                }
            }
        });
        btnOpenDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mState == UART_PROFILE_CONNECTED){
                    Log.d(LOGTAG,"Attempting to open DoggyDoor...");
//                    showMessage("Attempting to open DoggyDoor...");
                    sendSetCommand(Helper.DD_CMD_OPEN_DOOR,"dd");
                }
                else{
                    showMessage("Not connected to Doggy Door. Try connecting again.");
                }
            }
        });
        btnCloseDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mState == UART_PROFILE_CONNECTED){
                    Log.d(LOGTAG,"Attempting to close DoggyDoor...");
//                    showMessage("Attempting to close DoggyDoor...");
                    sendSetCommand(Helper.DD_CMD_CLOSE_DOOR,"dd");
                }
                else{
                    showMessage("Not connected to Doggy Door. Try connecting again.");
                }
            }
        });
        btnLockDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mState == UART_PROFILE_CONNECTED){
                    Log.d(LOGTAG,"Attempting to lock DoggyDoor...");
//                    showMessage("Attempting to lock DoggyDoor...");
                    sendSetCommand(Helper.DD_CMD_LOCK_DOOR,"dd");
                }
                else{
                    showMessage("Not connected to Doggy Door. Try connecting again.");
                }
            }
        });
        btnUnockDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mState == UART_PROFILE_CONNECTED){
                    Log.d(LOGTAG,"Attempting to unlock DoggyDoor...");
//                    showMessage("Attempting to unlock DoggyDoor...");
                    sendSetCommand(Helper.DD_CMD_UNLOCK_DOOR,"dd");
                }
                else{
                    showMessage("Not connected to Doggy Door. Try connecting again.");
                }
            }
        });

        btnGetDoorStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDoorStatus();
            }
        });

        btnGetEncLimit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryDoorEncoder();
            }
        });
        btnSetEncLimit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mState == UART_PROFILE_CONNECTED){
                    if(!editEncLimit.getText().toString().equals("")){
                        Log.d(LOGTAG,"Setting Doggy Door Encoder Limit to -> " + editEncLimit.getText() + "...");
                        showMessage("Setting Doggy Door Encoder Limit to \'" + editEncLimit.getText().toString() +"\'...");
                        sendSetCommand(Helper.DD_CMD_DOOR_ENCODER_LIMIT,"dd",editEncLimit.getText().toString());
                        editEncLimit.setText("");
                        editEncLimit.setEnabled(false);
                        editEncLimit.setEnabled(true);
                    }
                    else{
                        Log.d(LOGTAG,"No encoder limit provided, not sending anything to doggy door.");
                        showMessage("No encoder limit provided. Please input the encoder limit you wish to set for the DoggyDoor.");
                    }
                }else{
                    showMessage("Not connected to Doggy Door. Try connecting again.");
                }
            }
        });
        btnGetMotorSpd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryMotorSpeed();
            }
        });
        btnSetMotorSpd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mState == UART_PROFILE_CONNECTED){
                    if(!editMotorSpd.getText().toString().equals("")){
                        Log.d(LOGTAG,"Setting Doggy Door Motor Speed to -> " + editMotorSpd.getText() + "...");
                        showMessage("Setting Doggy Door Motor Speed to \'" + editMotorSpd.getText().toString() +"\'...");
                        sendSetCommand(Helper.DD_CMD_DOOR_SPEED,"dd",editMotorSpd.getText().toString());
                        editMotorSpd.setText("");
                        editMotorSpd.setEnabled(false);
                        editMotorSpd.setEnabled(true);
                    }
                    else{
                        Log.d(LOGTAG,"No motor speed provided, not sending anything to doggy door.");
                        showMessage("No motor speed provided. Please input the motor speed you wish the DoggyDoor to open/close.");
                    }
                }else{
                    showMessage("Not connected to Doggy Door. Try connecting again.");
                }
            }
        });
    }

    private void requestDoorStatus(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Getting Doggy Door Status...");
//            showMessage("Getting Doggy Door Status...");
            sendGetCommand(Helper.DD_CMD_DOOR_STATUS,"dd");
        }
        else{
            showMessage("Not connected to Doggy Door. Try connecting again.");
        }
    }
    private void queryDoorEncoder(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Getting Doggy Door Encoder Limit...");
//            showMessage("Getting Doggy Door Encoder Limit...");
            sendGetCommand(Helper.DD_CMD_DOOR_ENCODER_LIMIT,"dd");
        }else{
            showMessage("Not connected to Doggy Door. Try connecting again.");
        }
    }
    private void queryMotorSpeed(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Getting Doggy Door Motor Speed...");
//            showMessage("Getting Doggy Door Motor Speed...");
            sendGetCommand(Helper.DD_CMD_DOOR_SPEED,"dd");
        }else{
            showMessage("Not connected to Doggy Door. Try connecting again.");
        }
    }
    private void queryDoorBattery(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Getting battery information for Doggy Door...");
//            showMessage("Getting battery information for Doggy Door...");
            sendGetCommand(Helper.DD_CMD_BATTERY_INFO,"dd");
        }
        else{
            showMessage("Not connected to Doggy Door. Try connecting again.");
        }
    }

    private void clearTagGui(boolean withDisconnect){
        txtDevAddr.setText("");
        txtDevId.setText("");
        txtDevAlias.setText("");
        editAlias.setText("");
        editDebounceThresh.setText("");
        editRssiThresh.setText("");
        txtDevRssiThresh.setText("");
        txtDevDebounceThresh.setText("");
        txtTimesSeen.setText("");
        txtDevRssi.setText("");
        if(withDisconnect){
            btnAddDev.setEnabled(false);
        }
    }
    private void clearDoorGui(boolean withDisconnect){
        txtBatLbl.setVisibility(View.INVISIBLE);
        txtBatLife.setText("");
        txtChargeStatus.setText("");
        txtMotorSpd.setText("");
        txtEncLimit.setText("");
        editEncLimit.setText("");
        editMotorSpd.setText("");
        setLedDrawable(false, ledLockStatus);
        setLedDrawable(false, ledDoorStatus);
        setLedDrawable(false, ledUpLimitSwitch);
        setLedDrawable(false, ledLowLimitSwitch);
        if(withDisconnect){
            // btnAddDev.setEnabled(false);
        }
    }
    private void setLedDrawable(Boolean isActive,TextView tv){
        if(isActive){
            tv.setBackground(picLedActive);
        }else{
            tv.setBackground(picLedInactive);
        }
    }

    private Integer parseDdResponse(final byte[] resp){
        String text;
        Integer result;
        // Error handling
        try {
            text = new String(resp, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            result = -1;
            return result;
        }
        Log.d(LOGTAG, "Received Response containing [" + resp.length + " bytes] from Thingy --- \'" + text + "\'.\r\n");

        // Add response to Thingy response history, and split response using delimiters
        deviceStrList.add(text);
        text = text.replaceAll("\r", "").replaceAll("\n", "").replaceAll("\0", "");
        String[] tokens = text.split(":|\\s+");
        Log.d(LOGTAG, "Parsed response to [" + text.getBytes().length + " bytes] = \'" + Arrays.toString(tokens) + "\'.\r\n");

        /** *********************************************************************************
         *   If first response token is 'get' then parse it further to be used for updating TextView's
         *   If first response token is 'set' then ignore it
         *   If first response token is neither of the above then print it out for debugging
         *   ********************************************************************************* */
        if(tokens[0].equals("get") || tokens[0].equals("report")){
            String respHead = tokens[1];
            String[] respData = Arrays.copyOfRange(tokens, 2, tokens.length);
            result = extractDdParameters(respHead,respData);
        }else if(tokens[0].equals("set")){
            Log.d(LOGTAG, "Thingy responded with a \'set\' command [" + text + "].\r\n");
            result = 2;
        }else{
            Log.d(LOGTAG, "Thingy Responded with the following message:\r\n\t\'" + text + "\'.\r\n");
            result = 3;
        }
        return result;
    }

    private Integer extractDdParameters(String header, String[] data){
        int nData = data.length;
        Integer result = null;
        Log.d(LOGTAG, "extractDdParameters --- Thingy response message contains " + nData + " valid parameters to extract.\r\n");

        switch(header) {
            case Helper.DD_CMD_QUERY_DEVICES:{
                if(nData >= 7){
                    ntagId = data[0];
                    String[] addrTokens = Arrays.copyOfRange(data, 1, 7);
                    String tmpAddr = constructTagAddress(addrTokens);
                    BluetoothDevice bleDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(tmpAddr);
                    addNewDevice(bleDevice);
//                    addDeviceList(tmpDevice);
                }
                else {
                    result = -1;
                }
            } break;
            case Helper.DD_CMD_TAG_INFO:{
                if(nData >= 12){
                    ntagId = data[0];
                    String[] addrTokens = Arrays.copyOfRange(data, 1, 7);
                    String tmpAddr = constructTagAddress(addrTokens);
                    ntagAlias = data[7];
                    ntagRssi = Integer.parseInt(data[8]);
                    ntagRssiThresh = Integer.parseInt(data[9]);
                    ntagDebounceThresh = Integer.parseInt(data[10]);
                    ntagTimesSeen = Integer.parseInt(data[11]);
                    updateGuiTagInfo(ntagId,tmpAddr, ntagAlias, ntagRssiThresh, ntagDebounceThresh, ntagRssi, ntagTimesSeen);
                }
                else {
                    result = -1;
                }
            } break;
            case Helper.DD_CMD_TAG_ID:{
                if(nData >= 1){
                    ntagId = data[0];
                    updateGuiTagInfo(ntagId,null,null,null,null,null,null);
                    result = 0;
                }
                else {
                    result = -1;
                }
            } break;
            case Helper.DD_CMD_TAG_ADDRESS:{
                String[] addrTokens;
                if(nData == 6){
                    addrTokens = Arrays.copyOfRange(data, 0, 6);
                }
                else if(nData > 6){
                    addrTokens = Arrays.copyOfRange(data, 1, 1+6);
                }
                else{
                    Log.e(LOGTAG, "extractDdParameters --- Not enough address bytes received from doggy door. Received [" + nData + "] / 6 bytes.\r\n");
                    result = -1;
                    break;
                }
                String tmpAddr = constructTagAddress(addrTokens);
                updateGuiTagInfo(null,tmpAddr,null,null,null,null,null);
                result = 0;
            } break;
            case Helper.DD_CMD_TAG_ALIAS:{
                if(nData == 1){
                    ntagAlias = data[0];
                }
                else if(nData > 1){
                    String tmpDevId = data[0];
                    ntagAlias = data[1];
                }
                else{
                    result = -1;
                    break;
                }
                updateGuiTagInfo(null,null, ntagAlias,null,null,null,null);
                result = 0;
            } break;
            case Helper.DD_CMD_TAG_RSSI:{
                if(nData == 1){
                    ntagRssi = Integer.parseInt(data[0]);
                }
                else if(nData >= 2){
                    String tmpDevId = data[0];
                    ntagRssi = Integer.parseInt(data[1]);
                }
                else{
                    result = -1;
                    break;
                }
                updateGuiTagInfo(null,null,null,null,null, ntagRssi,null);
            } break;
            case Helper.DD_CMD_TAG_RSSI_THRESHOLD:{
                if(nData == 1){
                    ntagRssiThresh = Integer.parseInt(data[0]);
                }
                else if(nData > 1){
                    String tmpDevId = data[0];
                    ntagRssiThresh = Integer.parseInt(data[1]);
                }
                else{
                    result = -1;
                    break;
                }
                updateGuiTagInfo(null,null,null, ntagRssiThresh,null,null,null);
                result = 0;
            } break;
            case Helper.DD_CMD_TAG_DEBOUNCE_THRESHOLD:{
                if(nData == 1){
                    ntagDebounceThresh = Integer.parseInt(data[0]);
                }
                else if(nData > 1){
                    String tmpDevId = data[0];
                    ntagDebounceThresh = Integer.parseInt(data[1]);
                }
                else{
                    result = -1;
                    break;
                }
                updateGuiTagInfo(null,null,null,null, ntagDebounceThresh,null,null);
                result = 0;
            } break;
            case Helper.DD_CMD_TAG_STATISTICS:{
                if(nData == 1){
                    ntagTimesSeen = Integer.parseInt(data[0]);
                }
                else if(nData > 1){
                    String tmpDevId = data[0];
                    ntagTimesSeen = Integer.parseInt(data[1]);
                }
                else{
                    result = -1;
                    break;
                }
                updateGuiTagInfo(null,null,null,null,null,null, ntagTimesSeen);
            } break;                       /** TODO */
            case Helper.DD_CMD_TAG_TIMEOUT:{
                Log.d(LOGTAG, "extractDdParameters ---- TODO: DD_CMD_TAG_LAST_ACTIVITY...\r\n");
                result = 0;
            } break;                          /** TODO */
            case Helper.DD_CMD_BATTERY_LEVEL:{
                if(nData == 1){
                    nBatVolt = Integer.parseInt(data[0]);
                    nBatPercent = null;
                }
                else if(nData >= 1){
                    nBatVolt = Integer.parseInt(data[0]);
                    nBatPercent = Integer.parseInt(data[1]);
                }
                else {
                    nBatVolt = null;
                    nBatPercent = null;
                    Log.e(LOGTAG, "extractDdParameters ---- DD_CMD_BATTERY_LEVEL.\r\n");
                    result = -1;
                    break;
                }
                Log.d(LOGTAG, "extractDdParameters --- Extracted Battery Life: " + nBatVolt + " mV (" + nBatPercent + "%).\r\n");
                updateGuiDoggyDoor(nBatVolt,nBatPercent,null,null,null,null,null,null,null);
                result = 0;
            } break;
            case Helper.DD_CMD_CHARGE_STATUS:{
                if(nData >= 1){
                    Integer chargeStatusCode = Integer.parseInt(data[0]);
                    nChargeStatus = decodeBatChargeStatus(chargeStatusCode);
                }
                else {
                    nChargeStatus = null;
                    Log.e(LOGTAG, "extractDdParameters ---- DD_CMD_CHARGE_STATUS.\r\n");
                    result = -1;
                    break;
                }
                updateGuiDoggyDoor(null,null,nChargeStatus,null,null,null,null,null,null);
                result = 0;
            } break;
            case Helper.DD_CMD_BATTERY_INFO:{
                if(nData == 1){
                    nBatVolt = Integer.parseInt(data[0]);
                    nBatPercent = null;
                    nChargeStatus = null;
                }
                else if(nData == 2){
                    nBatVolt = Integer.parseInt(data[0]);
                    nBatPercent = Integer.parseInt(data[1]);
                    nChargeStatus = null;
                }
                else if(nData >= 3){
                    nBatVolt = Integer.parseInt(data[0]);
                    nBatPercent = Integer.parseInt(data[1]);
                    Integer chargeStatusCode = Integer.parseInt(data[2]);
                    nChargeStatus = decodeBatChargeStatus(chargeStatusCode);
                }
                else {
                    nBatVolt = null;
                    nBatPercent = null;
                    nChargeStatus = null;
                    Log.e(LOGTAG, "extractDdParameters ---- DD_CMD_BATTERY_INFO.\r\n");
                    result = -1;
                    break;
                }
                Log.d(LOGTAG, "extractDdParameters --- Extracted Battery Information: " + nBatVolt + " mV (" + nBatPercent + "%) -- Status = " + nChargeStatus + ".\r\n");
                updateGuiDoggyDoor(nBatVolt,nBatPercent,nChargeStatus,null,null,null,null,null,null);
                result = 0;
            } break;
            case Helper.DD_CMD_DOOR_ENCODER_LIMIT:{
                if(nData >= 1){
                    nEncLimit = Integer.parseInt(data[0]);
                }
                else{
                    nEncLimit = null;
                    result = -1;
                    break;
                }
                Log.d(LOGTAG, "extractDdParameters ---- Extracted Encoder Limit = " + nEncLimit + "...\r\n");
                updateGuiDoggyDoor(null,null,null,null,null,null,null,null,nEncLimit);
                result = 0;
            } break;
            case Helper.DD_CMD_DOOR_SPEED:{
                if(nData >= 1){
                    nMotorSpd = Double.parseDouble(data[0]);
                }
                else{
                    nMotorSpd = null;
                    result = -1;
                    break;
                }
                Log.d(LOGTAG, "extractDdParameters ---- Extracted Encoder Limit = " + nMotorSpd + "...\r\n");
                updateGuiDoggyDoor(null,null,null,null,null,null,null,nMotorSpd,null);
                result = 0;
            } break;
            case Helper.DD_CMD_DOOR_STATUS:{
                if(nData >= 1){
                    nIsOpen = Boolean.parseBoolean(data[0]);
                    nIsLocked = Boolean.parseBoolean(data[1]);
                }
                else {
                    nIsOpen = null;
                    nIsLocked = null;
                    Log.e(LOGTAG, "extractDdParameters ---- DD_CMD_DOOR_STATUS.\r\n");
                    result = -1;
                    break;
                }
                updateGuiDoggyDoor(null,null,null,nIsLocked,nIsOpen,null,null,null,null);
                result = 0;
            } break;
            case Helper.DD_CMD_LIMIT_SWITCH_STATUS:{
                if(nData >= 2){
                    String switchId = data[0];
                    if(switchId.equals("upper_switch")){
                        nUpLimitSwitchState = Boolean.parseBoolean(data[1]);
                    }else if(switchId.equals("lower_switch")){
                        nLowLimitSwitchState = Boolean.parseBoolean(data[1]);
                    }
                }
                else {
                    Log.d(LOGTAG, "extractDdParameters ---- DD_CMD_LIMIT_SWITCH_STATUS unable to get enough data.\r\n");
                    result = -1;
                    break;
                }
                updateGuiDoggyDoor(null,null,null,null,null,nUpLimitSwitchState,nLowLimitSwitchState,null,null);
                result = 0;
            } break;
            case Helper.DD_CMD_TAG_LAST_ACTIVITY:{
                Log.d(LOGTAG, "extractDdParameters ---- TODO: DD_CMD_TAG_LAST_ACTIVITY...\r\n");
                result = 0;
            } break;                    /** TODO */
            case Helper.DD_CMD_BLE_UPDATE_RATE:{
                Log.d(LOGTAG, "extractDdParameters ---- TODO: DD_CMD_BLE_UPDATE_RATE...\r\n");
                result = 0;
            } break;                      /** TODO */
            case Helper.DD_CMD_DOOR_UPDATE_RATE:{
                Log.d(LOGTAG, "extractDdParameters ---- TODO: DD_CMD_DOOR_UPDATE_RATE...\r\n");
                result = 0;
            } break;                     /** TODO */
            case Helper.DD_CMD_TAG_CHECK_UPDATE_RATE:{
                Log.d(LOGTAG, "extractDdParameters ---- TODO: DD_CMD_TAG_CHECK_UPDATE_RATE...\r\n");
                result = 0;
            } break;                /** TODO */
            default:{
                Log.d(LOGTAG, "extractDdParameters: Unable to parse response.\r\n");
                result = 0;
            }break;
        }
        return result;
    }


    private void updateGuiTagInfo(String id_, String address_, String alias_, Integer rssiThresh_,
                                  Integer debounceThresh_, Integer rssi_, Integer times_seen_){
        if(id_ != null){
            txtDevId.setText(id_);
        }
        if(address_ != null){
            txtDevAddr.setText(address_);
        }
        if(alias_ != null){
            txtDevAlias.setText(alias_);
        }
        if(rssiThresh_ != null){
            txtDevRssiThresh.setText(rssiThresh_.toString());
        }
        if(debounceThresh_ != null){
            txtDevDebounceThresh.setText(debounceThresh_.toString());
        }
        if(rssi_ != null){
            txtDevRssi.setText(rssi_.toString());
        }
        if(times_seen_ != null){
            txtTimesSeen.setText(times_seen_.toString());
        }
    }

    private void updateGuiDoggyDoor(Integer battery_voltage, Integer battery_percent, String charge_status,
                                    Boolean isLocked, Boolean isOpen, Boolean upper_switch_state, Boolean lower_switch_state,
                                    Double door_speed, Integer encoder_limit){
        if(battery_voltage != null && battery_percent != null){
            Double volts = battery_voltage.doubleValue() / 1000.0;
            txtBatLife.setText(String.format("%.2fV (%d%%)", volts, battery_percent));
        }else if(battery_voltage != null && battery_percent == null){
            Double volts = battery_voltage.doubleValue() / 1000.0;
            txtBatLife.setText(String.format("%.2fV", volts));
        }else if(battery_voltage == null && battery_percent != null){
            txtBatLife.setText(String.format("%d%%", battery_percent));
        }

        if(charge_status != null){
            txtChargeStatus.setText(charge_status);
        }
        if(isLocked != null){
            setLedDrawable(isLocked,ledLockStatus);
        }
        if(isOpen != null){
            setLedDrawable(isOpen,ledDoorStatus);
        }
        if(upper_switch_state != null){
            setLedDrawable(upper_switch_state,ledUpLimitSwitch);
        }
        if(lower_switch_state != null){
            setLedDrawable(lower_switch_state,ledLowLimitSwitch);
        }

        if(door_speed != null){
            txtMotorSpd.setText(String.format("%.2f", door_speed));
        }
        if(encoder_limit != null){
            txtEncLimit.setText(encoder_limit.toString());
        }
    }

    private void populateList() {
        /* Initialize device list container */
        Log.d(LOGTAG, "populateList");
        deviceLst = new ArrayList<BluetoothDevice>();
        deviceAdapter = new DeviceAdapter(this, deviceLst);
        devRssiValues = new HashMap<String, Integer>();

        newDevicesListView = (ListView) findViewById(R.id.listPetTags);
        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
    }
    private void addDeviceList(BluetoothDevice device) {
        boolean deviceFound = false;
        Log.d(LOGTAG, "addDeviceList: " + device.getAddress() + "\r\n");
        for (BluetoothDevice listDev : deviceList) {
            Log.d(LOGTAG, "addDeviceList: Checking stored device [" + device.getAddress() + "]...\r\n");
            if (listDev.getAddress().equals(device.getAddress())) {
                Log.d(LOGTAG, "addDeviceList: Found Device [" + device.getAddress() + "] in stored devices...\r\n");
                deviceFound = true;
                break;
            }
        }
        if (!deviceFound) {
            Log.d(LOGTAG, "ADDING DEVICE TO LIST....");
            deviceList.add(device);
            sendAddDevCommand(device);
        }
    }
    private void addNewDevice(BluetoothDevice device) {
        boolean deviceFound = false;
        Log.d(LOGTAG, "addNewDevice: " + device.getAddress() + "\r\n");
        for (BluetoothDevice listDev : deviceList) {
            Log.d(LOGTAG, "addNewDevice: Checking stored device [" + device.getAddress() + "]...\r\n");
            if (listDev.getAddress().equals(device.getAddress())) {
                Log.d(LOGTAG, "addNewDevice: Found Device [" + device.getAddress() + "] in stored devices...\r\n");
                deviceFound = true;
                break;
            }
        }
        if (!deviceFound) {
            Log.d(LOGTAG, "ADDING NEW DEVICE TO LIST....");
            deviceList.add(device);
            deviceLst.add(device);
            txtEmptyList.setVisibility(View.GONE);
            deviceAdapter.notifyDataSetChanged();
        }
    }
    private void sendAddDevCommand(BluetoothDevice device){
        deviceLst.add(tmpDevice);
        String address = device.getAddress();
        String name = device.getName();
        txtEmptyList.setVisibility(View.GONE);
        deviceAdapter.notifyDataSetChanged();

        String nusMsg = "add_dev " + name + " " + address;
        byte[] value;
        try { //send data to service
            value = nusMsg.getBytes("UTF-8");
            mService.writeRXCharacteristic(value);
            //Update the log with time stamp
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private void delDeviceList(BluetoothDevice device) {
        boolean deviceFound = false;
        Log.d(LOGTAG, "delDeviceList: " + device.getAddress() + "\r\n");
        for (BluetoothDevice listDev : deviceList) {
            Log.d(LOGTAG, "\t device - " + listDev.getAddress());
            if (listDev.getAddress().equals(device.getAddress())) {
                Log.d(LOGTAG, "delDeviceList: Found Device [" + device.getAddress() + "] in stored devices...\r\n");
                deviceFound = true;
                break;
            }
        }

        if (deviceFound) {
            Log.d(LOGTAG, "DELETING DEVICE FROM LIST....");
            deviceList.remove(device);
            sendDelDevCommand(device);
        }


    }
    private void sendDelDevCommand(BluetoothDevice device){
        String address = device.getAddress();
        String name = device.getName();
        String nusMsg = "del_dev " + name + " " + address;
        byte[] buf;
        try { //send data to service
            buf = nusMsg.getBytes("UTF-8");
            mService.writeRXCharacteristic(buf);
            //Update the log with time stamp
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private void sendGetCommand(String paramId, String device){
        String nusMsg = "get " + paramId + " " + device + "\0";
        byte[] buf;
        try {   //send data to service
            buf = nusMsg.getBytes("UTF-8");
            mService.writeRXCharacteristic(buf);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    private void sendSetCommand(String paramId, String device){
        String nusMsg = "set " + paramId + " " + device + "\0";
        byte[] buf;
        try {   //send data to service
            buf = nusMsg.getBytes("UTF-8");
            mService.writeRXCharacteristic(buf);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    private void sendSetCommand(String paramId, String device, String value){
        String nusMsg = "set " + paramId + " " + device + " " + value + "\0";
        byte[] buf;
        try {   //send data to service
            buf = nusMsg.getBytes("UTF-8");
            mService.writeRXCharacteristic(buf);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    private void sendSetCommand(String paramId, String device, Integer value){
        String nusMsg = "set " + paramId + " " + device + " " + value.toString() + "\0";
        byte[] buf;
        try {   //send data to service
            buf = nusMsg.getBytes("UTF-8");
            mService.writeRXCharacteristic(buf);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    private void sendSetCommand(String paramId, String device, Float value){
        String nusMsg = "set " + paramId + " " + device + " " + value.toString() + "\0";
        byte[] buf;
        try {   //send data to service
            buf = nusMsg.getBytes("UTF-8");
            mService.writeRXCharacteristic(buf);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private String constructTagAddress(String[] addressTokens_){
        ntagAddr = new ArrayList<Integer>(6);
        StringJoiner strAddr = new StringJoiner(":");
        for(String tk: addressTokens_){
            strAddr.add(tk.toUpperCase());
            ntagAddr.add(Integer.parseInt( tk, 16 ));
        }
        return strAddr.toString();
    }
    private String decodeBatChargeStatus(Integer charge_status_code){
        String strChargeStatus = null;
        if(charge_status_code.equals(Helper.DD_BAT_CHARGE_STATUS_NOT_CHARGING)){
            strChargeStatus = "Not Charging";
        }
        else if(charge_status_code.equals(Helper.DD_BAT_CHARGE_STATUS_CHARGING)){
            strChargeStatus = "Charging";
        }
        else if(charge_status_code.equals(Helper.DD_BAT_CHARGE_STATUS_CHARGING_FINISHED)){
            strChargeStatus = "Charging Finished";
        }
        else if(charge_status_code.equals(Helper.DD_BAT_CHARGE_STATUS_CHARGING_DISCONNECTED)){
            strChargeStatus = "Not Charging (Disconnected)";
        }
        else if(charge_status_code.equals(Helper.DD_BAT_CHARGE_STATUS_LOW)){
            strChargeStatus = "Battery Low!";
        }
        else if(charge_status_code.equals(Helper.DD_BAT_CHARGE_STATUS_FULL)){
            strChargeStatus = "Battery Full";
        }
        else if(charge_status_code.equals(Helper.DD_BAT_CHARGE_STATUS_ERROR)){
            strChargeStatus = "Measurement Error";
        }
        return strChargeStatus;
    }

    private void clearStoredDevices(){
        Log.d(LOGTAG, "ADDING NEW DEVICE TO LIST....");
        deviceList.clear();
        deviceLst.clear();
        txtEmptyList.setVisibility(View.GONE);
        deviceAdapter.notifyDataSetChanged();
    }

    public void requestStoredDevices(){
        showMessage("Requesting information on all pet tags stored by the DoggyDoor...");
        String message = "query devices\0";
        byte[] value;
        try {
            value = message.getBytes("UTF-8");
            mService.writeRXCharacteristic(value);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
        }
        catch (UnsupportedEncodingException e) { e.printStackTrace(); }
    }

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

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device;
            String tmpAddr;
            String tmpName;
            if (updateview != null) { updateview.setBackgroundColor(Color.TRANSPARENT); }
            updateview = view;

            if(mSelectedItem == position){
                view.setBackgroundColor(Color.TRANSPARENT);
                mSelectedItem = -1;
                tmpAddr = "";
                tmpName = "";
            }else{
                view.setBackgroundColor(Color.CYAN);
                mSelectedItem = position;
                device = deviceList.get(position);
                /** static locally stored devices */
                tmpName = device.getName();
                tmpAddr = device.getAddress();
            }

            if(!tmpName.equals("")){ sendGetCommand(Helper.DD_CMD_TAG_INFO,tmpName); }
            else{ clearTagGui(false); }
            Log.d(LOGTAG, "mDeviceClickListener --  device [" + mSelectedItem + "] selected - " + tmpName + " (" + tmpAddr + ")");
        }
    };

    @Override
    public void onStart() {
        super.onStart();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOGTAG, "onDestroy()");
        try { LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver); }
        catch (Exception ignore) { Log.e(LOGTAG, ignore.toString()); }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Helper.REQUEST_SELECT_DEVICE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                    String tmpName = BluetoothAdapter.getDefaultAdapter().getName();
                    Log.i(LOGTAG, "... onActivityResult CONNECTING ----- device.address == " + tmpName + " (" + mDevice + ")  mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);
                }
                else if(resultCode == Helper.RESULT_ADD_USER && data != null){
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    tmpDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                    addDeviceList(tmpDevice);
                }break;
            case Helper.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(LOGTAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(LOGTAG, "wrong request code");
                break;
        }
    }

    private void showMessage(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.search:
                return true;
            case R.id.debug:
//                Intent newIntent = new Intent(MainActivity.this, ConfigActivity.class);
//                startActivity(newIntent);
//                setContentView(R.layout.config_activity);
                return true;
            case R.id.connect:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) { vg = (ViewGroup) convertView; }
            else { vg = (ViewGroup) inflater.inflate(R.layout.device_element, null); }

            BluetoothDevice device = devices.get(position);
            final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
            final TextView tvname = ((TextView) vg.findViewById(R.id.name));
            final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);
            final TextView tvrssi = (TextView) vg.findViewById(R.id.rssi);
            tvrssi.setVisibility(View.VISIBLE);

            tvname.setText(device.getName());
            tvadd.setText(device.getAddress());
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(LOGTAG, "device::" + device.getName());
                tvname.setTextColor(Color.BLACK);
                tvadd.setTextColor(Color.BLACK);
                tvpaired.setTextColor(Color.GRAY);
                tvpaired.setVisibility(View.VISIBLE);
                tvpaired.setText(R.string.paired);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.BLACK);
            } else {
                tvname.setTextColor(Color.BLACK);
                tvadd.setTextColor(Color.BLACK);
                tvpaired.setVisibility(View.GONE);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.BLACK);
            }
            return vg;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
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


    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(NusService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(LOGTAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        btnAddDev.setEnabled(true);
                        txtBatLbl.setVisibility(View.VISIBLE);
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - ready");
                        mState = UART_PROFILE_CONNECTED;
                        isConnectedToDd = true;
                    }
                });
            }

            if (action.equals(NusService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(LOGTAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        btnAddDev.setEnabled(false);
                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        clearTagGui(true);
                        clearDoorGui(true);
                        clearStoredDevices();
                        isConnectedToDd = false;
                    }
                });
            }

            if (action.equals(NusService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
                long dt = 0;
                long t0 = System.currentTimeMillis();
                long t1;
                while(dt < CONNECT_DT){
                    t1 = System.currentTimeMillis();
                    dt = t1 - t0;
                    // Log.d(LOGTAG, "Waiting " + dt/1000.0 + " seconds.\r\n");
                }
                requestStoredDevices();
                requestDoorStatus();
                queryDoorEncoder();
                queryMotorSpeed();
                queryDoorBattery();
            }

            if (action.equals(NusService.ACTION_DATA_AVAILABLE)) {
                final byte[] txValue = intent.getByteArrayExtra(NusService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try { parseDdResponse(txValue); }
                        catch (Exception e) { Log.e(LOGTAG, e.toString()); }
                    }
                });
            }

            if (action.equals(NusService.DEVICE_DOES_NOT_SUPPORT_UART)){
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
                isConnectedToDd = false;
            }

            if (action.equals(NusService.BLUETOOTH_NULL)){
                showMessage("Not connected to Doggy Door. Try connecting again.");
                mService.disconnect();
                isConnectedToDd = false;
            }
        }
    };
}