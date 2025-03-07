package com.example.myappndefined.plugins;

import android.Manifest;
import android.annotation.SuppressLint;
import android.util.Log;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.example.myappndefined.dto.Shop;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

@CapacitorPlugin(
        name = "MyDistance"
)
public class MyDistance extends Plugin {

    @SuppressLint("NewApi")
    @PluginMethod
    public void getDistance(PluginCall call) throws JSONException {
        JSObject data = call.getData();
        JSObject currentLocation = data.getJSObject("currentLocation");
        double myLatitude = currentLocation.getDouble("latitude");
        double myLongitude = currentLocation.getDouble("longitude");
        JSONArray shopLocations = data.getJSONArray("shopLocations");

        // 定义起点
        LatLng origin = new LatLng(myLatitude, myLongitude); // 北京天安门

        List<Shop> shops = new ArrayList<>();
        for (int i = 0; i < shopLocations.length(); i++) {
            JSONObject shopJson = shopLocations.getJSONObject(i);
            Shop shop = new Shop();
            shop.id = shopJson.getInt("id");
            shop.name = shopJson.getString("name");
            shop.latitude = shopJson.getDouble("latitude");
            shop.longitude = shopJson.getDouble("longitude");
            shop.address = shopJson.getString("location");
            shops.add(shop);
        }

        // 计算起点到每个终点的直线距离
        for (Shop shop : shops) {
            shop.distance = AMapUtils.calculateLineDistance(origin, new LatLng(shop.latitude, shop.longitude));
        }

        // 按距离排序
        shops.sort(Comparator.comparing(shop -> shop.distance));

        // 将shops列表转换为JSON格式
        JSONArray shopsJsonArray = new JSONArray();
        for (Shop shop : shops) {
            JSONObject shopJson = new JSONObject();
            try {
                shopJson.put("id", shop.id);
                shopJson.put("name", shop.name);
                shopJson.put("latitude", shop.latitude);
                shopJson.put("longitude", shop.longitude);
                shopJson.put("address", shop.address);
                shopJson.put("distance", shop.distance);
                shopsJsonArray.put(shopJson);
            } catch (JSONException e) {
                Log.e("XXXX", "Error converting shop to JSON", e);
            }
        }

        // 返回结果给前端
        JSObject result = new JSObject();
        result.put("shops", shopsJsonArray);
        call.resolve(result);

    }

}
