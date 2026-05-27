package com.winlator.fusion.core;

import com.winlator.fusion.xenvironment.RootFS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class EvshimPatcher {
    private static final String WINEBUS_SO = "winebus.so";
    private static final String TAG = "EvshimPatcher";

    private static final byte[] SEARCH_PATTERN = "libudev.so.0\0".getBytes();
    private static final byte[] REPLACE_PATTERN = "libevshim.so\0".getBytes();

    public static boolean patch(File winePrefixDir) {
        File wineBusFile = findWineBusSo(winePrefixDir);
        if (wineBusFile == null || !wineBusFile.exists()) return false;

        try {
            byte[] fileContent = readFile(wineBusFile);
            int offset = findPattern(fileContent, SEARCH_PATTERN);
            if (offset == -1) return false;

            System.arraycopy(REPLACE_PATTERN, 0, fileContent, offset, REPLACE_PATTERN.length);
            writeFile(wineBusFile, fileContent);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static File findWineBusSo(File rootDir) {
        String[] searchPaths = {
            "/home/xuser/.wine/drive_c/windows/system32/" + WINEBUS_SO,
            RootFS.WINEPREFIX + "/drive_c/windows/system32/" + WINEBUS_SO,
            "/lib/wine/x86_64-unix/" + WINEBUS_SO,
            "/lib64/wine/x86_64-unix/" + WINEBUS_SO,
            "/usr/lib/wine/x86_64-unix/" + WINEBUS_SO,
            "/lib/wine/aarch64-unix/" + WINEBUS_SO,
            "/usr/lib/wine/aarch64-unix/" + WINEBUS_SO
        };

        for (String path : searchPaths) {
            File file = new File(rootDir, path);
            if (file.exists()) return file;
        }

        File optDir = new File(rootDir, "opt");
        if (optDir.isDirectory()) {
            File[] optFiles = optDir.listFiles();
            if (optFiles != null) {
                for (File optFile : optFiles) {
                    String name = optFile.getName();
                    if (name.startsWith("proton") || name.startsWith("wine")) {
                        File arm64ec = new File(optFile, "lib/wine/aarch64-unix/" + WINEBUS_SO);
                        if (arm64ec.exists()) return arm64ec;
                        File x86_64 = new File(optFile, "lib/wine/x86_64-unix/" + WINEBUS_SO);
                        if (x86_64.exists()) return x86_64;
                    }
                }
            }
        }
        return null;
    }

    private static int findPattern(byte[] data, byte[] pattern) {
        for (int i = 0; i <= data.length - pattern.length; i++) {
            boolean found = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    private static byte[] readFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        }
    }

    private static void writeFile(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }
}
