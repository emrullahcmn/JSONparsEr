package org.edeveloper;

import java.util.ArrayList;
import java.util.HashMap;

final public class JsonMap {

    private HashMap<String, Object> hashMap;
    public JsonMap(HashMap<String, Object> hashMap){
        this.hashMap = hashMap;
    }

    public Object getValue(String key){
        Object o = null;
        try{
            o = hashMap.get(key);
        }catch (Exception e){
            e.printStackTrace();
        }
        return o;
    }

    public JsonList getList(String key){
        JsonList jsonList = null;
        try{
            jsonList = new JsonList((ArrayList<Object>) hashMap.get(key));
        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonList;
    }

    public JsonMap getMap(String key){
        JsonMap jsonMap = null;
        try{
            jsonMap = new JsonMap((HashMap<String, Object>) hashMap.get(key));

        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonMap;
    }

}
