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

package com.cw.showpdfutil.pdf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.cw.showpdfutil.pdf.view.DownloadActivity;
import com.cw.showpdfutil.pdf.view.PdfViewerActivity;
import com.cw.showpdfutil.utils.CacheUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * @author Cw
 * @date 16/12/3
 */
public class ShowPdfUtil {

    private static final String TAG = ShowPdfUtil.class.getSimpleName();
    private static Handler mUIHandler = new Handler(Looper.getMainLooper());

    public static void onGetPdfUrl(Activity context, String pdfUrl,
                                   String fileName, boolean isForceDownload) {
        if (TextUtils.isEmpty(pdfUrl)) {
            Log.w(TAG, "pdfUrl is empty!");
            return;
        }
        fileName = getFileName(pdfUrl, fileName);
        Log.d(TAG, "pdfUrl:" + pdfUrl + "; fileName: " + fileName);
        if (isForceDownload || !CacheUtil.isCacheFileExists(context, fileName)) {
            startDownloadPdfActivity(context, pdfUrl, fileName);
        } else {
            Log.i(TAG, fileName + " exist, open it.");
            openPdf(context, new File(CacheUtil.getAppCachePath(context) + fileName));
        }
    }

    public static void openPdf(final Context context, final File file) {
        if (null == context) {
            return;
        }
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (null == file || !file.exists()) {
                    Log.d(TAG, "openPdf file not exist");
                    return;
                }
                Uri uri = Uri.fromFile(file);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Constant.KEY_PDF_NAME, getFileTitle(file.getName()));
                intent.setClass(context, PdfViewerActivity.class);
                context.startActivity(intent);
            }
        });
    }

    public static File savePdf(Context context, InputStream body, String fileName) {
        String fullPathName = CacheUtil.getAppCachePath(context) + fileName;
        File f = new File(fullPathName);
        try {
            if (f.exists()) {
                f.delete();
            }
            byte[] data = new byte[1024];
            int len;
            FileOutputStream fos;
            fos = new FileOutputStream(f);
            while ((len = body.read(data)) != -1) {
                fos.write(data, 0, len);
            }
            body.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return f;
    }

    private static void startDownloadPdfActivity(Context context, String pdfUrl, String fileName) {
        Intent intent = new Intent(context, DownloadActivity.class);
        intent.putExtra(Constant.KEY_PDF_URL, pdfUrl);
        intent.putExtra(Constant.KEY_PDF_NAME, fileName);
        context.startActivity(intent);
    }

    private static String getFileTitle(String fileName) {
        if (!TextUtils.isEmpty(fileName) && fileName.contains(".")) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        }
        return fileName;
    }

    private static String getFileName(String pdfUrl, String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            fileName = getFileNameFormUrl(pdfUrl);
        } else {
            if (!fileName.endsWith(".pdf")) {
                fileName = fileName + ".pdf";
            }
        }
        return fileName;
    }

    private static String getFileNameFormUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return "";
        }
        if (url.endsWith(".pdf")) {
            return url.substring(url.lastIndexOf("/") + 1);
        } else {
            return url.substring(url.lastIndexOf("=") + 1) + ".pdf";
        }
    }
}