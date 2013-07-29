package com.baidu.nlog;

import java.util.*;

import android.util.Log;

public final class NTracker {
    /**
     * nlog
     * @description Nativeͳ�ƿ�ܣ�׷����ʵ��
     * @author ������(WangJihu,http://weibo.com/zswang),����ɽ(PengZhengshan)
     * @see https://github.com/uxrp/nlog/wiki/design
     * @version 1.0
     * @copyright www.baidu.com
     */
    /**
     *  ��־TAG
     */
    private static String LOGTAG = "NTracker";

    /**
     * ׷�������ϣ���nameΪ�±�
     */
    private Map<String, Object> fields = new HashMap<String, Object>();
    
    /**
     * ���������
     */
    private class Args {
        /**
         * ������
         */
        public String method;
        /**
         * ����
         */
        public Object[] params;
        public Args(String method, Object[] params) {
            this.method = method;
            this.params = params;
        }
    }
    
    /**
     * ����������棬��׷����û��������ʱ��
     */
    private ArrayList<Args> argsList = new ArrayList<Args>();
    
    /**
     * �Ƿ��������� 
     */
    private Boolean running = false;
    public Boolean getRunning() {
        return running;
    }
    public void setRunning(Boolean value) {
        if (value) {
            start();
        } else {
            stop();
        }
    }
    
    /**
     * ��ʼ�ɼ�
     * @param params ��ʼ����
     */
    public void start(Object... params) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.start([length:%s])", this, params.length));
        /* debug end */
        
