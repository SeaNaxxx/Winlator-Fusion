package com.winlator.nova.core;

import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Patches the rootfs after extraction to fix hardcoded package paths.
 *
 * The rootfs tarballs were built for com.winlator but Nova uses com.winlator.nova.
 * This class rewrites text config files containing the old path and handles
 * binary ELF files that need the correct dynamic linker invocation.
 */
public abstract class RootFSPatcher {
    private static final String TAG = "RootFSPatcher";

    // Old and new package paths
    private static final String OLD_PATH = "/data/data/com.winlator/files/rootfs";
    private static final String NEW_PATH = "/data/data/com.winlator.nova/files/rootfs";

    // Also fix paths from Cmod's package name (in wrapper.tzst and other Ludashi assets)
    private static final String OLD_CMOD_PATH = "/data/data/com.winlator.cmod/files/imagefs";
    private static final String OLD_CMOD_CACHE = "/data/data/com.winlator.cmod/files/cache";

    // Text file extensions that should be scanned for path replacement
    private static final String[] TEXT_EXTENSIONS = {
        ".conf", ".json", ".xml", ".txt", ".cfg", ".ini", ".sh",
        ".so", ".list", ".cache", ".log", ".properties", ".desktop",
        ".reg", ".pol", ".manifest", ".mime", ".types"
    };

    // Specific text files inside rootfs that are known to contain hardcoded paths
    private static final String[] KNOWN_TEXT_FILES = {
        "etc/ld.so.cache",
        "usr/lib/libc.so",
        "usr/lib/libm.so",
        "usr/lib/libpthread.so",
        "usr/lib/libdl.so",
        "usr/lib/librt.so",
        "usr/share/alsa/alsa.conf",
        "etc/fonts/fonts.conf",
        "etc/pulse/client.conf",
        "usr/share/vulkan/icd.d/vortek_icd.aarch64.json",
        "usr/share/vulkan/icd.d/freedreno_icd.aarch64.json"
    };

    /**
     * Patches the rootfs directory after extraction.
     * This must be called on a background thread.
     *
     * @param rootDir The rootfs directory
     * @return true if patching was successful
     */
    public static boolean patch(File rootDir) {
        if (rootDir == null || !rootDir.isDirectory()) return false;

        Log.i(TAG, "Patching rootfs at: " + rootDir.getAbsolutePath());
        int patchedCount = 0;

        // 1. Delete the ld.so.cache since it contains binary data with old paths
        //    that cannot be safely rewritten. The linker will use LD_LIBRARY_PATH
        //    and the env vars set by GuestProgramLauncherComponent instead.
        File ldCache = new File(rootDir, "etc/ld.so.cache");
        if (ldCache.exists()) {
            if (ldCache.delete()) {
                Log.i(TAG, "Deleted broken ld.so.cache");
                patchedCount++;
            } else {
                Log.w(TAG, "Failed to delete ld.so.cache");
            }
        }

        // 2. Fix known text config files
        for (String relativePath : KNOWN_TEXT_FILES) {
            File file = new File(rootDir, relativePath);
            if (file.exists() && !file.getName().equals("ld.so.cache")) {
                if (patchTextFile(file)) patchedCount++;
            }
        }

        // 3. Scan and fix Vulkan ICD JSON files
        File vulkanICDDir = new File(rootDir, "usr/share/vulkan/icd.d");
        if (vulkanICDDir.isDirectory()) {
            File[] icdFiles = vulkanICDDir.listFiles();
            if (icdFiles != null) {
                for (File icdFile : icdFiles) {
                    if (icdFile.getName().endsWith(".json") && patchTextFile(icdFile)) {
                        patchedCount++;
                    }
                }
            }
        }

        // 4. Fix ALSA config directory
        File alsaConfDir = new File(rootDir, "etc/alsa/conf.d");
        if (alsaConfDir.isDirectory()) {
            File[] alsaFiles = alsaConfDir.listFiles();
            if (alsaFiles != null) {
                for (File alsaFile : alsaFiles) {
                    if (patchTextFile(alsaFile)) patchedCount++;
                }
            }
        }

        // 5. Fix drirc files (VirGL driver config)
        patchTextFile(new File(rootDir, "usr/share/drirc.d/00-mesa-defaults.conf"));
        patchTextFile(new File(rootDir, "usr/etc/drirc"));

        // 6. Fix locale Compose files that may contain hardcoded paths
        File localeDir = new File(rootDir, "usr/share/X11/locale");
        if (localeDir.isDirectory()) {
            patchedCount += patchDirectoryTextFiles(localeDir);
        }

        // 7. Fix pulseaudio config files
        File pulseDir = new File(rootDir, "etc/pulse");
        if (pulseDir.isDirectory()) {
            File[] pulseFiles = pulseDir.listFiles();
            if (pulseFiles != null) {
                for (File pulseFile : pulseFiles) {
                    if (patchTextFile(pulseFile)) patchedCount++;
                }
            }
        }

        // 8. Fix libGL.so linker scripts (text files with .so extension)
        File usrLibDir = new File(rootDir, "usr/lib");
        if (usrLibDir.isDirectory()) {
            File[] libFiles = usrLibDir.listFiles();
            if (libFiles != null) {
                for (File libFile : libFiles) {
                    String name = libFile.getName();
                    // These are linker scripts (text files) that contain absolute paths
                    if ((name.startsWith("lib") && name.endsWith(".so") && libFile.length() < 500)
                            || name.equals("libGL.so.1.7.0")) {
                        if (patchTextFile(libFile)) patchedCount++;
                    }
                }
            }
        }

        // 9. Fix lib directory linker scripts
        File libDir = new File(rootDir, "lib");
        if (libDir.isDirectory()) {
            File[] libFiles = libDir.listFiles();
            if (libFiles != null) {
                for (File libFile : libFiles) {
                    String name = libFile.getName();
                    if (name.startsWith("lib") && name.endsWith(".so") && libFile.length() < 500) {
                        if (patchTextFile(libFile)) patchedCount++;
                    }
                }
            }
        }

        Log.i(TAG, "RootFS patching complete. Patched " + patchedCount + " files.");
        return true;
    }

