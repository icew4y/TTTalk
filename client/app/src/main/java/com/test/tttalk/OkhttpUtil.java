package com.test.tttalk;

import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class OkhttpUtil {


    public static Response post(String url, Map<String, Object> headers, byte[] data, String dataType) {

        try {


            RequestBody body = RequestBody.create(MediaType.parse(dataType), data);
            OkHttpClient client = new OkHttpClient.Builder()
                    //.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888)))
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(body);

            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = (String)entry.getValue();
                builder.addHeader(key, value);
            }

            Request request = builder.build();

            Response response = client.newCall(request).execute();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Response get(String url, Map<String, Object> headers) {
        try {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request.Builder builder = new Request.Builder()

                    .url(url)
                    .get();

            for (Map.Entry<String, Object> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = (String)entry.getValue();
                builder.addHeader(key, value);
            }
            Request request = builder.build();

            Response response = client.newCall(request).execute();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Map<String, List<String>> HeadersToMultimap(Headers headers) {
        Map<String, List<String>> result = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        int i = 0;

        for(int size = headers.size(); i < size; ++i) {
            String name = headers.name(i);
            List<String> values = (List)result.get(name);
            if (values == null) {
                values = new ArrayList(2);
                result.put(name, values);
            }

            ((List)values).add(headers.value(i));
        }

        return result;
    }
}