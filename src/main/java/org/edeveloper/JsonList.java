package org.edeveloper;

import java.util.ArrayList;
import java.util.HashMap;

final public class JsonList{

    private ArrayList<Object> arrayList;
    private int size;
    public JsonList(ArrayList<Object> arrayList){
        this.arrayList = arrayList;
        this.size = arrayList.size();
    }

    public Object getValue(int indx){
        Object o = null;
        try{
            o = arrayList.get(indx);
        }catch (Exception e){
            e.printStackTrace();
        }
        return o;
    }

    public JsonList getList(int indx){
        JsonList jsonList = null;
        try{
            jsonList = new JsonList((ArrayList<Object>) arrayList.get(indx));
        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonList;
    }

    public JsonMap getMap(int indx){
        JsonMap jsonMap = null;
        try{
            jsonMap = new JsonMap((HashMap<String, Object>) arrayList.get(indx));
        }catch (Exception e){
            e.printStackTrace();
        }
        return jsonMap;
    }

    public int getSize() {
        return size;
    }
}
