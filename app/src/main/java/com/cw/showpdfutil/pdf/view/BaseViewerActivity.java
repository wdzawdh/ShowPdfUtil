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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.cw.showpdfutil.R;
import com.cw.showpdfutil.pdf.Config;
import com.cw.showpdfutil.pdf.Constant;
import com.cw.showpdfutil.utils.CacheUtil;
import com.cw.showpdfutil.utils.ToastUtils;

import org.ebookdroid.XDroidApp;
import org.ebookdroid.core.BaseDocumentView;
import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.IDocumentView;
import org.ebookdroid.core.IDocumentViewController;
import org.ebookdroid.core.IViewerActivity;
import org.ebookdroid.core.Page;
import org.ebookdroid.core.PageAlign;
import org.ebookdroid.core.PageIndex;
import org.ebookdroid.core.ViewState;
import org.ebookdroid.core.cache.CacheManager;
import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.events.CurrentPageListener;
import org.ebookdroid.core.events.DecodingProgressListener;
import org.ebookdroid.core.log.LogContext;
import org.ebookdroid.core.models.DecodingProgressModel;
import org.ebookdroid.core.models.DocumentModel;
import org.ebookdroid.core.models.ZoomModel;
import org.ebookdroid.core.settings.AppSettings;
import org.ebookdroid.core.settings.ISettingsChangeListener;
import org.ebookdroid.core.settings.SettingsManager;
import org.ebookdroid.core.settings.books.BookSettings;
import org.ebookdroid.core.touch.IMultiTouchListener;
import org.ebookdroid.core.utils.AndroidVersion;
import org.ebookdroid.core.utils.PathFromUri;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Cw
 * @date 16/12/3
 */
public abstract class BaseViewerActivity extends AppCompatActivity implements IViewerActivity, DecodingProgressListener, CurrentPageListener, ISettingsChangeListener {

    private static final String TAG = BaseViewerActivity.class.getSimpleName();
    private static final String E_MAIL_ATTACHMENT = "[E-mail Attachment]";
    private static final int MENU_EXIT = 0;
    private static final int MENU_FULL_SCREEN = 1;

