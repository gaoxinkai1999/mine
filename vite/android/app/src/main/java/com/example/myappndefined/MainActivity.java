package com.example.myappndefined;

import android.os.Bundle;

import com.amap.api.location.AMapLocationClient;
import com.example.myappndefined.plugins.MyDistance;
import com.example.myappndefined.plugins.MyLocation;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {


        // 注册插件
        registerPlugin(MyLocation.class);
        registerPlugin(MyDistance.class);

        super.onCreate(savedInstanceState);


        // 设置隐私政策
        AMapLocationClient.updatePrivacyShow(this, true, true);
        AMapLocationClient.updatePrivacyAgree(this, true);

    }
}
