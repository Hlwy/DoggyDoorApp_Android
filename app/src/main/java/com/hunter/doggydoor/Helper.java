package com.hunter.doggydoor;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

public class Helper {
    public static final String LOGTAG = "Helper";
    public static final int REQUEST_SELECT_DEVICE = 1;
    public static final int REQUEST_ENABLE_BT = 2;

    public static final int RESULT_ADD_USER   = 2;
    public static final int RESULT_DEL_USER   = 3;

    public static final String DD_CMD_QUERY_DEVICES = "add_tag";
    public static final String DD_CMD_BATTERY_LEVEL = "battery_level";
    public static final String DD_CMD_CHARGE_STATUS = "charge_status";
    public static final String DD_CMD_BATTERY_INFO = "battery";
    public static final String DD_CMD_BLE_UPDATE_RATE = "ble_rate";
    public static final String DD_CMD_DOOR_UPDATE_RATE = "door_update_rate";
    public static final String DD_CMD_TAG_CHECK_UPDATE_RATE = "tag_check_rate";
    public static final String DD_CMD_STOP_MOTOR = "stop_motor";
    public static final String DD_CMD_OPEN_DOOR = "open_door";
    public static final String DD_CMD_CLOSE_DOOR = "close_door";
    public static final String DD_CMD_NORMAL_DOOR_OPERATION = "normal_door_operation";
    public static final String DD_CMD_TOGGLE_DOOR_LOCK = "toggle_door_lock";
    public static final String DD_CMD_LOCK_DOOR = "lock_door";
    public static final String DD_CMD_UNLOCK_DOOR = "unlock_door";
    public static final String DD_CMD_DOOR_ENCODER_LIMIT = "encoder_limit";
    public static final String DD_CMD_DOOR_SPEED = "door_speed";
    public static final String DD_CMD_DOOR_STATUS = "door_status";
    public static final String DD_CMD_LIMIT_SWITCH_STATUS = "limit_switch";
    public static final String DD_CMD_TAG_INFO = "tag_info";
    public static final String DD_CMD_TAG_ID = "tag_id";
    public static final String DD_CMD_TAG_ALIAS = "tag_alias";
    public static final String DD_CMD_TAG_ADDRESS = "tag_addr";
    public static final String DD_CMD_TAG_RSSI = "tag_rssi";
    public static final String DD_CMD_TAG_RSSI_THRESHOLD = "tag_rssi_thresh";
    public static final String DD_CMD_TAG_DEBOUNCE_THRESHOLD = "tag_debounce_thresh";
    public static final String DD_CMD_TAG_TIMEOUT = "tag_timeout";
    public static final String DD_CMD_TAG_LAST_ACTIVITY = "tag_last_act";
    public static final String DD_CMD_TAG_STATISTICS = "tag_stats";


    public static final Integer DD_BAT_CHARGE_STATUS_NOT_CHARGING               = 0;
    public static final Integer DD_BAT_CHARGE_STATUS_CHARGING                   = 1;
    public static final Integer DD_BAT_CHARGE_STATUS_CHARGING_FINISHED          = 2;
    public static final Integer DD_BAT_CHARGE_STATUS_CHARGING_DISCONNECTED      = 3;
    public static final Integer DD_BAT_CHARGE_STATUS_LOW                        = 4;
    public static final Integer DD_BAT_CHARGE_STATUS_FULL                       = 5;
    public static final Integer DD_BAT_CHARGE_STATUS_ERROR                      = 6;


    /** *******************************************************************************************
    *
    * ******************************************************************************************* */
    public static void fragmentFocus(FragmentManager fm){
        Fragment fragment = new MainFragment();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.simpleFrameLayout, fragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();
    }

    //Handler events that received from UART service
    @SuppressLint("HandlerLeak")
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) { }
    };

    /** *******************************************************************************************
    *
    * ******************************************************************************************* */
    public static void nonblockingWait(long delay_ms, Boolean verbose){
        long t1;
        long dt = 0;
        long t0 = System.currentTimeMillis();
        while(dt < delay_ms){
            t1 = System.currentTimeMillis();
            dt = t1 - t0;
            if(verbose) Log.d(LOGTAG, "Waiting " + dt/1000.0 + " seconds.\r\n");
        }
    }

}
