package com.baidu.nlog;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import android.content.Context;

public final class NLog {
    /**
     * nlog
     * @description Nativeͳ�ƿ��
     * @author ������(WangJihu,http://weibo.com/zswang),����ɽ(PengZhengshan)
     * @version 1.0
     * @copyright www.baidu.com
     */
	
	/**
	 * �豸������
	 */
	private Context context;
	/**
	 * ��ȡ�豸������
	 */
	public Context getContext() {
		return context;
	}
	
	/**
	 * �ɼ�ģ��������ʱ��
	 */
	private Long startTime = System.currentTimeMillis();
	public Long getStartTime() {
		return startTime;
	}
	
	/**
	 * ��ȡʱ��� 
	 * @param now ��ǰʱ��
	 * @return ���ز�ֵ
	 */
	public Long timestamp(Long now) {
		return System.currentTimeMillis() - now;
	}
	
	/**
	 * session��ʱʱ�䣬��λ����
	 */
	private Integer sessionTimeout = 30;
	
	/**
	 * ��ȡsession��ʱʱ��
	 * @param context ������
	 * @return ����session��ʱʱ��
	 */
	public static Integer getSessionTimeout(Context context) {
		NLog instance = getInstance(context);
		return instance.sessionTimeout;
	}
	/**
	 * ��ȡsession��ʱʱ��
	 * @return ����session��ʱʱ�䣬��λ����
	 */
	public Integer getSessionTimeout() {
		return sessionTimeout;
	}

	/**
	 * ����session��ʱʱ��
	 * @param context ������
	 * @param value ��ʱʱ�䣬��λ���룬Ĭ��30
	 */
	public static void setSessionTimeout(Context context, Integer value) {
		NLog instance = getInstance(context);
		instance.setSessionTimeout(value);
	}
	/**
	 * ����session��ʱʱ��
	 */
	public void setSessionTimeout(Integer value) {
		if (sessionTimeout == value) {
			return;
		}
		sessionTimeout = value;
		fire("sessionTimeoutChange", "value=", value);
	}
	
	/**
	 * �����»Ự
	 */
	private void createSession() {
		buildSessionId();
		startTime = System.currentTimeMillis();
		fire("createSession", "sessionId=", sessionId);
	}
	
	/**
	 * ��ȡʱ��� 
	 * @return ���ز�ֵ
	 */
	public Long timestamp() {
		return System.currentTimeMillis() - startTime;
	}
	
	/**
	 * ��ȡʱ��� 
	 * @param context ������
	 * @param now ��ǰʱ��
	 * @return ���ز�ֵ
	 */
	public static Long timestamp(Context context, Long now) {
		NLog instance = getInstance(context);
		return instance.timestamp(now);
	}
	
	/**
	 * �Ựid, ��ǰʱ�����36����+�����
	 */
	private String sessionId;
	public String getSessionId() {
		return sessionId;
	}
	
	/**
	 * ��ȡʱ���
	 * @param context ������ 
	 * @return ���ز�ֵ
	 */
	public static Long timestamp(Context context) {
		NLog instance = getInstance(context);
		return instance.timestamp();
	}

	/**
	 * �Ƿ���debug״̬
	 */
	private Boolean debug = false;
	public void setDebug(Boolean debug) {
		this.debug = debug;
	}
	public static void setDebug(Context context, Boolean debug) {
		NLog instance = getInstance(context);
		instance.setDebug(debug);
	}
	public Boolean getDebug() {
		return debug;
	}
	public static Boolean getDebug(Context context) {
		NLog instance = getInstance(context);
		return instance.getDebug();
	}
	
	/**
	 * �̶�����������ڼ��������
	 */
	private static Double randomSeed = Math.random();
	public static Double getRandomSeed() {
		return randomSeed;
	}

	/**
	 * �����ַ����������磺"wenku.set" -> ["wenku", "set"] "set" -> [null, "set"]
	 */
	private static Pattern cmdPattern = Pattern.compile("^(?:([\\w$_]+)\\.)?(\\w+)$");

	/**
	 * ʵ�����ϣ���contextΪ�±�
	 */
	private static Map<Context, NLog> instances = new HashMap<Context, NLog>();

