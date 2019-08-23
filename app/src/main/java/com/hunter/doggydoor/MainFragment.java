package com.hunter.doggydoor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
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

public class MainFragment extends Fragment {
    /** *************    Constants    ****************** */
    private static final String LOGTAG = "YEET.MainFragment";
    private static boolean debug_nus_io = false;

    public static final int UART_PROFILE_READY = 10;
    public static final int UART_PROFILE_CONNECTED = 20;
    public static final int UART_PROFILE_DISCONNECTED = 21;
    private static Drawable picLedInactive;
    private static Drawable picLedActive;

    /** *************    State Parameters    ****************** */
    public static int mSelectedItem = -1;
    private static Context mContext;
    public static boolean isConnectedToDd = false;
    private static boolean flagMotorForceStopped = false;
    public static int mState = UART_PROFILE_DISCONNECTED;
    public static String connectedDevice = null;
    public static String btnConnectDisconnectText = "Connect";

    /** *************    Helper Variables    ****************** */
    private static Integer ntagTimesSeen = null;
    private static Integer ntagDebounceThresh = null;
    private static Integer ntagRssiThresh = null;
    private static Integer ntagRssi = null;
    private static String ntagAlias = null;
    private static String ntagId = null;
    private static String nChargeStatus = null;
    private static Integer nBatVolt = null;
    private static Integer nBatPercent = null;
    private static Double nMotorSpd = null;
    private static Integer nEncLimit = null;
    private static Boolean nIsOpen = null;
    private static Boolean nIsLocked = null;
    private static Boolean nUpLimitSwitchState = null;
    private static Boolean nLowLimitSwitchState = null;
    private static ArrayList<Integer> ntagAddr;
    private static ArrayList<String> deviceStrList;

    /** *************    Bluetooth Objects    ****************** */
    public static String nAliasLbl = null;
    private static BluetoothDevice tmpDevice = null;
    public static ArrayList<BluetoothDevice> deviceList;
    public static BluetoothDevice mLastBleCentralDevice = null;

    /** ********************************************************************************************
    *                                      GUI Elements
    * ****************************************************************************************** */
    // Buttons
    public static Button btnConnectDisconnect;
    public static Button btnAddDev;
    public static Button btnGetDoorStatus;
    public static Button btnLockDoor;
    public static Button btnUnockDoor;
    public static Button btnCloseDoor;
    public static Button btnOpenDoor;
    public static Button btnStopDoor;
    public static Button btnSetMotorSpd;
    public static Button btnGetMotorSpd;
    public static Button btnSetEncLimit;
    public static Button btnGetEncLimit;

    private static RadioGroup radioDoorOperationGroup;

    // Text Input Fields
    public static EditText editAlias;
    public static EditText editDebounceThresh;
    public static EditText editRssiThresh;
    public static EditText editEncLimit;
    public static EditText editMotorSpd;

    // Text Views
    public static TextView txtDevId;
    public static TextView txtDevAddr;
    public static TextView txtDevAlias;
    public static TextView txtDevRssiThresh;
    public static TextView txtDevDebounceThresh;
    public static TextView txtTimesSeen;
    public static TextView txtDevRssi;
    public static TextView txtBatLbl;
    public static TextView txtBatLife;
    public static TextView txtChargeStatus;
    public static TextView txtEncLimit;
    public static TextView txtMotorSpd;

    // Graphic TextViews
    public static TextView ledLockStatus;
    public static TextView ledDoorStatus;
    public static TextView ledUpLimitSwitch;
    public static TextView ledLowLimitSwitch;
    public static TextView ddRefreshIcon;

    /** *************    List View and GUI Helpers    ****************** */
    public static TextView txtEmptyList;
    Map<String, Integer> devRssiValues = null;
    public static DeviceAdapter deviceAdapter = null;
    public static List<BluetoothDevice> deviceLst = null;
    public static ListView newDevicesListView = null;
    View updateview;

