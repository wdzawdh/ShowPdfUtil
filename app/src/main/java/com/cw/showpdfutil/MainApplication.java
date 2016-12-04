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

package com.cw.showpdfutil;

import android.app.Application;

import com.cw.showpdfutil.network.RequestManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * @author Cw
 * @date 16/12/4
 */
public class MainApplication extends Application {

    public static RequestManager sRequestManager;

    @Override
    public void onCreate() {
        super.onCreate();
        OkHttpClient client = new OkHttpClient()
                .newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
        sRequestManager = RequestManager.getInstance(client);
    }
}