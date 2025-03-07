package com.example.myappndefined.plugins;

import android.Manifest;


import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.getcapacitor.*;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
        name = "MyLocation",
        permissions = {
                @Permission(alias = "location", strings = {Manifest.permission.ACCESS_FINE_LOCATION})
        }
)
public class MyLocation extends Plugin {
    private AMapLocationClient mLocationClient = null;

    @PluginMethod
    public void startLocationService(PluginCall call) {


        // 检查权限
        if (getPermissionState("location") != PermissionState.GRANTED) {
            requestPermissionForAlias("location", call, "locationPermissionCallback");
            return;
        }

        // 初始化定位
        initLocation(call);
    }

    @PermissionCallback
    private void locationPermissionCallback(PluginCall call) {
        if (getPermissionState("location") == PermissionState.GRANTED) {
            initLocation(call);
        } else {
            call.reject("Location permission denied");
        }
    }

    private void initLocation(PluginCall call) {
        try {
            // 初始化定位客户端
            mLocationClient = new AMapLocationClient(getContext());
            AMapLocationClientOption mLocationOption = new AMapLocationClientOption();

            // 设置定位模式为高精度
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);

            // 设置定位回调监听
            mLocationClient.setLocationListener(new AMapLocationListener() {
                @Override
                public void onLocationChanged(AMapLocation amapLocation) {
                    if (amapLocation != null) {
                        if (amapLocation.getErrorCode() == 0) {
                            // 定位成功，返回结果


                            JSObject ret = new JSObject();
                            ret.put("latitude", amapLocation.getLatitude());
                            ret.put("longitude", amapLocation.getLongitude());
                            ret.put("address", amapLocation.getAddress());
                            System.out.println(ret);
                            call.resolve(ret);
                        } else {
                            // 定位失败
                            call.reject("Location error: " + amapLocation.getErrorInfo());
                        }
                        mLocationClient.stopLocation(); // 停止定位
                    }
                }
            });

            // 设置定位参数并启动定位
            mLocationClient.setLocationOption(mLocationOption);
            mLocationClient.startLocation();

        } catch (Exception e) {
            call.reject("Failed to initialize location: " + e.getMessage());
        }
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
    }




}