    /**
     * ����������ת�����ֵ䣬Ϊ�˼򻯵��÷�ʽ
     * @param params �����б�
     * @param offset ��ʼλ��
     * @return ����key-value����
     */
    public static Map<String, Object> buildMap(Object[] params, Integer offset) {
        Map<String, Object> result = new HashMap<String, Object>();
        for (Integer i = offset; i + 1 < params.length; i += 2) {
            String key = (String)params[i];
            key = key.replaceFirst("[:=]$", ""); // "a=", 3, "b:", 4 -> "a", 3, "b", 4
            Object value = params[i + 1];
            result.put(key, value);
        }
        return result;
    }
    
    /**
     * �ϲ�����map
     * @param a map1
     * @param b map2
     * @return ���غϲ����map
     */
    public static Map<String, Object> merge(Map<String, Object> a, Map<String, Object> b) {
    	Map<String, Object> result = new HashMap<String, Object>();
    	result.putAll(a);
    	result.putAll(b);
    	return result;
    }

	/**
	 * ����������ת�����ֵ�
	 * @param params �����б�
	 * @return ����key-value����
	 */
    public static Map<String, Object> buildMap(Object... params) {
	    return buildMap(params, 0);
	}

    /**
	 * ��ȡNLogʵ��
	 * @param context �豸������
	 */
	public static NLog getInstance(Context context) {
		// ֻ�ܴ���Application������android����ֻ����һ��applicationʵ��
		Context app = context.getApplicationContext();
		NLog result = instances.get(app);
		if (result == null) {
			result = new NLog(app);
			instances.put(app, result);
		}
		
		/**
		NLog result = instances.get(null);
		if (result == null) {
			result = new NLog(context);
			instances.put(null, result);
		} else {
			result.context = context;
		}
		*/
		
		/* debug start */
        System.out.println(String.format("NLog.getInstance(%s) => %s", context, result));
		/* debug end */
		return result;
	}
	
	/**
	 * ׷�������ϣ���nameΪ�±�
	 */
	private Map<String, NTracker> trackers = new HashMap<String, NTracker>();
	
	/**
	 * ��ȡ׷����
	 * @param name ׷��������
	 */
	public NTracker getTracker(String name) {
		if (name == null) {
			name = "default";
		}
		NTracker result = trackers.get(name);
		if (result == null) {
			result = new NTracker(name, this);
			trackers.put(name, result);
		}
		return result;
	}
	
	/**
	 * ��ȡ׷����
	 * @param context ������
	 * @param name ׷��������
	 */
	public static NTracker getTracker(Context context, String name) {
		NLog instance = getInstance(context);
		return instance.getTracker(name);
	}
	
	/**
	 * �����µ�sessionId
	 */
	private void buildSessionId() {
		sessionId = Long.toString(System.currentTimeMillis(), 36) + 
				Long.toString((long)(36 * 36 * 36 * 36 * Math.random()), 36);
	}
	
    /**
	 * ���캯��
	 * @param context ׷��������
	 */
	private NLog(Context context) {
		this.context = context;
		this.nstorage = new NStorage(context);
		buildSessionId();
	}
	
	/**
	 * �Ƿ�ֻ��wifi����������ϱ�����
	 */
	public void setOnlywifi(Boolean value) {
		nstorage.setOnlywifi(value);
	}
	public static void setOnlywifi(Context context, Boolean value) {
		NLog instance = getInstance(context);
		instance.setOnlywifi(value);
	}
	
	/**
	 * �ط����ݵ�ʱ����
	 */
	public void setSendInterval(Integer value) {
		nstorage.setSendInterval(value);
		fire("sendInterval", value);
	}
	public static void setSendInterval(Context context, Integer value) {
		NLog instance = getInstance(context);
		instance.setSendInterval(value);
	}
	
	
	/**
	 * ִ������
	 * @param cmd ���"<׷������>.<������>"
	 * @param params �����б�
	 * @return ����get������
	 */
	public Object cmd(String cmd, Object... params) {
		/* debug start */
        System.out.println(String.format("%s.command('%s', [length:%s])", this, cmd, params.length));
		/* debug end */

        // �ֽ� "name.method" Ϊ ["name", "method"]
		Matcher matcher = cmdPattern.matcher(cmd);

		if (!matcher.find()) {
			/* TODO : ��¼�쳣 */
			return null;
		}

		String trackerName = matcher.group(1);
		String method = matcher.group(2);
		NTracker tracker = getTracker(trackerName);
		return tracker.command(method, params);
	}
	
	/**
	 * ִ������
	 * @param context �豸������
	 * @param cmd ���"<׷������>.<������>"
	 * @param params �����б�
	 * @return ����get������
	 */
	public static Object cmd(Context context, String cmd, Object... params) {
		NLog instance = getInstance(context);
		return instance.cmd(cmd, params);
	}
	
