package com.winlator.fusion.box64;

import android.content.Context;

import com.winlator.fusion.core.ArrayUtils;
import com.winlator.fusion.core.StreamUtils;
import com.winlator.fusion.xenvironment.FusionFS;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class Box64Utils {
    public static String extractBinVersion(Context context, String variant) {
        FusionFS fusionFS = FusionFS.find(context);
        File binFile = new File(fusionFS.getBox64PathForVariant(variant));
        try (BufferedInputStream inStream = new BufferedInputStream(new FileInputStream(binFile), StreamUtils.BUFFER_SIZE)) {
            int bytesRead;
            byte[] buffer = new byte[4096];
            final byte[] str = {'B','o','x','6','4',' ','a','r','m','6','4',' ','v'};
            while ((bytesRead = inStream.read(buffer)) != -1) {
                int index = ArrayUtils.indexOf(buffer, 0, bytesRead, str);
                if (index != ArrayUtils.INDEX_NOT_FOUND) {
                    int start = index + str.length;
                    int end = ArrayUtils.indexOf(buffer, start, bytesRead, (byte)' ');
                    return end != ArrayUtils.INDEX_NOT_FOUND ? new String(buffer, start, end - start) : "";
                }
            }
        }
        catch (IOException e) {}
        return "";
    }

    public static String extractBinVersion(Context context) {
        return extractBinVersion(context, com.winlator.fusion.container.Container.GLIBC);
    }
}
