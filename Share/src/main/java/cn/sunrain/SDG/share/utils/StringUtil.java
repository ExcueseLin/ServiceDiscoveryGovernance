package cn.sunrain.SDG.share.utils;

public class StringUtil {


    public static String join(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append(',').append(value);
        }
        return sb.substring(1);
    }



}
