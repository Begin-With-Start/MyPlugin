package com.leoao.netmonitor;


import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * create by hexiaofei on 2020/12/29 17:14
 * path: netmonitor com.leoao.netmonitor
 * <p>
 * description:
 */
public class NetWorkInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response;
        Log.e("hxf",request.url() + " 请求 ");
        try {
            // 发送请求，获得回包
            response = chain.proceed(request);
        } catch (IOException e) {
            // 如果发生了IO Exception，则通知Chrome网络请求失败了，显示对应的错误信息
            throw e;
        }

        // 展示回包信息
        // body信息题
        ResponseBody body = response.body();
        MediaType contentType = null;
        InputStream responseStream = null;
        if (body != null) {
            contentType = body.contentType();
            responseStream = body.byteStream();
        }
        long requestLength  =  request.body().contentLength();

        long responseBytes = parseAndSaveBody(responseStream,response.header("Content-Encoding"));

        Log.e("hxf",request.url() + " :  " + (requestLength + responseBytes));
        return response;
    }

    private static final String GZIP_ENCODING = "gzip";
    private long parseAndSaveBody(InputStream inputStream, String contentEncoding) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        long responseByte = 0 ;
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buffer)) > -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            byteArrayOutputStream.flush();
            InputStream newStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            BufferedReader bufferedReader;
            if (GZIP_ENCODING.equals(contentEncoding)) {
                GZIPInputStream gzipInputStream = new GZIPInputStream(newStream);
                bufferedReader = new BufferedReader(new InputStreamReader(gzipInputStream));
            } else {
                bufferedReader = new BufferedReader(new InputStreamReader(newStream));
            }
            StringBuilder bodyBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                bodyBuilder.append(line + '\n');
            }
            String body = bodyBuilder.toString();
//            networkFeedModel.setBody(body);
//            networkFeedModel.setSize(body.getBytes().length);
//
//            //设置响应数据大小
//            NetworkRecord record = NetworkManager.get().getRecord(networkFeedModel.getRequestId());
//            record.setResponseLength(body.getBytes().length);
            responseByte = body.getBytes().length;
            byteArrayOutputStream.close();
        } catch (IOException e) {
            System.out.println("----parseAndSaveBody---"+ e);
        }
        return responseByte;
    }
}
