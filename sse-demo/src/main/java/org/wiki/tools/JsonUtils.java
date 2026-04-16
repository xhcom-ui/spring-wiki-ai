package org.wiki.tools;

import com.google.gson.Gson;
import org.springframework.stereotype.Component;

@Component
public class JsonUtils {
    private static final Gson gson = new Gson();
    
    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }
    
    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }
}