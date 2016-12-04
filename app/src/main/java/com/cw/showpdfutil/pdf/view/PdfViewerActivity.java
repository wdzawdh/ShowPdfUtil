/**
 *
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

import org.ebookdroid.core.DecodeService;
import org.ebookdroid.core.DecodeServiceBase;
import org.ebookdroid.pdfdroid.codec.PdfContext;

/**
 *
 * @author Cw
 * @date 16/12/3
 */
public class PdfViewerActivity extends BaseViewerActivity {

    @Override
    protected DecodeService createDecodeService() {
        return new DecodeServiceBase(new PdfContext());
    }

}