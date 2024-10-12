package cn.ccg.grpc.util;

import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;


/**
 * 封装各种格式的编码解码工具类.
 * 1.Commons-Codec的 hex/base64 编码
 * 4.JDK提供的URLEncoder
 */
@Slf4j
public class MD5Encodes {

    private static final String DEFAULT_URL_ENCODING = "UTF-8";


    /**
     * MD5加密.
     */
    public static String MD5(String sourceStr) {
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes());
            byte[] b = md.digest();
            int i;
            StringBuilder buf = new StringBuilder();
            for (byte value : b) {
                i = value;
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();
        } catch (NoSuchAlgorithmException e) {
            log.warn("MD5 encode failed.",e);
        }
        return result;
    }

    /**
     * 二维码MD5加密
     */
    public static String qrEncodeMD5(String message) {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(message.getBytes(StandardCharsets.UTF_8));
            //converting byte array to Hexadecimal String
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            digest = sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            //Logger.getLogger(StringReplace.class.getName()).log(Level.SEVERE, null, ex);
        }
        return digest;
    }

    /**
     * SHA1 加密算法
     */
    public static String SHA1(String message) {

        try {
            //指定sha1算法  
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(message.getBytes());
            //获取字节数组  
            byte[] messageDigest = digest.digest();
            // Create Hex String  
            StringBuffer hexString = new StringBuffer();
            // 字节数组转换为 十六进制 数  
            for (int i = 0; i < messageDigest.length; i++) {
                String shaHex = Integer.toHexString(messageDigest[i] & 0xFF);
                if (shaHex.length() < 2) {
                    hexString.append(0);
                }
                hexString.append(shaHex);
            }
            message = hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            log.warn("SHA1 encode failed.",e);
        }
        return message;
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }


}
