package com.leoao.netmonitor;


import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.zip.GZIPInputStream;

import okhttp3.HttpUrl;
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

    public NetWorkInterceptor() {
        Log.e("hxf"," NetWorkInterceptor() 初始化 ");
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request oldRequest = chain.request();
        Response oldResponse = chain.proceed(oldRequest);
        String contentType = oldResponse.header("Content-Type");
        HttpUrl url = oldRequest.url();
        String host = url.host();
        //如果是mock平台的接口则不进行拦截
//        if (host.equalsIgnoreCase(NetworkManager.MOCK_HOST)) {
//            return oldResponse;
//        }

        //path  /test/upload/img
//        String path = URLDecoder.decode(url.encodedPath(), "utf-8");
//        String queries = url.query();
//        String jsonQuery = transformQuery(queries);
//        String jsonRequestBody = transformRequestBody(oldRequest.body());
//        //LogHelper.i(TAG, "realJsonQuery===>" + jsonQuery);
//        //LogHelper.i(TAG, "realJsonRequestBody===>" + jsonRequestBody);
//        String interceptMatchedId = DokitDbManager.getInstance().isMockMatched(path, jsonQuery, jsonRequestBody, DokitDbManager.MOCK_API_INTERCEPT, DokitDbManager.FROM_SDK_OTHER);
//        String templateMatchedId = DokitDbManager.getInstance().isMockMatched(path, jsonQuery, jsonRequestBody, DokitDbManager.MOCK_API_TEMPLATE, DokitDbManager.FROM_SDK_OTHER);
        try {
            //网络的健康体检功能 统计流量大小
//            if (DoKitConstant.APP_HEALTH_RUNNING) {
                addNetWokInfoInAppHealth(oldRequest, oldResponse);
//            }

            //是否命中拦截规则
//            if (!TextUtils.isEmpty(interceptMatchedId)) {
//                return matchedInterceptRule(url, path, interceptMatchedId, templateMatchedId, oldRequest, oldResponse, chain);
//            }

            //是否命中模板规则
//            matchedTemplateRule(oldResponse, path, templateMatchedId);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return oldResponse;
    }

    /**
     * 动态添加网络拦截
     *
     * @param request
     * @param response
     */
    private void addNetWokInfoInAppHealth( Request request,  Response response) {
        try {
            long upSize = -1;
            long downSize = -1;
            if (request.body() != null) {
                upSize = request.body().contentLength();
            }
            if (response.body() != null) {
                downSize = response.body().contentLength();
            }


            if (upSize < 0 && downSize < 0) {
                return;
            }

            upSize = upSize > 0 ? upSize : 0;
            downSize = downSize > 0 ? downSize : 0;


            Log.e("hxf",request.url() + " : " + upSize + "    down " +downSize);

//            String activityName = ActivityUtils.getTopActivity().getClass().getCanonicalName();
//            AppHealthInfo.DataBean.NetworkBean networkBean = AppHealthInfoUtil.getInstance().getNetWorkInfo(activityName);
//            AppHealthInfo.DataBean.NetworkBean.NetworkValuesBean networkValuesBean = new AppHealthInfo.DataBean.NetworkBean.NetworkValuesBean();
//            networkValuesBean.setCode("" + response.code());
//
//            networkValuesBean.setUp("" + upSize);
//            networkValuesBean.setDown("" + downSize);
//            networkValuesBean.setMethod(request.method());
//            networkValuesBean.setTime("" + TimeUtils.getNowMills());
//            networkValuesBean.setUrl(request.url().toString());
//            if (networkBean == null) {
//                networkBean = new AppHealthInfo.DataBean.NetworkBean();
//                networkBean.setPage(activityName);
//                List<AppHealthInfo.DataBean.NetworkBean.NetworkValuesBean> networkValuesBeans = new ArrayList<>();
//                networkValuesBeans.add(networkValuesBean);
//                networkBean.setValues(networkValuesBeans);
//                AppHealthInfoUtil.getInstance().addNetWorkInfo(networkBean);
//            } else {
//                List<AppHealthInfo.DataBean.NetworkBean.NetworkValuesBean> networkValuesBeans = networkBean.getValues();
//                if (networkValuesBeans == null) {
//                    networkValuesBeans = new ArrayList<>();
//                    networkValuesBeans.add(networkValuesBean);
//                    networkBean.setValues(networkValuesBeans);
//                } else {
//                    networkValuesBeans.add(networkValuesBean);
//                }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