        start(NLog.buildMap(params));
    }
    
    /**
     * ��ʼ�ɼ�
     * @param map ��ʼ������key-value��ʽ
     */
    public void start(Map<String, Object> map) {
        if (running) {
            return;
        }
        running = true;
        
        /* debug start */
        Log.d(LOGTAG, String.format("%s.start(%s)", this, map));
        /* debug end */

        set(map);
        // ����֮ǰ�Ĳ���
        for (Args args : argsList) {
            command(args.method, args.params);
        }
        argsList.clear();
        fire("start");
    }

    /**
     * ֹͣ�ɼ�
     */
    public void stop() {
        if (!running) return;
        running = true;
        
        /* debug start */
        Log.d(LOGTAG, String.format("%s.stop()", this));
        /* debug end */
        
        fire("stop");
    }
    
    /**
     * ׷��������
     */
    private String name;
    public String getName() {
        return name;
    }
    
    /**
     * �̶��������ֶ�
     */
    private static Map<String, Object> configFields = NLog.buildMap(
        "postUrl=", null, // �ϱ�·��
        "protocolParameter=", null // �ֶ���д�ֵ�
    );

    /**
     * �����ֶ�ֵ
     * @param map �������ϣ�key-value��ʽ
     */
    @SuppressWarnings("unchecked")
    public void set(Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.set(%s)", this, map));
        /* debug end */
        
        Iterator<String> iterator = map.keySet().iterator();    
        while (iterator.hasNext()) {    
            String key = iterator.next();
            
            Object value = map.get(key);
            if ("protocolParameter".equals(key)) {
                if (!(value instanceof Map)) continue;
                value = NLog.mergeMap(
                    configFields, 
                    (Map<String, Object>)value
                );
            }
            fields.put(key, value);
        }
    }

    /**
     * �����ֶ�ֵ
     * @param params ��������
     */
    public void set(Object... params) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.set(%s)", this, params));
        /* debug end */
        set(NLog.buildMap(params));
    }
    
    /**
     * ��ȡ�ֶ�ֵ
     * @param key ��ֵ��
     * @return ���ؼ�ֵ��Ӧ������
     */
    public Object get(String key) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.get('%s') => %s", this, key, fields.get(key)));
        /* debug end */
        
        return fields.get(key);
    }
    
    /**
     * ���캯��
     * @param name ׷��������
     * @param nlog NLog����
     */
    public NTracker(String name) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s::NTracker('%s')", this, name));
        /* debug end */

        this.name = name;
        fields.put("protocolParameter", configFields);
    }
    
    /**
     * ��������
     * @param hitType �������ͣ�appview��event��timing��exception
     * @param map ��������
     */
    public void send(String hitType, Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.send('%s', %s)", this, hitType, map));
        /* debug end */
        
        @SuppressWarnings("unchecked")
        Map<String, Object> data = NLog.mergeMap(
            NLog.buildMap(
                "sid=", NLog.getSessionId(), // �Ựid
                "seq=", NLog.getSessionSeq(), // �Ự˳��
                "time=", System.currentTimeMillis(), // �¼�������ʱ��
                "ts=", Long.toString(NLog.timestamp(), 36), // 36���Ƶ�ʱ���
                "ht=", hitType
            ), map);
        fire("send", data);
        NLog.report(name, fields, data);
    }

    /**
     * ��������
     * @param hitType �������ͣ�appview��event��timing��exception
     * @param map ��������
     */
    public void send(String hitType, Object... params) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.send('%s', %s)", this, hitType, params));
        /* debug end */
        
        send(hitType, NLog.buildMap(params));
    }
    
    /**
     * ����appview
     * @param map ��������
     */
    public void sendView(Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendView(%s)", this, map));
        /* debug end */
        
        send("appview", map);
    }
    
    /**
     * ����appview
     * @param appScreen ��Ļ��������
     */
    public void sendView(String appScreen) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendView('%s')", this, appScreen));
        /* debug end */
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("appScreen", appScreen);
        send("appview", map);
    }
        
    /**
     * �����¼�
     * @param map ��������
     */
    public void sendEvent(Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendEvent('%s')", this, map));
        /* debug end */
        
        send("event", map);
    }
    
    /**
     * �����¼�
     * @param category �¼����࣬�磺button
     * @param action �������磺click
     * @param label ��ǩ��e.g��save
     * @param value ִ�д���
     */
    public void sendEvent(String category, String action, String label, Long value) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendEvent('%s', '%s', '%s', %s)", this, category, action, label, value));
        /* debug end */
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("eventCategory", category);
        map.put("eventAction", action);
        map.put("eventLabel", label);
        map.put("eventValue", value);
        send("event", map);
    }

    /**
     * �����쳣
     * @param map ��������
     */
    public void sendException(Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendException('%s')", this, map));
        /* debug end */
        
        send("exception", map);
    }
    
    /**
     * �����쳣
     * @param description �쳣����
     * @param fatal �Ƿ��±���
     */
    public void sendException(String description, Boolean fatal) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendException('%s', %s)", this, description, fatal));
        /* debug end */
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("exDescription", description);
        map.put("exFatal", fatal);
        
        send("exception", map);
    }
    
    
    /**
     * �����쳣
     * @param threadName �߳���
     * @param description �쳣����
     * @param fatal �Ƿ��±���
     */
    public void sendException(String threadName, String description, Boolean fatal) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendException('%s', '%s', %s)", this, threadName, description, fatal));
        /* debug end */
        
        send("exception", NLog.buildMap(
            "exThread=", threadName,
            "exDescription=", description,
             "exFatal=", fatal
        ));
    }

    /**
     * ����ʱ��ͳ��
     * @param map ��������
     */
    public void sendTiming(Map<String, Object> map) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendTiming('%s')", this, map));
        /* debug end */
        
        send("timing", map);
    }
    
    /**
     * ����ʱ��ͳ��
     * @param category ���
     * @param intervalInMilliseconds ��ʱ
     * @param name ����
     * @param label ��ǩ
     */
    public void sendTiming(String category, String var, Long value, String label) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.sendTiming('%s')", this, category, var, value, label));
        /* debug end */
        
        send("timing", NLog.buildMap(
            "timingCategory=", category,
            "timingVar=", var,
            "timingValue=", value,
            "timingLabel=", label
        ));
    }
    
    /**
     * ���¼�
     * @param eventName �¼���
     * @param callback �ص�������
     */
    public void on(String eventName, NLog.EventListener callback) {
        NLog.on(name + "." + eventName, callback);
    }
    
    /**
     * ע���¼���
     * @param eventName �¼���
     * @param callback �ص�������
     */
    public void un(String eventName, NLog.EventListener callback) {
        NLog.un(name + "." + eventName, callback);
    }
    
    /**
     * �ɷ��¼�
     * @param eventName �¼���
     * @param params �����б�
     */
    public void fire(String eventName, Object... params) {
        NLog.fire(name + "." + eventName, params);
    }
    
    /**
     * �ɷ��¼�
     * @param eventName �¼���
     * @param map �����б�
     */
    public void fire(String eventName, Map<String, Object> map) {
        NLog.fire(name + "." + eventName, map);
    }
    
    /**
     * ִ������
     * @param method ������ set��get��send��start��stop
     * @param params
     * @return ��������ִ�еĽ������Ҫ����get����
     */
    public Object command(String method, Object... params) {
        /* debug start */
        Log.d(LOGTAG, String.format("%s.command('%s', [length:%s])", this, method, params.length));
        /* debug end */
        
        if (!running && "".equals(method.replaceAll("^(fire|send)$", ""))) {
            argsList.add(new Args(method, params));
            return null;
        }
        
        if (method.equals("set")) {
            set(NLog.buildMap(params));
        } else if (method.equals("get")) {
            return get((String)params[0]);
        } else if (method.equals("send")) {
            if (params.length >= 1) { // send�����������hitType
                String hitType = (String)params[0];
                send(hitType, NLog.buildMapOffset(params, 1));
            }
        } else if (method.equals("start")) {
            start(NLog.buildMap(params));
        } else if (method.equals("stop")) {
            stop();
        } else if (method.equals("on") || method.equals("un")) {
            if (params.length >= 2 && params[1] instanceof NLog.EventListener) {
                String eventName = (String)params[0]; 
                NLog.EventListener callback = (NLog.EventListener)params[1]; 
                if (method.equals("on")) {
                    on(eventName, callback);
                } else {
                    un(eventName, callback);
                }
            }
        } else if (method.equals("fire")) {
            if (params.length >= 1) {
                String eventName = (String)params[0];
                fire(eventName, NLog.buildMapOffset(params, 1));
            }
        }
        
        return null;
    }
}