    // Required empty public constructor
    public MainFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG,"onCreate: " + isConnectedToDd);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOGTAG,"onCreateView");
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);
        Log.d(LOGTAG,"onViewCreated");
        picLedInactive = getResources().getDrawable(android.R.drawable.presence_invisible);
        picLedActive = getResources().getDrawable(android.R.drawable.presence_online);

        initGui();
        populateList();

        if(mState == UART_PROFILE_DISCONNECTED) {
            deviceList = new ArrayList<BluetoothDevice>();
            deviceStrList = new ArrayList<String>();
            clearTagGui(true);
            clearDoorGui(true);
        }
        mContext = getContext();
    }

    /** *******************************************************************************************
     *                                    END onCreate()
     * ******************************************************************************************* */

    private void initGui(){
        newDevicesListView = (ListView) getActivity().findViewById(R.id.listPetTags);

        btnConnectDisconnect = (Button) getView().findViewById(R.id.btn_scan);
        btnAddDev = (Button) getView().findViewById(R.id.btn_add_dev);
        Button btnDelDev = (Button) getView().findViewById(R.id.btn_del_dev);
        Button btnGetRssiThresh = (Button) getView().findViewById(R.id.btn_get_rssi_thresh);
        Button btnGetAlias = (Button) getView().findViewById(R.id.btn_get_alias);
        Button btnGetDebounceThresh = (Button) getView().findViewById(R.id.btn_get_debounce_thresh);
        Button btnSetRssiThresh = (Button) getView().findViewById(R.id.btn_set_rssi_thresh);
        Button btnSetAlias = (Button) getView().findViewById(R.id.btn_set_alias);
        Button btnSetDebounceThresh = (Button) getView().findViewById(R.id.btn_set_debounce_thresh);
        Button btnRefreshTag = (Button) getView().findViewById(R.id.btn_refresh);

        /** Deprecated: Use with fragment_main_backup */
        Button btnGetRssi = (Button) getView().findViewById(R.id.btn_get_rssi);
        Button btnGetStats = (Button) getView().findViewById(R.id.btn_get_stats);
        Button btnGetBatInfo = (Button) getView().findViewById(R.id.btn_get_bat);
        btnGetDoorStatus = (Button) getView().findViewById(R.id.btn_get_door_status);
        btnLockDoor = (Button) getView().findViewById(R.id.btn_lock_door);
        btnUnockDoor = (Button) getView().findViewById(R.id.btn_unlock_door);
        btnCloseDoor = (Button) getView().findViewById(R.id.btn_close_door);
        btnOpenDoor = (Button) getView().findViewById(R.id.btn_open_door);

        Switch switchDoorLock = (Switch) getView().findViewById(R.id.swDoorLock);

        btnStopDoor = (Button) getView().findViewById(R.id.btn_stop_motor);
        btnSetMotorSpd = (Button) getView().findViewById(R.id.btn_set_spd);
        btnGetMotorSpd = (Button) getView().findViewById(R.id.btn_get_spd);
        btnSetEncLimit = (Button) getView().findViewById(R.id.btn_set_enc_limit);
        btnGetEncLimit = (Button) getView().findViewById(R.id.btn_get_enc_limit);

        editAlias = (EditText) getView().findViewById(R.id.editAlias);
        editDebounceThresh = (EditText) getView().findViewById(R.id.editDebounceThresh);
        editRssiThresh = (EditText) getView().findViewById(R.id.editRssiThresh);
        editEncLimit = (EditText) getView().findViewById(R.id.editEncLimit);
        editMotorSpd = (EditText) getView().findViewById(R.id.editSpd);

        txtDevId  = (TextView) getView().findViewById(R.id.txtId);
        txtDevAddr = (TextView) getView().findViewById(R.id.txtAddr);
        txtDevAlias = (TextView) getView().findViewById(R.id.txtAlias);
        txtDevRssiThresh = (TextView) getView().findViewById(R.id.txtRssiThresh);
        txtDevDebounceThresh = (TextView) getView().findViewById(R.id.txtDebounceThresh);
        txtTimesSeen = (TextView) getView().findViewById(R.id.txtTimesSeen);
        txtDevRssi = (TextView) getView().findViewById(R.id.txtRssi);
        txtEmptyList = (TextView) getView().findViewById(R.id.empty);
        txtBatLbl = (TextView) getView().findViewById(R.id.txtBatLbl);
        txtBatLife = (TextView) getView().findViewById(R.id.txtBatLife);
        txtChargeStatus = (TextView) getView().findViewById(R.id.txtChargeStatus);
        txtEncLimit = (TextView) getView().findViewById(R.id.txtEncLimit);
        txtMotorSpd = (TextView) getView().findViewById(R.id.txtSpd);
        ledLockStatus = (TextView) getView().findViewById(R.id.led_lock_status);
        ledDoorStatus = (TextView) getView().findViewById(R.id.led_open_status);
        ledUpLimitSwitch = (TextView) getView().findViewById(R.id.led_top_swtich);
        ledLowLimitSwitch = (TextView) getView().findViewById(R.id.led_bottom_switch);

        radioDoorOperationGroup  = (RadioGroup) getView().findViewById(R.id.radioGroupOperation);
        RadioButton radioNormalOperationButton = (RadioButton) getView().findViewById(R.id.radioNormalOperation);
        RadioButton radioOpenOperationButton = (RadioButton) getView().findViewById(R.id.radioOpenOperation);
        RadioButton radioCloseOperationButton = (RadioButton) getView().findViewById(R.id.radioCloseOperation);
        ddRefreshIcon = (TextView) getView().findViewById(R.id.ic_dd_refresh);

        // Used for handling switching between fragments w/o changing GUI
        if(isConnectedToDd){
            ((TextView) getView().findViewById(R.id.deviceName)).setText(connectedDevice + " - ready");
            btnConnectDisconnect.setText("Disconnect");
            // Request internals from connected doggy door
            requestStoredDevices();
            Helper.nonblockingWait(100,false);
            requestDoorStatus();
            Helper.nonblockingWait(100,false);
            queryDoorEncoder();
            Helper.nonblockingWait(100,false);
            queryMotorSpeed();
            Helper.nonblockingWait(100,false);
            queryDoorBattery();
        }
        else{
            ((TextView) getView().findViewById(R.id.deviceName)).setText("Not Connected");
            btnConnectDisconnect.setText("Connect");
        }

        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!MainActivity.mBtAdapter.isEnabled()) {
                    Log.i(LOGTAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, Helper.REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {
                        Intent newIntent = new Intent(getActivity(), ConnectDoggyDoorActivity.class);
                        startActivityForResult(newIntent, Helper.REQUEST_SELECT_DEVICE);
                    }
                    else {
                        if (MainActivity.mDevice != null) { MainActivity.mService.disconnect(); }
                    }
                }
            }
        });

        btnAddDev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!MainActivity.mBtAdapter.isEnabled()) {
                    Log.i(LOGTAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, Helper.REQUEST_ENABLE_BT);
                }
                else {
                    Intent newIntent2 = new Intent(getActivity(), AddDoggyTagActivity.class);
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
                    if (updateview != null) { updateview.setBackgroundColor(Color.TRANSPARENT); }
                }
            }
        });

        btnGetAlias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    Log.d(LOGTAG,"Getting Alias for device [" + txtDevId.getText() + "]...");
                    sendGetCommand(Helper.DD_CMD_TAG_ALIAS,txtDevId.getText().toString());
                }
                else{
                    showMessage(mContext,"No target device selected. Please select the pet tag you wish to get the alias of.");
                }
            }
        });

        btnSetAlias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    if(!editAlias.getText().toString().equals("")){
                        Log.d(LOGTAG,"Setting Alias for device [" + txtDevId.getText() + "] to -> " + editAlias.getText() + "...");
                        sendSetCommand(Helper.DD_CMD_TAG_ALIAS,txtDevId.getText().toString(),editAlias.getText().toString());
                        nAliasLbl = editAlias.getText().toString();
                        txtDevAlias.setText(editAlias.getText().toString());
                        editAlias.setText("");
                        editAlias.setEnabled(false);
                        editAlias.setEnabled(true);
                        updateTagListAlias();
                    }
                    else{
                        Log.d(LOGTAG,"No device alias provided, not setting Alias for device [" + txtDevId.getText() + "].");
                        nAliasLbl = null;
                        showMessage(mContext,"No alias provided. Please input the alias you wish to change for the selected pet tag.");
                    }
                }else{
                    nAliasLbl = null;
                    showMessage(mContext,"No target device selected. Please select the pet tag you wish to change the alias of.");
                }
            }
        });

        btnGetRssiThresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().toString().equals("")){
                    Log.d(LOGTAG,"Getting RSSI Threshold for device [" + txtDevId.getText() + "]...");
                    sendGetCommand(Helper.DD_CMD_TAG_RSSI_THRESHOLD,txtDevId.getText().toString());
                }else{
                    showMessage(mContext,"No target device selected. Please select the pet tag you wish to get the RSSI threshold of.");
                }
            }
        });

        btnSetRssiThresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    if(!editRssiThresh.getText().toString().equals("")){
                        Log.d(LOGTAG,"Setting RSSI Threshold for device [" + txtDevId.getText() + "] to -> " + editRssiThresh.getText() + "...");
                        sendSetCommand(Helper.DD_CMD_TAG_RSSI_THRESHOLD,txtDevId.getText().toString(),editRssiThresh.getText().toString());
                        txtDevRssiThresh.setText(editRssiThresh.getText().toString());
                        editRssiThresh.setText("");
                        editRssiThresh.setEnabled(false);
                        editRssiThresh.setEnabled(true);
                    }
                    else{
                        Log.d(LOGTAG,"No device RSSI threshold provided, not setting RSSI Threshold for device [" + txtDevId.getText() + "].");
                        showMessage(mContext,"No RSSI threshold provided. Please input the RSSI threshold you wish to change for the selected pet tag.");
                    }
                }else{
                    showMessage(mContext,"No target device selected. Please select the pet tag you wish to change the RSSI threshold of.");
                }
            }
        });

        btnGetDebounceThresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().toString().equals("")){
                    Log.d(LOGTAG,"Getting Debounce Threshold for device [" + txtDevId.getText() + "]...");
                    sendGetCommand(Helper.DD_CMD_TAG_DEBOUNCE_THRESHOLD,txtDevId.getText().toString());
                }else{
                    showMessage(mContext,"No target device selected. Please select the pet tag you wish to get the debounce threshold of.");
                }
            }
        });

        btnSetDebounceThresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    if(!editDebounceThresh.getText().toString().equals("")){
                        Log.d(LOGTAG,"Setting Debounce Threshold for device [" + txtDevId.getText() + "] to -> " + editDebounceThresh.getText() + "...");
                        sendSetCommand(Helper.DD_CMD_TAG_DEBOUNCE_THRESHOLD,txtDevId.getText().toString(),editDebounceThresh.getText().toString());
                        txtDevDebounceThresh.setText(editDebounceThresh.getText().toString());
                        editDebounceThresh.setText("");
                        editDebounceThresh.setEnabled(false);
                        editDebounceThresh.setEnabled(true);
                    }
                    else{
                        Log.d(LOGTAG,"No device Debounce threshold provided, not setting Debounce Threshold for device [" + txtDevId.getText() + "].");
                        showMessage(mContext,"No debounce threshold provided. Please input the debounce threshold you wish to change for the selected pet tag.");
                    }
                }else{
                    showMessage(mContext,"No target device selected. Please select the pet tag you wish to change the debounce threshold of.");
                }
            }
        });
        /** Deprecated: Use with fragment_main_backup */
