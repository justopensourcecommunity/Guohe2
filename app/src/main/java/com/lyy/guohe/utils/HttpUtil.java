package com.lyy.guohe.utils;

import com.lyy.guohe.model.Res;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Created by lyy on 2017/10/6.
 */

public class HttpUtil {

    public static void get(String address, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)       //设置连接超时
                .readTimeout(60, TimeUnit.SECONDS)          //设置读超时
                .writeTimeout(60, TimeUnit.SECONDS)          //设置写超时
                .retryOnConnectionFailure(true)             //是否自动重连
                .build();                                   //构建OkHttpClient对象

        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }

    //异步发送post请求
    public static void post(String address, RequestBody requestBody, Callback callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)       //设置连接超时
                .readTimeout(60, TimeUnit.SECONDS)          //设置读超时
                .writeTimeout(60, TimeUnit.SECONDS)          //设置写超时
                .retryOnConnectionFailure(true)             //是否自动重连
                .build();                                   //构建OkHttpClient对象
        Request request = new Request.Builder().url(address).post(requestBody).build();
        client.newCall(request).enqueue(callback);
    }

    //处理传来的信息
    public static Res handleResponse(String response) {
        try {
            JSONObject object = new JSONObject(response);
            String code = object.getString("code");
            String msg = object.getString("msg");
            String info = object.getString("info");
            return new Res(Integer.parseInt(code), msg, info);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //通过传递map对象获得post请求的body
    public static RequestBody getBody(Map map) {
        FormBody.Builder builder = new FormBody.Builder();
        Set keySet = map.keySet(); // key的set集合
        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            Object k = it.next(); // key
            Object v = map.get(k);  //value
            builder.add(k.toString(), v.toString());
        }
        return builder.build();
    }
}
