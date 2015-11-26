/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */
package org.wso2.carbon.iot.android.sense;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.wso2.carbon.iot.android.sense.constants.AvailableSensors;
import org.wso2.carbon.iot.android.sense.constants.SenseConstants;
import org.wso2.carbon.iot.android.sense.events.input.Sensor.RealTimeSensorReader;
import org.wso2.carbon.iot.android.sense.register.SenseDeEnroll;
import org.wso2.carbon.iot.android.sense.scheduler.DataUploaderReceiver;
import org.wso2.carbon.iot.android.sense.scheduler.RealTimeSensorChangeReceiver;
import org.wso2.carbon.iot.android.sense.service.SenseScheduleReceiver;
import org.wso2.carbon.iot.android.sense.util.SensorViewAdaptor;
import org.wso2.carbon.iot.android.sense.util.TempStore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import agent.sense.android.iot.carbon.wso2.org.wso2_senseagent.R;


/**
 * Activity for selecting sensors available in the device
 */

public class ActivitySelectSensor extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SelectSensorDialog.SensorListListener {

    SharedPreferences sp;
    SelectSensorDialog dialog = new SelectSensorDialog();
    Set<String> senseorList = new HashSet<>();
    FloatingActionButton fab;
    FloatingActionButton add;
    ListView listView;
    SensorManager manager;
    ArrayList<Sensor> sensors = new ArrayList<>();
    boolean check;

    RealTimeSensorReader sensorReader = null;
    RealTimeSensorChangeReceiver realTimeSensorChangeReceiver = new RealTimeSensorChangeReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_select_sensor);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        listView = (ListView) findViewById(R.id.senseListContainer);

        registerReceiver(realTimeSensorChangeReceiver, new IntentFilter("sensor"));

        //Publish data
        fab = (FloatingActionButton) findViewById(R.id.publish);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Publishing data started", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                check = true;
                DataUploaderReceiver dataUploaderReceiver = new DataUploaderReceiver();
                dataUploaderReceiver.clearAbortBroadcast();
                dataUploaderReceiver.onReceive(getApplicationContext(), null);
            }
        });

        add = (FloatingActionButton) findViewById(R.id.addSensors);
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show(getFragmentManager(), "Sensor List");
            }
        });

        sp = getSharedPreferences(SenseConstants.SELECTED_SENSORS, 0);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_select_sensor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            for (Sensor s : sensors) {
                manager.unregisterListener(sensorReader, s);
            }
            /**
             * unregister the sensors
             * */
            unregisterReceiver(realTimeSensorChangeReceiver);

            Intent intent = new Intent(this, SenseDeEnroll.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.select) {
            dialog.show(getFragmentManager(), "Sensor List");
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onDialogPositiveClick(SelectSensorDialog dialog) {

        System.out.println(dialog.getSet().toString());
        senseorList = dialog.getSet();
        update();

        TempStore.realTimeSensors.clear();

        SenseScheduleReceiver senseScheduleReceiver = new SenseScheduleReceiver();
        senseScheduleReceiver.clearAbortBroadcast();
        senseScheduleReceiver.onReceive(this, null);

        /**
         * Get the selected sensors
         * Register them
         * */
        SensorViewAdaptor adaptor1 = new SensorViewAdaptor(getApplicationContext(), TempStore.realTimeSensors);

        sensorReader = new RealTimeSensorReader(this, adaptor1);
        getSensors();

        for (Sensor s : sensors) {
            manager.registerListener(sensorReader, s, SensorManager.SENSOR_DELAY_NORMAL);
        }


        realTimeSensorChangeReceiver.updateOnChange(adaptor1);
        listView.setAdapter(adaptor1);

    }

    public boolean update() {
        try {
            Log.d("Update", "Set the values to SP");
            Log.d("List", senseorList.toString());

            SharedPreferences.Editor editor = sp.edit();
            editor.putStringSet(SenseConstants.SELECTED_SENSORS_BY_USER, senseorList);
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    public void getSensors() {
        for (String sensor : senseorList.toArray(new String[senseorList.size()])) {
            sensors.add(manager.getDefaultSensor(AvailableSensors.getType(sensor.toLowerCase())));
        }
    }
}