//        btnGetRssi.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(!txtDevId.getText().equals("")){
//                    Log.d(LOGTAG,"Getting RSSI for device [" + txtDevId.getText() + "]...");
//                    sendGetCommand(Helper.DD_CMD_TAG_RSSI,txtDevId.getText().toString());
//                }
//                else{
//                    showMessage(mContext,"No target device selected. Please select the pet tag you wish to get the RSSI of.");
//                }
//            }
//        });

        /** Deprecated: Use with fragment_main_backup */
//        btnGetStats.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(!txtDevId.getText().equals("")){
//                    Log.d(LOGTAG,"Getting Statistics for device [" + txtDevId.getText() + "]...");
//                    sendGetCommand(Helper.DD_CMD_TAG_STATISTICS,txtDevId.getText().toString());
//                }
//                else{
//                    showMessage(mContext,"No target device selected. Please select the pet tag you wish to get the statistics of.");
//                }
//            }
//        });

        btnRefreshTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!txtDevId.getText().equals("")){
                    Log.d(LOGTAG,"Refreshing information for device [" + txtDevId.getText() + "]...");
                    sendGetCommand(Helper.DD_CMD_TAG_INFO,txtDevId.getText().toString());
                }
                else{ showMessage(mContext,"No target device selected. Please select the pet tag you wish to see."); }
            }
        });

        ddRefreshIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestDoorStatus();
                Helper.nonblockingWait(100,false);
                queryDoorBattery();
                Helper.nonblockingWait(100,false);
                queryDoorEncoder();
                Helper.nonblockingWait(100,false);
                queryMotorSpeed();
            }
        });

        /** Deprecated: Use with fragment_main_backup */
