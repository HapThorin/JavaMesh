package com.huawei.apm.bootstrap.extagent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ExtAgentUtils {
    private static final int bufferSize = 1024 * 16;

    public static boolean createParentDir(File file) {
        final File parentDir = file.getParentFile();
        return parentDir.exists() || parentDir.mkdirs();
    }

    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(sourceFile);
            outputStream = new FileOutputStream(targetFile);
            final byte[] buffer = new byte[bufferSize];
            int n;
            while (0 <= (n = inputStream.read(buffer))) {
                outputStream.write(buffer, 0, n);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void copyAllFiles(File sourceFile, String targetPath) throws IOException {
        if (sourceFile.isFile()) {
            final File targetFile = new File(targetPath);
            if (createParentDir(targetFile)) {
                copyFile(sourceFile, targetFile);
            }
        } else {
            final File[] subFiles = sourceFile.listFiles();
            if (subFiles != null) {
                for (File subFile : subFiles) {
                    copyAllFiles(subFile, targetPath + File.separatorChar + subFile.getName());
                }
            }
        }
    }

    public static boolean deleteDirs(File file) {
        if (!file.exists()) {
            return true;
        }
        if (file.isDirectory()) {
            final File[] subFiles = file.listFiles();
            if (subFiles != null) {
                boolean result = true;
                for (File subFile : subFiles) {
                    result &= deleteDirs(subFile);
                }
                return result && file.delete();
            }
        }
        return file.delete();
    }

    public static boolean isWildcardMatch(String str, String wc) {
        final char[] strArr = str.toCharArray();
        final char[] wcArr = wc.toCharArray();
        int wcCursor = 0;
        for (int strCursor = 0, starIdx = -1, starCursor = 0; strCursor < strArr.length; ) {
            if (wcCursor < wcArr.length && wcArr[wcCursor] != '*' &&
                    (wcArr[wcCursor] == '?' || strArr[strCursor] == wcArr[wcCursor])) {
                strCursor++;
                wcCursor++;
            } else if (wcCursor < wcArr.length && wcArr[wcCursor] == '*') {
                starIdx = wcCursor;
                starCursor = strCursor;
                wcCursor++;
            } else if (starIdx >= 0) {
                starCursor++;
                wcCursor = starIdx + 1;
                strCursor = starCursor;
            } else {
                return false;
            }
        }
        for (; wcCursor < wcArr.length; wcCursor++) {
            if (wcArr[wcCursor] != '*') {
                return false;
            }
        }
        return true;
    }
}
