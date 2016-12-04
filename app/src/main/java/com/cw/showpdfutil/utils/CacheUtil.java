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

package com.cw.showpdfutil.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.cw.showpdfutil.pdf.Config;

import java.io.File;

/**
 * @author Cw
 * @date 16/12/3
 */
public class CacheUtil {

    public static boolean isCacheFileExists(Context context, String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return false;
        }
        String filePath = getAppCachePath(context) + fileName;
        return new File(filePath).exists();
    }

    public static String getAppCachePath(Context context) {
        if (null == context) {
            return "";
        }
        return checkAppCacheDir(context);
    }

    public static boolean deleteCacheFile(Context context, String fileName) {
        if (null == context || TextUtils.isEmpty(fileName)) {
            return false;
        }
        String filePath = getAppCachePath(context) + fileName;
        File f = new File(filePath);
        return f.exists() && f.delete();
    }

    private static String checkAppCacheDir(Context context) {
        File dir = new File(context.getExternalCacheDir().getAbsolutePath() + Config.DIR_NAME_CACHE_ROOT);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                Log.d("CacheUtil", "App cache directory create.");
            }
        }
        return dir.getPath() + "/";
    }
}
