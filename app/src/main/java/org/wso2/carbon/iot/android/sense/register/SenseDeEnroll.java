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

package org.wso2.carbon.iot.android.sense.register;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.wso2.carbon.iot.android.sense.util.LocalRegister;

import agent.sense.android.iot.carbon.wso2.org.wso2_senseagent.R;

public class SenseDeEnroll extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (!LocalRegister.isExist(getApplicationContext())) {
            Intent activity = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(activity);
        }

        //No need to set to this content because of the direct de enrollment process.
//        setContentView(R.layout.activity_sense_settings);

        LocalRegister.removeUsername(getApplicationContext());
        LocalRegister.removeDeviceId(getApplicationContext());
        LocalRegister.removeServerURL(getApplicationContext());
        LocalRegister.setExist(false);
        Intent activity = new Intent(getApplicationContext(), RegisterActivity.class);
        startActivity(activity);
    }


}