//        btnGetBatInfo.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) { queryDoorBattery(); }
//        });
        /** Deprecated: Use with fragment_main_backup */
//        btnGetDoorStatus.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) { requestDoorStatus(); }
//        });

        btnStopDoor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mState == UART_PROFILE_CONNECTED){
                    Log.d(LOGTAG,"Attempting to Stop DoggyDoor motor...");
                    sendSetCommand(Helper.DD_CMD_STOP_MOTOR,"dd");
                    // Update GUI elements based on current states
                    if(!flagMotorForceStopped){  btnStopDoor.setText("Resume"); }
                    else{                       btnStopDoor.setText("Stop"); }
                    flagMotorForceStopped = !flagMotorForceStopped;
                }
                else{ showMessage(mContext,"Not connected to Doggy Door. Try connecting again."); }
            }
        });

        /** Deprecated: Use with fragment_main_backup */
//        btnOpenDoor.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) { forceDoorOpen(); }
//        });
        /** Deprecated: Use with fragment_main_backup */
//        btnCloseDoor.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) { forceDoorClose(); }
//        });

        radioDoorOperationGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton checkedRadioButton = (RadioButton)group.findViewById(checkedId);
                if (checkedRadioButton.equals(radioNormalOperationButton)) { forceDoorNormal(); }
                else if(checkedRadioButton.equals(radioOpenOperationButton)) { forceDoorOpen(); }
                else if(checkedRadioButton.equals(radioCloseOperationButton)) { forceDoorClose(); }
            }
        });

        /** Deprecated: Use with fragment_main_backup */
