package com.baidu.nlog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

public final class NLog {
    /**
     * nlog
     * @description Nativeͳ�ƿ��
     * @author ������(WangJihu,http://weibo.com/zswang),����ɽ(PengZhengshan)
     * @version 1.0
     * @copyright www.baidu.com
     */
    // ��־TAG
    private static String LOGTAG = (new Object() {
        public String getClassName() {
            String clazzName = this.getClass().getName();
            return clazzName.substring(0, clazzName.lastIndexOf('$'));
        }
    }).getClassName();
    /**
     * ׷�������ϣ���nameΪ�±�
     */
    private static Map<String, Object> fields;
    
    /**
     * ��ȡ�ֶ�ֵ
     * @param key ��ֵ��
     * @return ���ؼ�ֵ��Ӧ������
     */
    public static Object get(String key) {
        /* debug start */
        Log.d(LOGTAG, String.format("get('%s') => %s", key, fields.get(key)));
        /* debug end */
        
        return fields.get(key);
    }
    /**
     * ��ȡ�����ֶ�ֵ
     * @param key ��ֵ��
     * @return ���ؼ�ֵ��Ӧ������
     */
    public static Integer getInteger(String key) {
        /* debug start */
        Log.d(LOGTAG, String.format("get('%s') => %s", key, fields.get(key)));
        /* debug end */
        Object configField = configFields.get(key);
        if (configField == null) {
            return null;
        } 
        return safeInteger(fields.get(key), ((ConfigField)configField).defaultValue);
    }
    /**
     * �Ƿ��Ѿ���ʼ��
     */
    private static Boolean initCompleted = false;
    public static Boolean getInitCompleted() {
        return initCompleted;
    }
    
    /**
     * ��ȫ��ȡ������ֵ
     * @param value �������ַ���������
     * @param defaultValue Ĭ��ֵ
     * @return
     */
    @SuppressLint("UseValueOf")
    public static Integer safeInteger(Object value, Integer defaultValue) {
        Integer result = defaultValue;
        if (value != null) {
            if (value instanceof Integer) {
                result = (Integer)value;
            } else {
                try {
                    result = new Integer(value.toString());
                } catch(NumberFormatException e) {
                }
            }
        }
        return result;
    }
    /**
     * ��ȫ��ȡ������ֵ
     * @param value �������ַ���������
     * @param defaultValue Ĭ��ֵ
     * @return
     */
    @SuppressLint("UseValueOf")
    public static Double safeDouble(Object value, Double defaultValue) {
        Double result = defaultValue;
        if (value != null) {
            if (value instanceof Double) {
                result = (Double)value;
            } else {
                try {
                    result = new Double(value.toString());
                } catch(NumberFormatException e) {
                }
            }
        }
        return result;
    }
    /**
     * ��ȫ��ȡ�߼�ֵ
     * @param value �������ַ���
     * @param defaultValue Ĭ��ֵ
     * @return
     */
    @SuppressLint("UseValueOf")
    public static Boolean safeBoolean(Object value, Boolean defaultValue) {
        Boolean result = defaultValue;
        if (value != null) {
            if (value instanceof Boolean) {
                result = (Boolean)value;
            } else {
                result = new Boolean(value.toString());
            }
        }
        return result;
    }
    /**
     * �����ֶ�
     */
    private static class ConfigField {
        Integer defaultValue;
        Integer minValue;
        Integer maxValue;
        ConfigField(Integer defaultValue, Integer minValue, Integer maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.defaultValue = defaultValue;
        }
    }
   /*
    | ����             | ˵��                | ��λ  | Ĭ��ֵ |ȡֵ��Χ|
    | --------------- | -------------------| ------|------:|-------|
    | ruleUrl         | �ƶ˲��Դ�ŵ�·��     |       |null   |       |
    | ruleExpires     | �����ļ�����ʱ��       |��     | 2     |2-30   |
    | onlywifi        | ֻ��wifi�����·���    |bool   | false |       |
    | sendMaxLength   | ���η������İ�����   |KB     | 200   |2-500 |
    | sendInterval    | �ط���������          |��     | 300   |30-600 |
    | sendIntervalWifi| ��wifi�����µ��ط����� |��     | 150   |30-600 |
    | sessionTimeout  | �Ự��ʱʱ��          |��     | 30    |30-120 |
    | storageExpires  | �������ݹ���ʱ��       |��     | 10    |2-30  |
    | sampleRate      | ����Tracker�ĳ�����   |������  |[1...] |0-1    |
    */
    private static Map<String, Object> configFields;
    /**
     * �ڲ���ʼ��
     */
    static {
        configFields = buildMap(
            "ruleExpires=", new ConfigField(5, 2, 30),
            "sendMaxLength", new ConfigField(2, 500, 200),
            "sendInterval", new ConfigField(300, 30, 600),
            "sendIntervalWifi", new ConfigField(150, 30, 600),
            "sessionTimeout", new ConfigField(30, 30, 120),
            "storageExpires", new ConfigField(10, 2, 30)
        );
    }

