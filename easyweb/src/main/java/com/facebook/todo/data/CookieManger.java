package com.facebook.todo.data;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class CookieManger {

    private static String FILE_NAME = "cookie_cache";

    private static CookieManger sCookieManger;

    private File mAppDataDir;
    private String mLastCookie;

    public static CookieManger getInstance() {
        if (sCookieManger == null) {
            sCookieManger = new CookieManger();
        }
        return sCookieManger;
    }

    public void init(File file) {
        mAppDataDir = file;
    }

    public void parse(Map<String, String> headers) {
        String rawCookie = headers.get("Set-Cookie");
        Log.e("wtf", rawCookie);
        mLastCookie = rawCookie;
    }

    /**
     * rawCookie:
     * <p>
     * token1=OXR4eWllaWxhcXpnZmducndqYmxrZmJmcm1sbA==; Max-Age=7200; Expires=Thu, 28-Jun-2018 12:06:30 GMT; Path=/(\n)
     * token2=Y2FjZDcyZmE5ZTQxM2E1MGI4YTUxMTFmOTc1Yjk2ZWI=; Max-Age=7200; Expires=Thu, 28-Jun-2018 12:06:30 GMT; Path=/
     */
    public String getLastCookie() {
        return mLastCookie;
    }

    /**
     * 获取名值对
     *
     * @param keyword
     * @return eg. token1=NzF4aGpldmJhcHRmd3NleHZucWJudm1ocWU4NQ==; token2=NzQ5ZjAxMjE0ZjQzZWE4ZjI3NGIyYzkyNTIzYmY0MWQ=
     */
    public String getNameValuePair(String keyword) {
        if (TextUtils.isEmpty(mLastCookie)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String[] cookies = mLastCookie.split("\n");
        for (String cookie : cookies) {
            builder.append(indexOfNameValuePair(cookie, keyword)).append("; ");
        }
        if (builder.length() > 0) {
            builder.delete(builder.length() - 2, builder.length());
        }
        return builder.toString();
    }

    private String indexOfNameValuePair(String cookie, String keyword) {
        String[] splits = cookie.split(";");
        for (String split : splits) {
            if (split.contains(keyword)) {
                return split;
            }
        }
        return "";
    }

    private void saveRawCookies() {
        File file = new File(mAppDataDir, FILE_NAME);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(mLastCookie.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getRawCookies() {
        String result = "";
        File file = new File(mAppDataDir, FILE_NAME);
        if (!file.exists()) return result;

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] bytes = new byte[inputStream.available()];
            while (inputStream.read(bytes) != -1) {
                result = new String(bytes);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            return result;
        }
    }

    public void clearRawCookies() {
        mLastCookie = null;
        File file = new File(mAppDataDir, FILE_NAME);
        if (file.exists()) {
            file.delete();
        }
    }
}