	/**
	 * ����������
	 */
	private Map<String, ArrayList<EventListener>> listeners = new HashMap<String, ArrayList<EventListener>>();
	
	/**
	 * ���¼�
	 * @param eventName �¼���
	 * @param callback �ص�������
	 */
	public void on(String eventName, EventListener callback) {
		/* debug start */
        System.out.println(String.format("%s.on('%s', %s)", this, eventName, callback));
		/* debug end */

        ArrayList<EventListener> list = listeners.get(eventName);
		if (list == null) {
			list = new ArrayList<EventListener>();
			listeners.put(eventName, list);
		}
		list.add(list.size(), callback); // ������
	}
	
	/**
	 * ���¼�
	 * @param context ������
	 * @param eventName �¼���
	 * @param callback �ص�������
	 */
	public static void on(Context context, String eventName, EventListener callback) {
		NLog instance = getInstance(context);
		instance.on(eventName, callback);
	} 
	
	/**
	 * ע���¼���
	 * @param eventName �¼���
	 * @param callback �ص�������
	 */
	public void un(String eventName, EventListener callback) {
		/* debug start */
        System.out.println(String.format("%s.un('%s', %s)", this, eventName, callback));
		/* debug end */

        ArrayList<EventListener> list = listeners.get(eventName);
		if (list != null) {
			list.remove(callback);
		}
	}

	/**
	 * ע���¼���
	 * @param context ������
	 * @param eventName �¼���
	 * @param callback �ص�������
	 */
	public static void un(Context context, String eventName, EventListener callback) {
		NLog instance = getInstance(context);
		instance.un(eventName, callback);
	} 

	/**
	 * �ɷ��¼�
	 * @param eventName �¼���
	 * @param params �����б�
	 */
	public void fire(String eventName, Object... params) {
		/* debug start */
        System.out.println(String.format("%s.fire('%s', [length:%s])", this, eventName, params.length));
		/* debug end */
        fire(eventName, buildMap(params));
	}
	
	/**
	 * �ɷ��¼�
	 * @param context ������
	 * @param eventName �¼���
	 * @param params �����б�
	 */
	public static void fire(Context context, String eventName, Object... params) {
		NLog instance = getInstance(context);
		instance.fire(eventName, params);
	}

	/**
	 * �ɷ��¼�
	 * @param eventName �¼���
	 * @param map �����б�
	 */
	public void fire(String eventName, Map<String, Object> map) {
		/* debug start */
        System.out.println(String.format("%s.fire('%s', %s)", this, eventName, map));
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
	 * Activity�������ڷ����ı�
	 * @param context
	 */
	public static void follow(Context context) {
		/* debug start */
        System.out.println(String.format("NLog.follow(%s)", context));
		/* debug end */

        NLog instance = getInstance(context);
		instance.follow();
	}
	
	/**
	 * Activity�������ڷ����ı� ��Ҫ��ÿ��Activity��onResume()��onPause()�����е��ã�����session�仯
	 */
	public void follow() {
		String methodName = null;
		for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
			String name = element.getMethodName();
			if ("".equals(name.replaceFirst("^(onCreate|onStart|onResume|onPause|onStop|onDestroy|onRestart)$", ""))) {
				methodName = element.getMethodName();
				break;
			}
		}
        
		/* debug start */
        System.out.println(String.format("%s.follow() methodName => %s", this, methodName));
		/* debug end */
		if (methodName == null) {
			return;
		}

		if ("onResume".equals(methodName)) {
			if (System.currentTimeMillis() - pauseTime > sessionTimeout * 1000) { // session��ʱ
				createSession();
			}
		} else if ("onPause".equals(methodName)) {
			pauseTime = System.currentTimeMillis();
		}
		
		fire(context, methodName);
	}
	
	/**
	 * ���һ����ͣ��ʱ��
	 */
	private Long pauseTime = 0L;
	
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
	 * �洢�ͷ�������
	 */
	private NStorage nstorage;
	
	/**
	 * �ϱ�����
	 * @param trackerName ׷��������
	 * @param fields �����ֶ�
	 * @param data �ϱ�����
	 */
    public void report(String trackerName, Map<String, Object> fields, Map<String, Object> data) {
		/* debug start */
        System.out.println(String.format("%s.report(%s, %s)", this, fields, data));
		/* debug end */
        
        fire("report", buildMap("name=", trackerName, "fields=", fields, "data=", data));
        nstorage.report(trackerName, fields, data);
    }
}