/**
 *   @function:$
 *   @description: $
 *   @param:$
 *   @return:$
 *   @history:
 * 1.date:$ $
 *           author:$
 *           modification:
 */

package com.cw.showpdfutil.network;

import android.os.Handler;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * @author Cw
 * @date 16/12/4
 */
public class RequestManager {

    private static RequestManager sInstance;
    private Handler mUIHandler = new Handler();
    private OkHttpClient mClient;

    public static RequestManager getInstance(OkHttpClient client) {
        if (sInstance == null) {
            sInstance = new RequestManager(client);
        }
        return sInstance;
    }

    private RequestManager(OkHttpClient client) {
        HttpLoggingInterceptor.Level l = HttpLoggingInterceptor.Level.BODY;
        mClient = client.newBuilder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(l))
                .build();
    }

    public okhttp3.Call download(String url, final ProgressListener listener) {
        OkHttpClient client = mClient.newBuilder().addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                final okhttp3.Response originalResponse = chain.proceed(chain.request());
                //包装响应体并返回
                return originalResponse.newBuilder()
                        .body(new ProgressResponseBody(originalResponse.body(), listener))
                        .build();
            }
        }).build();
        final Request request = new Request.Builder()
                .addHeader("Accept-Encoding", "identity")
                .url(url)
                .build();
        okhttp3.Call call = client.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, final IOException e) {
                if (listener == null) {
                    return;
                }
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(e);
                    }
                });
            }

            @Override
            public void onResponse(okhttp3.Call call, final okhttp3.Response response) throws IOException {
                if (listener == null) {
                    return;
                }
                mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int code = response.code();
                        if (code >= 200 && code < 300) {
                            listener.onSuccess(response.body().byteStream());
                        } else if (code == 401) {
                            listener.onFailure(response);
                        } else if (code >= 400 && code < 500) {
                            listener.onFailure(response);
                        } else if (code >= 500 && code < 600) {
                            listener.onFailure(response);
                        } else {
                            listener.onError(new RuntimeException("Unexpected response " + response));
                        }
                    }
                });
            }
        });
        return call;
    }

}