    private AtomicReference<IDocumentViewController> ctrl = new AtomicReference<IDocumentViewController>(new EmptyContoller());
    public LogContext LCTX = LogContext.ROOT.lctx(TAG);
    public DisplayMetrics DM = new DisplayMetrics();
    private PageViewZoomControls zoomControls;
    private ZoomModel zoomModel;
    private FrameLayout frameLayout;
    private IDocumentView view;
    private DocumentModel documentModel;
    private DecodingProgressModel progressModel;
    private boolean temporaryBook;
    private IMultiTouchListener mtl;
    private Toast pageNumberToast;
    private String mFileName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XDroidApp.initXDroidApp(this);
        getWindowManager().getDefaultDisplay().getMetrics(DM);
        AbstractCodecContext.setDisplayMetrics(DM);
        SettingsManager.addListener(this);
        frameLayout = new FrameLayout(this);
        view = new BaseDocumentView(this);
        initActivity();
        initView();
    }

    private void initActivity() {
        AppSettings newSettings = SettingsManager.getAppSettings();
        AppSettings.Diff diff = new AppSettings.Diff(null, newSettings);
        this.onAppSettingsChanged(null, newSettings, diff);
    }

    private void initView() {
        view.getView().setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        frameLayout.addView(view.getView());
        if (Config.isShowZoomControls) {
            frameLayout.addView(getZoomControls());
        }
        setFullScreen(Config.isEnableFunScreen);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        setContentView(frameLayout, params);
        DecodeService decodeService = createDecodeService();
        documentModel = new DocumentModel(decodeService);
        documentModel.addListener(BaseViewerActivity.this);
        progressModel = new DecodingProgressModel();
        progressModel.addListener(BaseViewerActivity.this);
        Intent intent = getIntent();
        mFileName = intent.getStringExtra(Constant.KEY_PDF_NAME);
        Uri uri = intent.getData();
        String fileName;
        if (null != intent.getScheme() && intent.getScheme().equals("content")) {
            temporaryBook = true;
            fileName = E_MAIL_ATTACHMENT;
            CacheManager.clear(fileName);
        } else {
            fileName = PathFromUri.retrieve(getContentResolver(), uri);
        }
        try {
            SettingsManager.init(fileName);
            SettingsManager.applyBookSettingsChanges(null, SettingsManager.getBookSettings(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        view.getView().post(new BookLoadTask(decodeService, fileName, ""));
    }

    private void showErrorDlg(String msg) {
        if (TextUtils.isEmpty(msg)) {
            msg = "Unexpected error occured!";
        }
        ToastUtils.show(this, msg);
        Log.e(TAG, "Erro: " + msg);
    }

    private void onOpenErrorPdfFile() {
        ToastUtils.show(BaseViewerActivity.this, getString(R.string.pdf_viewer_toast_file_corrupted));
        boolean isDelete = CacheUtil.deleteCacheFile(BaseViewerActivity.this, mFileName + ".pdf");
        Log.d(TAG, "file :" + mFileName + ".pdf is corrupted, isDelete:" + isDelete);
        BaseViewerActivity.this.finish();
    }

    private void setFullScreen(boolean isFullScreen) {
        if (isFullScreen) {
            getSupportActionBar().hide();
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        }
    }

    private PageViewZoomControls getZoomControls() {
        if (zoomControls == null) {
            zoomControls = new PageViewZoomControls(this, getZoomModel());
            zoomControls.setGravity(Gravity.RIGHT | Gravity.BOTTOM);
            zoomModel.addListener(zoomControls);
        }
        return zoomControls;
    }

    @Override
    public void decodingProgressChanged(final int currentlyDecoding) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (currentlyDecoding == 0) {
                    setProgressBarIndeterminateVisibility(false);
                } else {
                    setProgressBarIndeterminateVisibility(true);
                }
            }
        });
    }

    @Override
    public void currentPageChanged(PageIndex oldIndex, PageIndex newIndex) {
        if (Config.isShowPageToast) {
            int pageCount = documentModel.getPageCount();
            String pageText = (newIndex.viewIndex + 1) + "/" + pageCount;
            if (pageNumberToast != null) {
                pageNumberToast.setText(pageText);
            } else {
                pageNumberToast = Toast.makeText(this, pageText, Toast.LENGTH_SHORT);
            }
            pageNumberToast.setGravity(Gravity.CENTER | Gravity.BOTTOM, 0, 150);
            pageNumberToast.show();
        }
        SettingsManager.currentPageChanged(oldIndex, newIndex);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setTitle(getIntent().getData().getLastPathSegment());
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        if (documentModel != null) {
            documentModel.recycle();
            documentModel = null;
        }
        if (temporaryBook) {
            CacheManager.clear(E_MAIL_ATTACHMENT);
        }
        SettingsManager.removeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_EXIT, 0, getString(R.string.pdf_viewer_menu_exit));
        menu.add(0, MENU_FULL_SCREEN, 0, getString(R.string.pdf_viewer_menu_fullscreen))
                .setChecked(Config.isEnableFunScreen);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_EXIT:
                BaseViewerActivity.this.finish();
                return true;
            case MENU_FULL_SCREEN:
                item.setChecked(!item.isChecked());
                setFullScreen(item.isChecked());
                return true;
        }
        return false;
    }

    @Override
    public ZoomModel getZoomModel() {
        if (zoomModel == null) {
            zoomModel = new ZoomModel();
        }
        return zoomModel;
    }

    @Override
    public IMultiTouchListener getMultiTouchListener() {
        if (mtl == null) {
            mtl = new IMultiTouchListener() {

                @Override
                public void onTwoFingerPinchEnd() {
                    getZoomModel().commit();
                }

                @Override
                public void onTwoFingerPinch(float oldDistance, float newDistance) {
                    zoomModel.setZoom(zoomModel.getZoom() * newDistance / oldDistance);

                }

                @Override
                public void onTwoFingerTap() {
                    Toast.makeText(BaseViewerActivity.this, "TWO FINGER TAP", Toast.LENGTH_SHORT).show();
                }

            };
        }
        return mtl;
    }

    @Override
    public DecodeService getDecodeService() {
        return documentModel != null ? documentModel.getDecodeService() : null;
    }

    @Override
    public DecodingProgressModel getDecodingProgressModel() {
        return progressModel;
    }

    @Override
    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    @Override
    public IDocumentViewController getDocumentController() {
        return ctrl.get();
    }

    @Override
    public IDocumentViewController switchDocumentController() {
        try {
            BookSettings bs = SettingsManager.getBookSettings();

            IDocumentViewController newDc = bs.viewMode.create(this);
            IDocumentViewController oldDc = ctrl.getAndSet(newDc);

            getZoomModel().removeListener(oldDc);
            getZoomModel().addListener(newDc);

            return newDc;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public IDocumentView getView() {
        return view;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @SuppressWarnings("WrongConstant")
    @Override
    public void onAppSettingsChanged(AppSettings oldSettings, AppSettings newSettings,
                                     AppSettings.Diff diff) {
        if (diff.isRotationChanged()) {
            setRequestedOrientation(newSettings.getRotation().getOrientation());
        }

        if (diff.isFullScreenChanged() && !AndroidVersion.is3x) {
            Window window = getWindow();
            if (newSettings.getFullScreen()) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }

        if (diff.isShowTitleChanged() && diff.isFirstTime()) {
            Window window = getWindow();
            try {
                if (!newSettings.getShowTitle()) {
                    window.requestFeature(Window.FEATURE_NO_TITLE);
                } else {
                    // Android 3.0+ you need both progress!!!
                    window.requestFeature(Window.FEATURE_PROGRESS);
                    window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
                    setProgressBarIndeterminate(true);
                }
            } catch (Throwable th) {
                LCTX.e("Error on requestFeature call: " + th.getMessage());
            }
        }
        if (diff.isKeepScreenOnChanged()) {
            view.getView().setKeepScreenOn(newSettings.isKeepScreenOn());
        }

        if (diff.isNightModeChanged() && !diff.isFirstTime()) {
            getDocumentController().toggleNightMode(newSettings.getNightMode());
        }
    }

    @Override
    public void onBookSettingsChanged(BookSettings oldSettings,
                                      BookSettings newSettings, BookSettings.Diff diff,
                                      AppSettings.Diff appDiff) {

        boolean redrawn = false;
        if (diff.isViewModeChanged() || diff.isSplitPagesChanged() || diff.isCropPagesChanged()) {
            redrawn = true;
            IDocumentViewController newDc = switchDocumentController();
            if (!diff.isFirstTime()) {
                newDc.init(null);
                newDc.show();
            }
        }

        if (diff.isFirstTime()) {
            getZoomModel().initZoom(newSettings.getZoom());
        }

        IDocumentViewController dc = getDocumentController();
        if (diff.isPageAlignChanged()) {
            dc.setAlign(newSettings.pageAlign);
        }

        if (diff.isAnimationTypeChanged()) {
            dc.updateAnimationType();
        }

        if (!redrawn && appDiff != null) {
            if (appDiff.isMaxImageSizeChanged() || appDiff.isPagesInMemoryChanged()
                    || appDiff.isDecodeModeChanged()) {
                dc.updateMemorySettings();
            }
        }

        DocumentModel dm = getDocumentModel();
        if (dm != null) {
            currentPageChanged(PageIndex.NULL, dm.getCurrentIndex());
        }
    }

    private class BookLoadTask extends AsyncTask<String, String, Exception> implements IBookLoadTask, Runnable {

        private DecodeService m_decodeService;
        private String m_fileName;
        private String m_password;
        private ProgressDialog progressDialog;

        BookLoadTask(DecodeService decodeService, String fileName,
                     String password) {
            m_decodeService = decodeService;
            m_fileName = fileName;
            m_password = password;
        }

        @Override
        public void run() {
            execute(" ");
        }

        @Override
        protected void onPreExecute() {
            LCTX.d("onPreExecute(): start");
            try {
                String message = getString(R.string.pdf_msg_loading);
                progressDialog = ProgressDialog.show(BaseViewerActivity.this, "", message, true);
            } catch (Throwable th) {
                LCTX.e("Unexpected error", th);
            } finally {
                LCTX.d("onPreExecute(): finish");
            }
        }

        @Override
        protected Exception doInBackground(String... params) {
            LCTX.d("doInBackground(): start");
            try {
                if (getIntent().getScheme().equals("content")) {
                    File tempFile = CacheManager.createTempFile(getIntent().getData());
                    m_fileName = tempFile.getAbsolutePath();
                }
                getView().waitForInitialization();

                m_decodeService.open(m_fileName, m_password);

                getDocumentController().init(this);

                if (null != documentModel && documentModel.getPageCount() == 0) {
                    LCTX.e("page size is 0");
                    return new Exception(getString(R.string.pdf_viewer_toast_file_corrupted));
                }
                return null;
            } catch (Exception e) {
                if (!TextUtils.isEmpty(e.getMessage()) && e.getMessage().contains("file is corrupted")) {
                    LCTX.e("file is corrupted", e);
                    return new Exception(getString(R.string.pdf_viewer_toast_file_corrupted));
                } else {
                    return new Exception(getString(R.string.pdf_viewer_toast_file_unsupport));
                }
            } catch (Throwable th) {
                LCTX.e("Unexpected error", th);
                return new Exception(th.getMessage());
            } finally {
                LCTX.d("doInBackground(): finish");
            }
        }

        @Override
        protected void onPostExecute(Exception result) {
            LCTX.d("onPostExecute(): start");
            try {
                if (result == null) {
                    getDocumentController().show();

                    DocumentModel dm = getDocumentModel();
                    currentPageChanged(PageIndex.NULL, dm.getCurrentIndex());

                    setProgressBarIndeterminateVisibility(false);
                    progressDialog.dismiss();
                } else {
                    progressDialog.dismiss();
                    final String msg = result.getMessage();
                    if (getString(R.string.pdf_viewer_toast_file_corrupted).equals(msg)) {
                        onOpenErrorPdfFile();
                    } else {
                        showErrorDlg(msg);
                    }
                }
            } catch (Throwable th) {
                LCTX.e("Unexpected error", th);
            } finally {
                LCTX.d("onPostExecute(): finish");
            }
        }

        @Override
        public void setProgressDialogMessage(int resourceID, Object... args) {
            String message = getString(resourceID, args);
            publishProgress(message);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            if (values != null && values.length > 0) {
                progressDialog.setMessage(values[0]);
            }
        }

    }

    private class EmptyContoller implements IDocumentViewController {

        @Override
        public void zoomChanged(float newZoom, float oldZoom) {
        }

        @Override
        public void commitZoom() {
        }

        @Override
        public void goToPage(int page) {
        }

        @Override
        public void invalidatePageSizes(InvalidateSizeReason reason, Page changedPage) {
        }

        @Override
        public int getFirstVisiblePage() {
            return 0;
        }

        @Override
        public int calculateCurrentPage(ViewState viewState) {
            return 0;
        }

        @Override
        public int getLastVisiblePage() {
            return 0;
        }

        @Override
        public void verticalConfigScroll(int i) {
        }

        @Override
        public void redrawView() {
        }

        @Override
        public void redrawView(ViewState viewState) {
        }

        @Override
        public void setAlign(PageAlign byResValue) {
        }

        @Override
        public IViewerActivity getBase() {
            return BaseViewerActivity.this;
        }

        @Override
        public IDocumentView getView() {
            return view;
        }

        @Override
        public void updateAnimationType() {
        }

        @Override
        public void updateMemorySettings() {
        }

        @Override
        public void drawView(Canvas canvas, ViewState viewState) {
        }

        @Override
        public boolean onLayoutChanged(boolean layoutChanged, boolean layoutLocked,
                                       Rect oldLaout, Rect newLayout) {
            return false;
        }

        @Override
        public Rect getScrollLimits() {
            return new Rect(0, 0, 0, 0);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            return false;
        }

        @Override
        public void onScrollChanged(int newPage, int direction) {
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            return false;
        }

        @Override
        public ViewState updatePageVisibility(int newPage, int direction,
                                              float zoom) {
            return new ViewState(this);
        }

        @Override
        public void show() {
        }

        @Override
        public void init(IBookLoadTask task) {
        }

        @Override
        public void pageUpdated(int viewIndex) {
        }

        @Override
        public void toggleNightMode(boolean nightMode) {
        }
    }

    protected abstract DecodeService createDecodeService();
}