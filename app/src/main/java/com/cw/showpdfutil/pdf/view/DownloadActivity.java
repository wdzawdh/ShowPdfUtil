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

package com.cw.showpdfutil.pdf.view;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cw.showpdfutil.MainApplication;
import com.cw.showpdfutil.R;
import com.cw.showpdfutil.network.ProgressListener;
import com.cw.showpdfutil.pdf.Constant;
import com.cw.showpdfutil.pdf.ShowPdfUtil;
import com.cw.showpdfutil.utils.CacheUtil;
import com.cw.showpdfutil.utils.FileSizeUtil;
import com.cw.showpdfutil.utils.NetWorkUtils;
import com.cw.showpdfutil.utils.ToastUtils;

import java.io.File;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Cw
 * @date 16/12/3
 */
public class DownloadActivity extends AppCompatActivity {

    private static final String TAG = DownloadActivity.class.getSimpleName();

    @BindView(R.id.tv_download_status)
    TextView tv_download_status;
    @BindView(R.id.tv_download_file_name)
    TextView tv_download_file_name;
    @BindView(R.id.tv_file_total_size)
    TextView tv_file_total_size;
    @BindView(R.id.tv_downloading_size)
    TextView tv_downloading_size;
    @BindView(R.id.pb_downloading)
    ProgressBar pb_downloading;
    @BindView(R.id.tv_download_progress)
    TextView tv_download_progress;
    @BindView(R.id.btn_download_cancel)
    Button btn_download_cancel;
    @BindView(R.id.btn_download_retry)
    Button btn_download_retry;
    @BindView(R.id.ll_btn_download_fail)
    LinearLayout ll_btn_download_fail;

    private String mFileName;
    private String mDownloadUrl;
    public okhttp3.Call mCall;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                boolean isConnected = NetWorkUtils.isNetworkConnected(DownloadActivity.this);
                if (isConnected) {
                    setDownloadRetryBtnEnable(true);
                } else {
                    setDownloadRetryBtnEnable(false);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        ButterKnife.bind(this);
        showDownloadFailView(false);
        registReceiver();
        initView();
        initData();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        cancelDownload();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mCall = null;
    }

    @OnClick(R.id.btn_download_cancel)
    void onClickCancelDownload() {
        Log.d(TAG, "click cancel");
        cancelDownload();
    }

    @OnClick(R.id.btn_download_retry)
    void onClickRetry() {
        downloadPdf(mDownloadUrl, mFileName);
        showDownloadFailView(false);
    }

    @OnClick(R.id.btn_download_quit)
    void onClickQuit() {
        finish();
    }

    private void showDownloadFailView(boolean isShow) {
        if (isShow) {
            tv_download_status.setText(this.getString(R.string.download_fail));
            ll_btn_download_fail.setVisibility(View.VISIBLE);
            btn_download_cancel.setVisibility(View.GONE);
        } else {
            tv_download_status.setText(this.getString(R.string.download_ing));
            ll_btn_download_fail.setVisibility(View.GONE);
            btn_download_cancel.setVisibility(View.VISIBLE);
        }
    }

