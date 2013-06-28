package com.baidu.nlog;

import java.util.*;

import android.content.Context;

public final class NTracker {
    /**
     * nlog
     * @description Nativeͳ�ƿ�ܣ�׷����ʵ��
     * @author ������(WangJihu,http://weibo.com/zswang),����ɽ(PengZhengshan)
     * @version 1.0
     * @copyright www.baidu.com
     */

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
        System.out.println(String.format("%s.start([length:%s])", this, params.length));
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
        System.out.println(String.format("%s.start(%s)", this, map));
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
        System.out.println(String.format("%s.stop()", this));
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
     * NLog����
     */
    private NLog nlog;
    public NLog getNLog() {
        return nlog;
    }
    
    /**
     * NLog����
     */
    private Context context;
    public Context getContext() {
        return context;
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
        System.out.println(String.format("%s.set(%s)", this, map));
        /* debug end */
        
        Iterator<String> iterator = map.keySet().iterator();    
        while (iterator.hasNext()) {    
            String key = iterator.next();
            
            Object value = map.get(key);
            if ("protocolParameter".equals(key)) {
            	if (!(value instanceof Map)) continue;
                value = NLog.merge(
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
        System.out.println(String.format("%s.set(%s)", this, params));
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
        System.out.println(String.format("%s.get('%s') => %s", this, key, fields.get(key)));
        /* debug end */
        
        return fields.get(key);
    }
    
    /**
     * ���캯��
     * @param name ׷��������
     * @param nlog NLog����
     */
    public NTracker(String name, NLog nlog) {
        /* debug start */
        System.out.println(String.format("%s::NTracker('%s', %s)", this, name, nlog));
        /* debug end */

        this.name = name;
        this.nlog = nlog;
        this.context = nlog.getContext();
        fields.put("protocolParameter", configFields);
    }
    
    /**
     * ��������
     * @param hitType �������ͣ�appview��event��timing��exception
     * @param map ��������
     */
    public void send(String hitType, Map<String, Object> map) {
        /* debug start */
        System.out.println(String.format("%s.send('%s', %s)", this, hitType, map));
        /* debug end */
        
        Map<String, Object> data = NLog.merge(
    		NLog.buildMap(
    		    "sid=", nlog.getSessionId(), // �Ựid
        		"time=", System.currentTimeMillis(), // �¼�������ʱ��
        		"ts=", Long.toString(nlog.timestamp(), 36), // 36���Ƶ�ʱ���
        		"ht=", hitType
    		), map);
        fire("send", data);
        nlog.report(name, fields, data);
    }

    /**
     * ��������
     * @param hitType �������ͣ�appview��event��timing��exception
     * @param map ��������
     */
    public void send(String hitType, Object... params) {
        /* debug start */
        System.out.println(String.format("%s.send('%s', %s)", this, hitType, params));
        /* debug end */
        
        send(hitType, NLog.buildMap(params));
    }
    
    /**
     * ����appview
     * @param map ��������
     */
    public void sendView(Map<String, Object> map) {
        /* debug start */
        System.out.println(String.format("%s.sendView(%s)", this, map));
        /* debug end */
        
        send("appview", map);
    }
    
    /**
     * ����appview
     * @param appScreen ��Ļ��������
     */
    public void sendView(String appScreen) {
        /* debug start */
        System.out.println(String.format("%s.sendView('%s')", this, appScreen));
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
        System.out.println(String.format("%s.sendEvent('%s')", this, map));
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
        System.out.println(String.format("%s.sendEvent('%s', '%s', '%s', %s)", this, category, action, label, value));
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
        System.out.println(String.format("%s.sendException('%s')", this, map));
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
        System.out.println(String.format("%s.sendException('%s', %s)", this, description, fatal));
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
        System.out.println(String.format("%s.sendException('%s', '%s', %s)", this, threadName, description, fatal));
        /* debug end */
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("exThread", threadName);
        map.put("exDescription", description);
        map.put("exFatal", fatal);
        
        send("exception", map);
    }

    /**
     * ����ʱ��ͳ��
     * @param map ��������
     */
    public void sendTiming(Map<String, Object> map) {
        /* debug start */
        System.out.println(String.format("%s.sendTiming('%s')", this, map));
        /* debug end */
        
        send("timing", map);
    }
    
	/**
	 * ���¼�
	 * @param eventName �¼���
	 * @param callback �ص�������
	 */
    public void on(String eventName, NLog.EventListener callback) {
    	nlog.on(name + "." + eventName, callback);
    }
    
	/**
	 * ע���¼���
	 * @param eventName �¼���
	 * @param callback �ص�������
	 */
    public void un(String eventName, NLog.EventListener callback) {
    	nlog.un(name + "." + eventName, callback);
    }
    
	/**
	 * �ɷ��¼�
	 * @param eventName �¼���
	 * @param params �����б�
	 */
    public void fire(String eventName, Object... params) {
    	nlog.fire(name + "." + eventName, params);
    }
    
	/**
	 * �ɷ��¼�
	 * @param eventName �¼���
	 * @param map �����б�
	 */
	public void fire(String eventName, Map<String, Object> map) {
    	nlog.fire(name + "." + eventName, map);
    }
    
    /**
     * ִ������
     * @param method ������ set��get��send��start��stop
     * @param params
     * @return ��������ִ�еĽ������Ҫ����get����
     */
    public Object command(String method, Object... params) {
        /* debug start */
        System.out.println(String.format("%s.command('%s', [length:%s])", this, method, params.length));
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
                send(hitType, NLog.buildMap(params, 1));
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
        		fire(eventName, NLog.buildMap(params, 1));
        	}
    	}
        
        return null;
    }
}