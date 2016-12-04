package com.cw.showpdfutil;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.cw.showpdfutil.pdf.ShowPdfUtil;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.bt_download_pdf)
    public void onClick() {
        String url = "https://github.com/wdzawdh/GetPhotoUtils/raw/master/app/src/main/res/drawable/test.pdf";
        ShowPdfUtil.onGetPdfUrl(this, url, "test.pdf", false);
    }
}
