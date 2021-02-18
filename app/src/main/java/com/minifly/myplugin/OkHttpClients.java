package com.minifly.myplugin;

import com.leoao.netmonitor.NetWorkInterceptor;

import okhttp3.OkHttpClient;

/**
 * create by hexiaofei on 2020/12/29 17:35
 * path: netmonitor com.minifly.myplugin
 * <p>
 * description:
 */
public class OkHttpClients {
    public OkHttpClient okHttpClient;


    static class OkhttpClientInstanceClass {
        public static OkHttpClients instance = new OkHttpClients();
    }

    private OkHttpClients() {
        okHttpClient = new OkHttpClient.Builder().addInterceptor(new NetWorkInterceptor()).build();
    }

    public static OkHttpClients getInstance() {
        return OkhttpClientInstanceClass.instance;
    }
}