    /**
     * ��ʼ��
     * @param context ������
     * @param params ��ʼ������
     */
    @SuppressLint({ "UseValueOf", "DefaultLocale" })
    @SuppressWarnings("unchecked")
    public static void init(Context context, Object... params) {
        if (initCompleted) {
            Log.w(LOGTAG, "init() Can't repeat initialization.");
            return;
        }

        if (context == null) {
            Log.w(LOGTAG, "init() Context can't for empty.");
            return;
        }
        pauseTime = System.currentTimeMillis();
        initCompleted = true;
        Context app = context.getApplicationContext();

        fields = mergeMap(buildMap(
                "ruleUrl=", null,
                "ruleExpires=", 2
        ), buildMap(params));
        fields.put("applicationContext", app);
        
        // �����¼� onXdddd -> xdddd
        for (String key : fields.keySet()) {
            Object listener = fields.get(key);
            if (!(listener instanceof EventListener)) {
                continue;
            }
            Matcher matcher = eventPattern.matcher(key);
            if (matcher.find()) {
                on(key.substring(2, 3).toLowerCase() + key.substring(3), (EventListener)listener);  
            }   
        }
        
        // ����ֵ����������Χ
        for (String key : configFields.keySet()) {
            ConfigField configField = (ConfigField)configFields.get(key); 
            fields.put(key, Math.min(
                    Math.max(safeInteger(fields.get(key), configField.defaultValue), configField.minValue),
                    configField.maxValue
            ));
        }
        
        // ���ó�����
        Object items = fields.get("sampleRate");
        if (items != null && items instanceof Map) {
            Map<String, ?> map = (Map<String, ?>)items;
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                sampleRate.put(key.toString(),
                        Math.max(Math.min(safeDouble(value, 1.0), 1), 0));
            }
        }
                
        NStorage.init();
        createSession();
        
        // ����δ��ʼ��ǰ������
        for (CmdParamItem item : cmdParamList) {
            item.tracker.command(item.method, item.params);
        }
        cmdParamList.clear();
        
