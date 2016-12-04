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

package com.cw.showpdfutil.utils;

import java.math.BigDecimal;

/**
 *
 * @author Cw
 * @date 16/12/3
 */
public class FileSizeUtil {

    public static final int SIZE_KB = 1024;
    public static final int SIZE_MB = 1024 * 1024;

    public static String bytes2kb(long bytes) {
        if( bytes <= 0 ){
            return 0 +" KB";
        }
        BigDecimal filesize = new BigDecimal(bytes);
        BigDecimal megabyte = new BigDecimal(SIZE_MB);
        float returnValue = filesize.divide(megabyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        if (returnValue > 1)
            return (returnValue + " MB");
        BigDecimal kilobyte = new BigDecimal(SIZE_KB);
        returnValue = filesize.divide(kilobyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        return (returnValue + " KB");
    }

}