    /**
     * Patches all text files in a directory recursively.
     */
    private static int patchDirectoryTextFiles(File directory) {
        int count = 0;
        File[] files = directory.listFiles();
        if (files == null) return 0;
        for (File file : files) {
            if (file.isDirectory()) {
                count += patchDirectoryTextFiles(file);
            } else if (isTextFile(file) && patchTextFile(file)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Determines if a file is likely a text file based on extension.
     */
    private static boolean isTextFile(File file) {
        String name = file.getName().toLowerCase();
        for (String ext : TEXT_EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        // Small .so files are likely linker scripts (text)
        if (name.endsWith(".so") && file.length() < 500) return true;
        return false;
    }

    /**
     * Patches a text file by replacing old package paths with new ones.
     * The file is read, string replacement is performed, and the file is rewritten.
     *
     * @param file The text file to patch
     * @return true if the file was modified
     */
    private static boolean patchTextFile(File file) {
        if (!file.exists() || !file.canRead() || !file.canWrite()) return false;

        try {
            byte[] data = FileUtils.read(file);
            if (data == null) return false;

            String content = new String(data, StandardCharsets.UTF_8);
            String original = content;

            // Replace old package paths with new ones
            content = content.replace(OLD_PATH, NEW_PATH);
            content = content.replace(OLD_CMOD_PATH, NEW_PATH);
            content = content.replace(OLD_CMOD_CACHE, "/data/data/com.winlator.nova/cache");

            if (!content.equals(original)) {
                FileUtils.write(file, content.getBytes(StandardCharsets.UTF_8));
                Log.i(TAG, "Patched: " + file.getAbsolutePath());
                return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to patch text file: " + file.getAbsolutePath(), e);
        }
        return false;
    }

    /**
     * Patches binary ELF files by replacing RPATH strings.
     * This handles .so files and executables that have embedded paths.
     *
     * For ELF binaries, we perform a binary-safe string replacement.
     * The new path is longer than the old path, so we can't do simple in-place
     * replacement. Instead, we rely on the explicit linker invocation in
     * GuestProgramLauncherComponent and the env vars (LD_LIBRARY_PATH) to
     * override any RPATH/INTERP settings at runtime.
     *
     * The only thing we need to fix in binaries is the RPATH that references
     * the Cmod package path, since those strings are the right length for
     * in-place replacement.
     *
     * @param file The ELF binary file
     * @return true if the file was modified
     */
    public static boolean patchElfRpath(File file) {
        if (!file.exists() || !file.canRead() || !file.canWrite()) return false;

        try {
            byte[] data = FileUtils.read(file);
            if (data == null) return false;

            boolean modified = false;

            // Check for Cmod path in binary - same length replacement
            String cmodPath = "/data/data/com.winlator.cmod/files/imagefs";
            String novaPath = "/data/data/com.winlator.nova/files/rootfs";
            // Both are 44 chars - safe for in-place binary replacement

            byte[] cmodBytes = cmodPath.getBytes(StandardCharsets.UTF_8);
            byte[] novaBytes = novaPath.getBytes(StandardCharsets.UTF_8);

            if (cmodBytes.length == novaBytes.length) {
                int index = indexOf(data, cmodBytes);
                while (index != -1) {
                    System.arraycopy(novaBytes, 0, data, index, novaBytes.length);
                    modified = true;
                    index = indexOf(data, cmodBytes, index + novaBytes.length);
                }
            }

            if (modified) {
                FileUtils.write(file, data);
                Log.i(TAG, "Patched ELF RPATH: " + file.getAbsolutePath());
            }
            return modified;
        } catch (Exception e) {
            Log.w(TAG, "Failed to patch ELF: " + file.getAbsolutePath(), e);
        }
        return false;
    }

    /**
     * Finds the index of a byte pattern in a byte array.
     */
    private static int indexOf(byte[] data, byte[] pattern) {
        return indexOf(data, pattern, 0);
    }

    private static int indexOf(byte[] data, byte[] pattern, int start) {
        if (pattern.length == 0) return -1;
        outer:
        for (int i = start; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }
}