//        btnLockDoor.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) { lockDoor(); }
//        });
        /** Deprecated: Use with fragment_main_backup */
//        btnUnockDoor.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) { unlockDoor(); }
//        });

        switchDoorLock.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){lockDoor();}
                else {unlockDoor();}
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
                        sendSetCommand(Helper.DD_CMD_DOOR_ENCODER_LIMIT,"dd",editEncLimit.getText().toString());
                        txtEncLimit.setText(editEncLimit.getText().toString());
                        editEncLimit.setText("");
                        editEncLimit.setEnabled(false);
                        editEncLimit.setEnabled(true);
                    }
                    else{
                        Log.d(LOGTAG,"No encoder limit provided, not sending anything to doggy door.");
                        showMessage(mContext,"No encoder limit provided. Please input the encoder limit you wish to set for the DoggyDoor.");
                    }
                }
                else{ showMessage(mContext,"Not connected to Doggy Door. Try connecting again."); }
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
                        sendSetCommand(Helper.DD_CMD_DOOR_SPEED,"dd",editMotorSpd.getText().toString());
                        txtMotorSpd.setText(editMotorSpd.getText().toString());
                        editMotorSpd.setText("");
                        editMotorSpd.setEnabled(false);
                        editMotorSpd.setEnabled(true);
                    }
                    else{
                        Log.d(LOGTAG,"No motor speed provided, not sending anything to doggy door.");
                        showMessage(mContext,"No motor speed provided. Please input the motor speed you wish the DoggyDoor to open/close.");
                    }
                }
                else{ showMessage(mContext,"Not connected to Doggy Door. Try connecting again."); }
            }
        });
    }

    public static void requestDoorStatus(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Getting Doggy Door Status...");
            sendGetCommand(Helper.DD_CMD_DOOR_STATUS,"dd");
        }
        else{
            showMessage(mContext,"Not connected to Doggy Door. Try connecting again.");
        }
    }
    public static void queryDoorEncoder(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Getting Doggy Door Encoder Limit...");
            sendGetCommand(Helper.DD_CMD_DOOR_ENCODER_LIMIT,"dd");
        }else{
            showMessage(mContext,"Not connected to Doggy Door. Try connecting again.");
        }
    }
    public static void queryMotorSpeed(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Getting Doggy Door Motor Speed...");
            sendGetCommand(Helper.DD_CMD_DOOR_SPEED,"dd");
        }else{
            showMessage(mContext,"Not connected to Doggy Door. Try connecting again.");
        }
    }
    public static void queryDoorBattery(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Getting battery information for Doggy Door...");
            sendGetCommand(Helper.DD_CMD_BATTERY_INFO,"dd");
        }
        else{
            showMessage(mContext,"Not connected to Doggy Door. Try connecting again.");
        }
    }
    public static void forceDoorOpen(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Attempting to open DoggyDoor...");
            sendSetCommand(Helper.DD_CMD_OPEN_DOOR,"dd");
        }
        else{ showMessage(mContext,"Not connected to Doggy Door. Try connecting again."); }
    }
    public static void forceDoorClose(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Attempting to close DoggyDoor...");
            sendSetCommand(Helper.DD_CMD_CLOSE_DOOR,"dd");
        }
        else{ showMessage(mContext,"Not connected to Doggy Door. Try connecting again."); }
    }
    public static void forceDoorNormal(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Attempting to set DoggyDoor back to normal operation...");
            sendSetCommand(Helper.DD_CMD_NORMAL_DOOR_OPERATION,"dd");
        }
        else{ showMessage(mContext,"Not connected to Doggy Door. Try connecting again."); }
    }
    public static void lockDoor(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Attempting to lock DoggyDoor...");
            sendSetCommand(Helper.DD_CMD_LOCK_DOOR,"dd");
        }
        else{ showMessage(mContext,"Not connected to Doggy Door. Try connecting again."); }
    }
    public static void unlockDoor(){
        if(mState == UART_PROFILE_CONNECTED){
            Log.d(LOGTAG,"Attempting to unlock DoggyDoor...");
            sendSetCommand(Helper.DD_CMD_UNLOCK_DOOR,"dd");
        }
        else{ showMessage(mContext,"Not connected to Doggy Door. Try connecting again."); }
    }

    public static void clearTagGui(boolean withDisconnect){
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
    public static void clearDoorGui(boolean withDisconnect){
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
    public static void setLedDrawable(Boolean isActive,TextView tv){
        if(isActive){ tv.setBackground(picLedActive); }
        else{ tv.setBackground(picLedInactive); }
    }
    public static void updateTagListAlias(){
        if(mSelectedItem != -1){
            deviceAdapter.setTempAlias(nAliasLbl);
            txtEmptyList.setVisibility(View.GONE);
            deviceAdapter.notifyDataSetChanged();
        }
    }

    public void populateList() {
        /* Initialize device list container */
        Log.d(LOGTAG, "populateList");
        if(!isConnectedToDd){
            deviceLst = new ArrayList<BluetoothDevice>();
            deviceAdapter = new DeviceAdapter(getContext(), deviceLst);
            devRssiValues = new HashMap<String, Integer>();
        }
        else{
            if(deviceLst == null){deviceLst = new ArrayList<BluetoothDevice>();}
            if(deviceAdapter == null){deviceAdapter = new DeviceAdapter(getContext(), deviceLst);}
            if(devRssiValues == null){devRssiValues = new HashMap<String, Integer>();}
        }

        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);
    }
    public static void addDeviceList(BluetoothDevice device) {
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
    public static void addNewDevice(BluetoothDevice device) {
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
            deviceAdapter.setTempAlias(null);
            txtEmptyList.setVisibility(View.GONE);
            deviceAdapter.notifyDataSetChanged();
        }
    }
    public static void addNewDevice(BluetoothDevice device, String alias) {
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
            deviceAdapter.setTempAlias(alias);
            txtEmptyList.setVisibility(View.GONE);
            deviceAdapter.notifyDataSetChanged();
        }
    }
    public static void sendAddDevCommand(BluetoothDevice device){
        deviceLst.add(tmpDevice);
        String address = device.getAddress();
        String name = device.getName();
        txtEmptyList.setVisibility(View.GONE);
        deviceAdapter.notifyDataSetChanged();

        String nusMsg = "add_dev " + name + " " + address;
        byte[] value;
        try { //send data to service
            value = nusMsg.getBytes("UTF-8");
            MainActivity.mService.writeRXCharacteristic(value);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        }
        catch (UnsupportedEncodingException e) { e.printStackTrace(); }
    }
    public void delDeviceList(BluetoothDevice device) {
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
    public void sendDelDevCommand(BluetoothDevice device){
        String address = device.getAddress();
        String name = device.getName();
        String nusMsg = "del_dev " + name + " " + address;
        byte[] buf;
        try { //send data to service
            buf = nusMsg.getBytes("UTF-8");
            MainActivity.mService.writeRXCharacteristic(buf);
            //Update the log with time stamp
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public static void sendGetCommand(String paramId, String device){
        String nusMsg = "get " + paramId + " " + device + "\0";
        byte[] buf;
        try {   //send data to service
            buf = nusMsg.getBytes("UTF-8");
            MainActivity.mService.writeRXCharacteristic(buf);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    public static void sendSetCommand(String paramId, String device){
        String nusMsg = "set " + paramId + " " + device + "\0";
        byte[] buf;
        try {   //send data to service
            buf = nusMsg.getBytes("UTF-8");
            MainActivity.mService.writeRXCharacteristic(buf);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    public static void sendSetCommand(String paramId, String device, String value){
        String nusMsg = "set " + paramId + " " + device + " " + value + "\0";
        byte[] buf;
        try {   //send data to service
            buf = nusMsg.getBytes("UTF-8");
            MainActivity.mService.writeRXCharacteristic(buf);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    public void sendSetCommand(String paramId, String device, Integer value){
        String nusMsg = "set " + paramId + " " + device + " " + value.toString() + "\0";
        byte[] buf;
        try {   //send data to service
            buf = nusMsg.getBytes("UTF-8");
            MainActivity.mService.writeRXCharacteristic(buf);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
    public void sendSetCommand(String paramId, String device, Float value){
        String nusMsg = "set " + paramId + " " + device + " " + value.toString() + "\0";
        byte[] buf;
        try {   //send data to service
            buf = nusMsg.getBytes("UTF-8");
            MainActivity.mService.writeRXCharacteristic(buf);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(LOGTAG,"["+currentDateTimeString+"] TX: "+ nusMsg);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void clearStoredDevices(){
        Log.d(LOGTAG, "Clearing stored DoggyDoor devices....");
        deviceList.clear();
        deviceLst.clear();
        txtEmptyList.setVisibility(View.GONE);
        deviceAdapter.notifyDataSetChanged();
    }
    public static void requestStoredDevices(){
//        MainActivity.showMessage("Requesting information on all pet tags stored by the DoggyDoor...");
        String message = "query devices\0";
        byte[] value;
        try {
            value = message.getBytes("UTF-8");
            MainActivity.mService.writeRXCharacteristic(value);
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
        }
        catch (UnsupportedEncodingException e) { e.printStackTrace(); }
    }

    public static Integer parseDdResponse(final byte[] resp){
        String text;
        Integer result;
        // Error handling
        try { text = new String(resp, "UTF-8"); }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            result = -1;
            return result;
        }
        if(debug_nus_io) Log.d(LOGTAG, "Received Response containing [" + resp.length + " bytes] from Thingy --- \'" + text + "\'.\r\n");

        // Add response to Thingy response history, and split response using delimiters
        deviceStrList.add(text);
        text = text.replaceAll("\r", "").replaceAll("\n", "").replaceAll("\0", "");
        String[] tokens = text.split(":|\\s+");
        if(debug_nus_io) Log.d(LOGTAG, "Parsed response to [" + text.getBytes().length + " bytes] = \'" + Arrays.toString(tokens) + "\'.\r\n");

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
            if(debug_nus_io) Log.d(LOGTAG, "Thingy responded with a \'set\' command [" + text + "].\r\n");
            result = 2;
        }else{
            Log.d(LOGTAG, "Thingy Responded with the following message:\r\n\t\'" + text + "\'.\r\n");
            result = 3;
        }
        return result;
    }
    public static String constructTagAddress(String[] addressTokens_){
        ntagAddr = new ArrayList<Integer>(6);
        StringJoiner strAddr = new StringJoiner(":");
        for(String tk: addressTokens_){
            strAddr.add(tk.toUpperCase());
            ntagAddr.add(Integer.parseInt( tk, 16 ));
        }
        return strAddr.toString();
    }
    public static String decodeBatChargeStatus(Integer charge_status_code){
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

    public static Integer extractDdParameters(String header, String[] data){
        int nData = data.length;
        Integer result = null;
        if(debug_nus_io) Log.d(LOGTAG, "extractDdParameters --- Thingy response message contains " + nData + " valid parameters to extract.\r\n");

        switch(header) {
            case Helper.DD_CMD_QUERY_DEVICES:{
                if(nData >= 7){
                    ntagId = data[0];
                    String[] addrTokens = Arrays.copyOfRange(data, 1, 7);
                    String tmpAddr = constructTagAddress(addrTokens);
                    if(nData > 7) ntagAlias = data[7];
                    BluetoothDevice bleDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(tmpAddr);

                    if(nData == 7) addNewDevice(bleDevice);
                    else addNewDevice(bleDevice, ntagAlias);
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
                if(debug_nus_io) Log.d(LOGTAG, "extractDdParameters --- Extracted Battery Life: " + nBatVolt + " mV (" + nBatPercent + "%).\r\n");
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
                if(debug_nus_io) Log.d(LOGTAG, "extractDdParameters --- Extracted Battery Information: " + nBatVolt + " mV (" + nBatPercent + "%) -- Status = " + nChargeStatus + ".\r\n");
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
                if(debug_nus_io) Log.d(LOGTAG, "extractDdParameters ---- Extracted Encoder Limit = " + nEncLimit + "...\r\n");
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
                if(debug_nus_io) Log.d(LOGTAG, "extractDdParameters ---- Extracted Encoder Limit = " + nMotorSpd + "...\r\n");
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

    public static void updateGuiTagInfo(String id_, String address_, String alias_, Integer rssiThresh_,
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

    public static void updateGuiDoggyDoor(Integer battery_voltage, Integer battery_percent, String charge_status,
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

    /** *******************************************************************************************
    *
    * ******************************************************************************************* */
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device;
            String tmpAddr;
            String tmpName;
            if (updateview != null)
                updateview.setBackgroundColor(Color.TRANSPARENT);
            updateview = view;

            if(mSelectedItem == position){
                view.setBackgroundColor(Color.TRANSPARENT);
                mSelectedItem = -1;
                tmpAddr = "";
                tmpName = "";
                clearTagGui(false);
            }else{
                view.setBackgroundColor(Color.CYAN);
                mSelectedItem = position;
                device = deviceList.get(position);
                /** static locally stored devices */
                tmpName = device.getName();
                tmpAddr = device.getAddress();
                sendGetCommand(Helper.DD_CMD_TAG_INFO,tmpName);
            }

//            if(!tmpName.equals("")){
//                sendGetCommand(Helper.DD_CMD_TAG_INFO,tmpName);
//            }else{
//                clearTagGui(false);
//            }
            Log.d(LOGTAG, "mDeviceClickListener --  device [" + mSelectedItem + "] selected - " + tmpName + " (" + tmpAddr + ")");
        }
    };

    /** *******************************************************************************************
    *
    * ******************************************************************************************* */
    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        LayoutInflater inflater;
        String tmpAliasLbl = null;

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        public void setTempAlias(String text){
            tmpAliasLbl = text;
        }

        public String getTempAlias(){
            return tmpAliasLbl;
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
            final TextView tvalias = (TextView) vg.findViewById(R.id.alias);

            tvrssi.setVisibility(View.VISIBLE);
            if (ntagRssi != null) { tvrssi.setText("Rssi = " + ntagRssi); }
            tvalias.setVisibility(View.VISIBLE);

            tvname.setText(device.getName());
            tvadd.setText(device.getAddress());
            if(tmpAliasLbl == null) tvalias.setText("Unknown");
            else tvalias.setText(tmpAliasLbl);

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
                tvalias.setVisibility(View.VISIBLE);
                tvalias.setTextColor(Color.BLACK);
            }
            return vg;
        }
    }

    /** *******************************************************************************************
    *
    * ******************************************************************************************* */
    public static void showMessage(Context context_,String msg) { Toast.makeText(context_, msg, Toast.LENGTH_SHORT).show(); }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Helper.REQUEST_SELECT_DEVICE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    MainActivity.mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                    String tmpName = BluetoothAdapter.getDefaultAdapter().getName();
                    Log.i(LOGTAG, "... onActivityResult CONNECTING ----- device.address == " + tmpName + " (" + MainActivity.mDevice + ")  mserviceValue" + MainActivity.mService);
                    ((TextView)  getActivity().findViewById(R.id.deviceName)).setText(MainActivity.mDevice.getName()+ " - connecting");
                    MainActivity.mService.connect(deviceAddress);
                    mLastBleCentralDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                }
                else if(resultCode == Helper.RESULT_ADD_USER && data != null){
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    tmpDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                    addDeviceList(tmpDevice);
                }break;
            case Helper.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
//                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(LOGTAG, "BT not enabled");
//                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
            default:
                Log.e(LOGTAG, "wrong request code");
                break;
        }
    }

    /** ==========================================================================================
    *
    * =========================================================================================== */
}