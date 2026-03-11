package com.example.demo.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class CustomFileUtil {
    /**
     * 文件名生成
     * @param filename 文件名
     * @return 文件名
     */
    public static String getFilename(String filename){
        return System.currentTimeMillis() + "-" + UUID.fastUUID() + "-"+ filename;
    }


    /**
     * 通过url读取远程文本内容
     */
    public static String readRemoteFile(String urlStr) {
        try {
            URL url = new URL(urlStr);
            URLConnection connection = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("远程文件读取异常");
        }
    }

    public static void mkdir(String dir) {
        FileUtil.mkdir(dir);
    }

    /**
     * 获取文件后缀
     * @param remoteFilePath
     * @return
     */
    public static String getSuffix(String remoteFilePath) {
        if (remoteFilePath.contains(".")) {
            return remoteFilePath.substring(remoteFilePath.lastIndexOf("."));
        }
        return "";
    }
}