        /* debug start */
        Log.i(LOGTAG, String.format("NLog.init(%s, %s) fields => %s", context, buildMap(params), fields));
        /* debug end */
    }
    
    /**
     * ��ȡ�豸������
     */
    public static Context getContext() {
        return (Context)fields.get("applicationContext");
    }
    
    /**
     * �ɼ�ģ��������ʱ��
     */
    private static Long startTime = System.currentTimeMillis();
    public static Long getStartTime() {
        return startTime;
    }
    
    /**
     * ��ȡʱ��� 
     * @param now ��ǰʱ��
     * @return ���ز�ֵ
     */
    public static Long timestamp(Long now) {
        return System.currentTimeMillis() - now;
    }
    
    /**
     * �����»Ự
     */
    private static void createSession() {
        sessionSeq++;
        buildSessionId();
        startTime = System.currentTimeMillis();
        fire("createSession", "sessionId=", sessionId);
    }
    
    /**
     * ��ȡʱ��� 
     * @return ���ز�ֵ
     */
    public static Long timestamp() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * �Ựid, ��ǰʱ�����36����+�����
     */
    private static String sessionId;
    public static String getSessionId() {
        return sessionId;
    }
    
    /**
     * ��ǰ�ڼ��λỰ
     */
    private static Integer sessionSeq = 0;
    public static Integer getSessionSeq() {
        return sessionSeq;
    }
    
    /**
     * �̶�����������ڼ��������
     */
    private static Double randomSeed = Math.random();

    /**
     * �����ַ����������磺"wenku.set" -> ["wenku", "set"] "set" -> [null, "set"]
     */
    private static Pattern cmdPattern = Pattern.compile("^(?:([\\w$_]+)\\.)?(\\w+)$");

    /**
     * �¼��ַ����������磺"onCreate" -> ["Create"]
     */
    private static Pattern eventPattern = Pattern.compile("^on([A-Z]\\w*)$");
    /**
     * ����������ת�����ֵ䣬Ϊ�˼򻯵��÷�ʽ
     * @param params �����б�
     * @param offset ��ʼλ��
     * @return ����key-value����
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> buildMapOffset(Object[] params, Integer offset) {
        Map<String, Object> result = new HashMap<String, Object>();
        if (params.length - 1 == offset && offset >= 0) {
            if (params[offset] instanceof Map) {
                result.putAll((Map<String, Object>)params[offset]);
            }
            return result;
        }
        for (Integer i = offset; i + 1 < params.length; i += 2) {
            String key = (String)params[i];
            key = key.replaceFirst("[:=]$", ""); // "a=", 3, "b:", 4 -> "a", 3, "b", 4
            Object value = params[i + 1];
            result.put(key, value);
        }
        return result;
    }
    
    /**
     * ����������ת�����ֵ䣬Ϊ�˼򻯵��÷�ʽ
     * @param params �����б�
     * @return ����key-value����
     */
    public static Map<String, Object> buildMap(Object... params) {
        return buildMapOffset(params, 0);
    }
    
    /**
     * �ϲ����map
     * @param maps ���Map
     * @return ���غϲ����map
     */
    public static Map<String, Object> mergeMap(Map<String, Object>... maps) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Map<String, Object> map : maps) {
            result.putAll(map);
        }
        return result;
    }
    private static Map<String, NTracker> trackers = new HashMap<String, NTracker>();
    /**
     * ��ȡ׷����
     * @param name ׷��������
     */
    private static NTracker getTracker(String name) {
        if (name == null) {
            name = "default";
        }
        NTracker result = trackers.get(name);
        if (result == null) {
            result = new NTracker(name);
            trackers.put(name, result);
        }
        return result;
    }
    
    /**
     * �����µ�sessionId
     */
    private static void buildSessionId() {
        sessionId = Long.toString(System.currentTimeMillis(), 36) + 
                Long.toString((long)(36 * 36 * 36 * 36 * Math.random()), 36);
    }
        
    private static class CmdParamItem {
        public NTracker tracker;
        public String method;
        public Object[] params;
        CmdParamItem(NTracker tracker, String method, Object[] params) {
            this.tracker = tracker;
            this.method = method;
            this.params = params;
        }
    }
    private static ArrayList<CmdParamItem> cmdParamList = new ArrayList<CmdParamItem>();
    /**
     * ִ������
     * @param cmd ���"<׷������>.<������>"
     * @param params �����б�
     * @return ����get������
     */
    public static Object cmd(String cmd, Object... params) {
        /* debug start */
        Log.d(LOGTAG, String.format("command('%s', [length:%s])", cmd, params.length));
        /* debug end */

        // �ֽ� "name.method" Ϊ ["name", "method"]
        Matcher matcher = cmdPattern.matcher(cmd);

        if (!matcher.find()) {
            /* debug start */
            Log.w(LOGTAG, String.format("'%s' Command format error.", cmd));
            /* debug end */
            return null;
        }

        String trackerName = matcher.group(1);
        String method = matcher.group(2);
        NTracker tracker = getTracker(trackerName);
        if (initCompleted) {
            return tracker.command(method, params); 
        } else {
            cmdParamList.add(new CmdParamItem(tracker, method, params));
            return null;
        }
    }

    /**
     * ����������
     */
    private static Map<String, ArrayList<EventListener>> listeners = new HashMap<String, ArrayList<EventListener>>();
    
    /**
     * ���¼�
     * @param eventName �¼���
     * @param callback �ص�������
     */
    public static void on(String eventName, EventListener callback) {
        /* debug start */
        Log.d(LOGTAG, String.format("on('%s', %s)", eventName, callback));
        /* debug end */

        ArrayList<EventListener> list = listeners.get(eventName);
        if (list == null) {
            list = new ArrayList<EventListener>();
            listeners.put(eventName, list);
        }
        list.add(list.size(), callback); // ������
    }
    
    /**
     * ע���¼���
     * @param eventName �¼���
     * @param callback �ص�������
     */
    public static void un(String eventName, EventListener callback) {
        /* debug start */
        Log.d(LOGTAG, String.format("un('%s', %s)", eventName, callback));
        /* debug end */

        ArrayList<EventListener> list = listeners.get(eventName);
        if (list != null) {
            list.remove(callback);
        }
    }

    /**
     * �ɷ��¼�
     * @param eventName �¼���
     * @param params �����б�
     */
    public static void fire(String eventName, Object... params) {
        /* debug start */
        Log.d(LOGTAG, String.format("fire('%s', [length:%s])", eventName, params.length));
        /* debug end */
        fire(eventName, buildMap(params));
    }

    /**
     * �ɷ��¼�
     * @param eventName �¼���
     * @param map �����б�
     */
    public static void fire(String eventName, Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("fire('%s', %s)", eventName, map));
        /* debug end */

        ArrayList<EventListener> list = listeners.get(eventName);
        if (list == null) {
            return;
        }
        for (EventListener callback : list) {
            callback.onHandler(map);
        }
    }
    
    /**
     * �¼�������
     */
    public static abstract class EventListener {
        /**
         * �����¼�
         * @param params �����б�
         */
        public abstract void onHandler(Map<String, Object> map);
    }
    /**
     * �û������˳��
     */
    private static ArrayList<Context> followPath = new ArrayList<Context>();
    
    /**
     * Activity�������ڷ����ı� ��Ҫ��ÿ��Activity��onResume()��onPause()�����е��ã�����session�仯
     */
    public static void follow(Context context) {
        String methodName = null;
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            String name = element.getMethodName();
            if ("".equals(name.replaceFirst("^(onCreate|onStart|onResume|onPause|onStop|onDestroy|onRestart)$", ""))) {
                methodName = element.getMethodName();
                break;
            }
        }
        
        /* debug start */
        Log.d(LOGTAG, String.format("follow(%s) methodName => %s", context, methodName));
        /* debug end */
        if (methodName == null) {
            Log.w(LOGTAG, String.format("follow() Not in the right place."));
            return;
        }
       
        if ("onResume".equals(methodName)) { // ���¼���
            
            if (System.currentTimeMillis() - pauseTime > (Integer)fields.get("sessionTimeout") * 1000) { // session��ʱ
                pauseTime = System.currentTimeMillis();
                createSession();
            }
            
            if (followPath.contains(context)) {
                Log.w(LOGTAG, String.format("follow('%s') Does not match the context onPause and onResume. context=%s", methodName, context));
            } else {
                followPath.add(context);
            }
            
        } else if ("onPause".equals(methodName)) { 
            
            pauseTime = System.currentTimeMillis();
            if (followPath.contains(context)) {
                followPath.remove(context);
            } else {
                Log.w(LOGTAG, String.format("follow('%s') Does not match the context onPause and onResume. context=%s", methodName, context));
            }
            
        }
        
        fire("follow", buildMap(
                "method", methodName,
                "path=", followPath
        ));
    }
    
    /**
     * ���һ����ͣ��ʱ��
     */
    private static Long pauseTime = 0L;
    
    /**
     * ���post�����ݣ���ֵ����url����
     * @param map ��������
     * @return ����url�����ַ���
     */
    public static String buildPost(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        for (String key : map.keySet()) {
            try {
                Object value = map.get(key);
                if (value == null) {
                    continue;
                }
                sb.append(String.format("&%s=%s", key, URLEncoder.encode(value.toString(), "utf-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (sb.length() > 0) sb.deleteCharAt(0);
        return sb.toString();
    }
    /**
     * �ϱ�����
     * @param trackerName ׷��������
     * @param fields �����ֶ�
     * @param data �ϱ�����
     */
    public static void report(String trackerName, Map<String, Object> fields, Map<String, Object> data) {
        /* debug start */
        Log.d(LOGTAG, String.format("report(%s, %s)", fields, data));
        /* debug end */
        if (!initCompleted) {
            return;
        }
        if (!isSampled(trackerName)) {
            /* debug start */
            Log.i(LOGTAG, String.format("Tracker '%s' Not sample.", trackerName));
            /* debug end */
            return;
        }
        fire("report", buildMap("name=", trackerName, "fields=", fields, "data=", data));
        NStorage.report(trackerName, fields, data);
    }
    
    /**
     * �ж��Ƿ�׷�����Ƿ񱻳���
     * @param trackerName 
     * @return �Ƿ񱻳���
     */
    public static Boolean isSampled(String trackerName) {
        Boolean result = true;
        Double trackerSampleRate = sampleRate.get(trackerName);
        if (trackerSampleRate != null && trackerSampleRate < randomSeed) {
            result = false;
        }
        return result;
    }
    
    /**
     * ������
     */
    public static Map<String, Double> sampleRate = new HashMap<String, Double>();
    /**
     * ���¹���
     * @param jsonText
     */
    public static void updateRule(String jsonText) {
        /* debug start */
        Log.d(LOGTAG, String.format("updateRule(%s)", jsonText));
        /* debug end */
        try {
            JSONObject json = new JSONObject(jsonText);

            // ����ֵ����������Χ
            for (String key : configFields.keySet()) {
                ConfigField configField = (ConfigField)configFields.get(key); 
                if (json.has(key)) {
                    // ����ֵ����������Χ
                    fields.put(key, Math.min(
                            Math.max(safeInteger(json.get(key), configField.defaultValue), configField.minValue),
                            configField.maxValue
                    ));
                }
            }

            if (json.has("sampleRate")) {
                JSONObject items = json.getJSONObject("sampleRate");
                @SuppressWarnings("unchecked")
                Iterator<String> keys = items.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    sampleRate.put(key, Math.max(Math.min(1, safeDouble(items.get(key), 1.0)), 0));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}