    private void registReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mReceiver, filter);
    }

    private void initView() {
        showDownloadingSize(0);
        tv_file_total_size.setText(this.getString(R.string.download_total_size, "--"));
    }

    protected void initData() {
        Intent intent = getIntent();
        mDownloadUrl = intent.getStringExtra(Constant.KEY_PDF_URL);
        mFileName = intent.getStringExtra(Constant.KEY_PDF_NAME);
        Log.d(TAG, "ACTION DOWNLOAD PDF url: " + mDownloadUrl + "; name: " + mFileName);
        downloadPdf(mDownloadUrl, mFileName);
    }

    private void showDownloadProgress(boolean isShow, int progress) {
        if (isShow) {
            pb_downloading.setProgress(progress);
            pb_downloading.setMax(100);
            tv_download_progress.setText(progress + "%");
        } else {
            pb_downloading.setIndeterminate(true);
            tv_download_progress.setText("");
        }
    }

    private void showTotalSize(long bytesTotal) {
        String sizeText = "--";
        if (bytesTotal > 1) {
            sizeText = FileSizeUtil.bytes2kb(bytesTotal);
        }
        tv_file_total_size.setText(this.getString(R.string.download_total_size, sizeText));
    }

    private void showDownloadingSize(long bytesWritten) {
        String downloadSize = FileSizeUtil.bytes2kb(bytesWritten);
        tv_downloading_size.setText(this.getString(R.string.download_ing_size, downloadSize));
    }

    private void setDownloadRetryBtnEnable(boolean isEnable) {
        if (btn_download_retry == null) {
            return;
        }
        if (isEnable) {
            if (btn_download_retry.getVisibility() == View.VISIBLE && !btn_download_retry.isEnabled()) {
                btn_download_retry.setEnabled(true);
                btn_download_retry.setClickable(true);
            }
        } else {
            btn_download_retry.setEnabled(false);
            btn_download_retry.setClickable(false);
        }
    }

    private void downloadPdf(String pdfUrl, String fileName) {
        if (TextUtils.isEmpty(pdfUrl)) {
            return;
        }
        if (fileName == null || "".equals(fileName)) {
            return;
        }
        mFileName = fileName;
        tv_download_file_name.setText(mFileName);

        if (!NetWorkUtils.isNetworkConnected(DownloadActivity.this)) {
            Log.d(TAG, "no network, can't download.");
            if (TextUtils.isEmpty(tv_file_total_size.getText())) {
                tv_file_total_size.setText(this.getString(R.string.download_total_size, "--"));
            }
            if (TextUtils.isEmpty(tv_downloading_size.getText())) {
                showDownloadingSize(0);
            }

            showDownloadFailView(true);
            setDownloadRetryBtnEnable(false);
            ToastUtils.show(DownloadActivity.this, "网络不佳");
        } else {
            startDownload(pdfUrl, fileName);
        }

    }

    private void startDownload(String pdfUrl, String fileName) {
        mCall = downloadPdf(this, pdfUrl, fileName, new ProgressListener() {
            @Override
            public void onProgress(long currentBytes, long contentLength, boolean done) {
                int progress = 0;
                if (contentLength > 1) {
                    float pb = (float) currentBytes / contentLength;
                    progress = (int) (pb * 100);
                }
                onDownloadProgress(progress, currentBytes, contentLength);
            }
        });
    }

    private void cancelDownload() {
        if (null != mCall) {
            mCall.cancel();
        }
        boolean isDelete = CacheUtil.deleteCacheFile(this, mFileName);
        Log.d(TAG, "cancel download delete file:" + mFileName + " isDelete:" + isDelete);
        finish();
    }

    public void onDownloadProgress(final int progress, final long bytesWritten, final long totalSize) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (totalSize > 1) {
                    showDownloadProgress(true, progress);
                } else {
                    showDownloadProgress(false, 0);
                }
                showTotalSize(totalSize);
                showDownloadingSize(bytesWritten);
            }
        });
    }

    public okhttp3.Call downloadPdf(final Activity act, String url, final String fileName,
                                    final ProgressListener listener) {
        return MainApplication.sRequestManager.download(url, new ProgressListener() {
            @Override
            public void onSuccess(InputStream body) {
                onDownloadPdfSuccess(act, fileName, body);
            }

            @Override
            public void onProgress(long currentBytes, long contentLength, boolean done) {
                if (null != listener) {
                    listener.onProgress(currentBytes, contentLength, done);
                }
            }

            @Override
            public void onFailure(Object body) {
                Log.e(TAG, "downloadPdf--onFailure.");
                showDownloadFailView(true);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "downloadPdf--onError:" + e.getMessage());
                showDownloadFailView(true);
            }
        });

    }

    private void onDownloadPdfSuccess(final Activity act, final String fileName, final InputStream body) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                final File f = ShowPdfUtil.savePdf(act, body, fileName);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv_download_status.setText(DownloadActivity.this.getString(R.string.download_finish_open));
                        ShowPdfUtil.openPdf(DownloadActivity.this, f);
                        finish();
                    }
                });
            }
        }.start();
    }

}