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

import java.io.InputStream;

/**
 * @author Cw
 * @date 16/12/3
 */
public abstract class ProgressListener {

    public abstract void onProgress(long currentBytes, long contentLength, boolean done);

    public void onSuccess(InputStream body){
    }

    public void onError(Exception e){
    }

    public void onFailure(Object body){
    }
}