package com.baidu.nlog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.net.Proxy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.telephony.TelephonyManager;
import android.util.Log;

@SuppressLint("HandlerLeak")
public class NStorage {
	/**
	 * �豸id
	 */
	private String deviceId = "";
	
	/**
	 * �������ӳ�ʱ,��λ������
	 */
	private static final int connTimeout = 40 * 1000; // 40��
	/**
	 * 
	 */
	private static final int readTimeout = 60 * 1000; //60�� ��ȡ���ݳ�ʱ
	/*
	 * �洢�ļ��汾 // ���������ļ��汾�ı�
	 */
	public static final String fileVersion = "0";
	
	/**
	 * ���滺���ļ���Ŀ¼
	 */
	private static String rootDir = Environment.getExternalStorageDirectory() + File.separator + "_nlog_cache";
	
	/**
	 * �����ļ���ģ�� _nlog_[version]_[itemname].dat, itemname => [name].[md5(head)]
	 */
	private static final String cacheFileFormat = rootDir + File.separator + "_nlog_%s_%s.dat";
	/**
	 * ��෢�͵��ֽ���
	 */
	private static final int sendMaxBytes = 20000;
	/**
	 * ��־�����ʱ�䣬��λ����
	 */
	private static final int saveMaxDays = 7;

	/**
	 * �Ƿ�ֻ��wifi����������ϱ�����
	 */
	private Boolean onlywifi = false;
	public void setOnlywifi(Boolean value) {
		if (onlywifi.equals(value)) return;
		onlywifi = value;
	}
	
	/**
	 * �ط����ݵ�ʱ����
	 */
	private Integer sendInterval = 120; // �룬��������ʱ��
	public void setSendInterval(Integer value) {
		if (sendInterval.equals(value)) return;
		sendInterval = value;
		updateTimer();
	}
	
	/**
	 * ������
	 */
    private class CacheItem {
    	public StringBuffer sb;
    	public String name;
    	public String head;
    	public byte[] pass;
    	/**
    	 * ����
    	 * @param name ���� 
    	 * @param head ����
    	 * @param sb �ַ�����
    	 */
    	CacheItem(String name, String head) {
    		this.sb = new StringBuffer();
    		this.sb.append(head + '\n');
    		this.head = head;
    		this.name = name;

    		this.pass = buildPass(name);
    	}
    }
    
	/**
	 * ������
	 */
    private class PostItem {
    	public String name;
    	public byte[] pass;
    	public String locked;
    	/**
    	 * ����
    	 * @param name ����
    	 * @param locked �����ļ���
    	 */
    	PostItem(String name, String locked) {
    		this.name = name;
    		this.locked = locked;
    		this.pass = buildPass(name);
    	}
    }
    
    /**
     * �ļ���Կ������ʵ���������޸ĳ��Լ���
     */
    private String secretKey = "5D97EEF8-3127-4859-2222-82E6C8FABD8A";
    
    /**
     * ��Կ�����棬Ϊ�������ٶ�
     */
    private Map<String, byte[]> passMap = new HashMap<String, byte[]>(); 
    
    /**
     * ������Կ����������޸���Ҫ����fileVersion
     * @param name ����
     * @return ������Կ��
     */
    private byte[] buildPass(String name) {
    	byte[] result = passMap.get(name);
    	if (result != null) return result;
		try {
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(String.format("%s,%s,%s", deviceId, name, secretKey).getBytes());
			baos.write(md.digest());
			md.update(String.format("%s,%s,%s", name, deviceId, secretKey).getBytes());
			baos.write(md.digest());
			md.update(String.format("%s,%s,%s", deviceId, secretKey, name).getBytes());
			baos.write(md.digest());
	    	result = baos.toByteArray(); 
	    	baos.close();
	    	passMap.put(name, result);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
    }

    /**
	 * ������־
	 */
	private Map<String, CacheItem> cacheItems = new HashMap<String, CacheItem>();
	
    /**
     * �����ֶ�����д�������дΪnull�������
     * @param protocolParameter �ֶ����ֵ�
     * @param map ��������
     * @return ���ش������ֵ�
     */
	@SuppressWarnings("unchecked")
	private Map<String, Object> runProtocolParameter(Object protocolParameter, Map<String, Object> map) {
    	if (protocolParameter == null || (!(protocolParameter instanceof Map))) {
    		return map;
    	}
    	Map<String, Object> parameter = (HashMap<String, Object>)protocolParameter;
    	Map<String, Object> result = new HashMap<String, Object>();
    	for (String key : map.keySet()) {
    		if (parameter.containsKey(key)) { // ��Ҫת��
        		Object newKey = parameter.get(key);
        		if (newKey != null) { // Ϊnullʱ����
            		result.put((String)newKey, map.get(key));
        		}
    		} else {
    			result.put(key, map.get(key));
    		}
    	}
    	return result;
    }

	/**
	 * �ϱ�����
	 * @param trackerName ׷��������
	 * @param fields �����ֶ�
	 * @param data �ϱ�����
	 */
    public void report(String trackerName, Map<String, Object> fields, Map<String, Object> data) {
		/* debug start */
        System.out.println(String.format("%s.report(%s, %s) postUrl=%s", this, fields, data, fields.get("postUrl")));
		/* debug end */
        
        String postUrl = (String)fields.get("postUrl");
    	if (fields.get("postUrl") == null) {
    		// ���ͱ�ȡ��
    		return;
    	}
		Object parameter = fields.get("protocolParameter");
		// ת��͹���
        Map<String, Object> headMap = runProtocolParameter(parameter, fields);
        Map<String, Object> lineMap = runProtocolParameter(parameter, data);
        appendCache(trackerName, postUrl + '?' + NLog.buildPost(headMap), NLog.buildPost(lineMap));
    }
    
    /**
     * ������Ϣ
     */
    private Map<String, Message> messages = new HashMap<String, Message>();
    
    /**
     * �����ݷŵ�������
	 * @param trackerName ׷��������
     * @param head �������ݣ���������
     * @param line ÿ������
     */
    private void appendCache(String trackerName, String head, String line) {
		/* debug start */
        System.out.println(String.format("%s.appendCache('%s', '%s', '%s')", this, trackerName, head, line));
		/* debug end */

		synchronized(cacheItems) {
			String itemname = String.format("%s.%s", trackerName, getMD5(head));
			CacheItem item = cacheItems.get(itemname);
			if (item == null) {
				item = new CacheItem(itemname, head);
				cacheItems.put(itemname, item); // ���뻺��
			}
			synchronized(item.sb) {
				item.sb.append(line + '\n');
			}
			sendMessage_saveFile(item);
		}
    }
	/** 
     * �ж�Network�Ƿ����ӳɹ�(�����ƶ������wifi) 
     * @return �����Ƿ�����
     */
    public boolean isNetworkConnected(){ 
        return checkWifiConnected() || checkNetConnected(); 
    }
    
    /**
     * ����ƶ������Ƿ�����
     * @return �����Ƿ�����
     */
	public boolean checkNetConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager)context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager
				.getActiveNetworkInfo();
		if (networkInfo != null) {
			return networkInfo.isConnected();
		}
		return false;
	}
	
	/**
	 * ���wifi�Ƿ�����
	 * @return �����Ƿ�����
	 */
	public boolean checkWifiConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager)context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wiFiNetworkInfo = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wiFiNetworkInfo != null) {
			return wiFiNetworkInfo.isConnected();
		}
		return false;
	}
	
	/**
	 * ���ļ�����
	 * @param itemname ����
	 * @return ������������ļ���
	 */
	private String buildLocked(String itemname) {
		String filename = String.format(cacheFileFormat, fileVersion, itemname);
		File file = new File(filename);
		if (!file.exists()) return null;
		String result = filename.replaceFirst("\\.dat$", "." + Long.toString(System.currentTimeMillis(), 36) + ".locked");
    	File locked = new File(result);
		while (!file.renameTo(locked)) {
			result = filename.replaceFirst("\\.dat$", "." + Long.toString(System.currentTimeMillis(), 36) + ".locked");
	    	locked = new File(result);
		}
		return result;
	}

	/**
	 * �����ļ�
	 * @param item ������
	 * @param lockedname �����ļ���
	 * @return �Ƿ��ͳɹ�
	 */
	@SuppressLint("DefaultLocale")
	private Boolean postFile(PostItem item) {
		/* debug start */
        System.out.println(String.format("%s.postFile('%s', '%s')", this, item.name, item.locked));
		/* debug end */
        
        Boolean result = false;
        if (onlywifi && !checkWifiConnected()) {
        	Log.d("NLOG", String.format("%s.postFile() - Without a wifi connection. onlywifi = true", this));
        	return result;
        } else if (!isNetworkConnected()) {
        	Log.d("NLOG", String.format("%s.postFile() - Without a network connection.", this));
        	return result;
        }
        

        String filename = String.format(cacheFileFormat, fileVersion, item.name);
    	File file = new File(filename);
    	if (!file.exists() || file.length() <= 0) {
        	Log.d("NLOG", String.format("%s.postFile() - file '%s' not found.", this, filename));
    		return result;
    	}
        
        byte[] pass = item.pass;
        int len;
        int size = 1024;
        byte[] buf = new byte[size];
        String postUrl = null;
		try {
			FileInputStream fis;
			fis = new FileInputStream(filename);
	        len = fis.read(buf);
	        for (int i = 0; i < len; i++) {
				buf[i] = (byte)(buf[i] ^ i % 256 ^ pass[i % pass.length]); 
	        	if (buf[i] == '\n') {
	        		postUrl = new String(buf, 0, i);
	        		break;
	        	}
	        }
	        fis.close();

	        Log.d("NLOG", String.format("%s.postFile() - postUrl = %s.", this, postUrl));
	        if (postUrl == null) {
	        	return result;
	        }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/* TODO ����״̬ */
		HttpURLConnection conn = null;
		ConnectivityManager conManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mobile = conManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		NetworkInfo wifi = conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		Proxy proxy = null;
		if (wifi != null && wifi.isAvailable()) {
			Log.d("NLOG", "WIFI is available");
		} else if (mobile != null && mobile.isAvailable()) {
			String apn = mobile.getExtraInfo().toLowerCase();
			Log.d("NLOG", "apn = " + apn);
			if (apn.startsWith("cmwap") || apn.startsWith("uniwap") || apn.startsWith("3gwap")) {
				proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress("10.0.0.172", 80));
			} else if (apn.startsWith("ctwap")) {
				proxy = new Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress("10.0.0.200", 80));
			}
		} else { //@fixed in TV
			Log.d("NLOG", "getConnection:not wifi and mobile");
		}
		URL url;
		try {
			url = new URL(postUrl);
			if (proxy == null) {
				conn = (HttpURLConnection)url.openConnection();
			} else {
				conn = (HttpURLConnection)url.openConnection(proxy);
			}
			conn.setConnectTimeout(connTimeout);
			conn.setReadTimeout(readTimeout);
			
			// POST��ʽ
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setUseCaches(false);
			conn.setRequestProperty("Content-Type", "gzip");
			
			conn.connect();
			GZIPOutputStream gos = new GZIPOutputStream(conn.getOutputStream());
			String lockedname = item.locked;
			if (lockedname == null) { // ��Ҫ�����ļ�
				lockedname = buildLocked(item.name);
			}
			File locked = new File(lockedname);
			@SuppressWarnings("resource")
			FileInputStream fis = new FileInputStream(lockedname);
	        int offset = 0;
	        Boolean isHead = false;
    		while ((len = fis.read(buf, 0, size)) != -1) {
				int t = 0;
				for (int i = 0; i < len; i++) {
					buf[i] = (byte)(buf[i] ^ offset % 256 ^ pass[offset % pass.length]); 
					offset++;
					if (!isHead) {
						if (buf[i] == '\n') {
							t = i;
							isHead = true;
						}
					}
				}
				gos.write(buf, t + 1, len - t - 1);
			}
    		gos.close();
    		gos = null;
			
			// �����������װһ��BufferReader����߶���Ч�� getInputStream���ص����ֽ���������ת�����ַ���
			//*
			StringBuffer sb = new StringBuffer();
			BufferedReader bufr = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String inputString;
			while ((inputString = bufr.readLine()) != null) {
				sb.append(inputString);
			}
			bufr.close(); // ���رգ����ͷ�������Դ
			bufr = null;

			conn.disconnect();

			int length = conn.getContentLength();
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK && length != 0) {
				result = true;
				locked.delete();
				Log.d("NLOG", "post success!");
				// ����ɹ�
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			conn.disconnect();
			conn = null;
		}
		return result;
	}
    /**
     * ���������Ϊ�ļ������֮ǰ�����ļ���׷��д��
     * @param item
     * @return
     */
    public Boolean saveFile(CacheItem item) {
    	if (item == null) {
    		return false;
    	}
    	String filename = String.format(cacheFileFormat, fileVersion, item.name);
		/* debug start */
        System.out.println(String.format("%s.saveFile() filename : %s", this, filename));
		/* debug end */
        
        Boolean result = false;
        synchronized(item) {
	    	try {
	    		File file = new File(filename);
	    		int offset = 0;
	    		byte[] linesBuffer;
	    		if (file.exists()) {
	    			offset = (int)file.length();
	    		}
	    		if (offset >= sendMaxBytes) { // �ļ�������Χ���������ļ�
	    			buildLocked(item.name); // ��֮ǰ���ļ�����
	    			offset = 0;
	    		}
	    		if (offset <= 0) { // �ļ������� // ͷ����д
	    			linesBuffer = (item.head + '\n' + item.sb.toString()).toString().getBytes();
	    		} else {
	    			linesBuffer = item.sb.toString().getBytes();
	    		}
	    		byte[] pass = item.pass;
				if (pass != null && pass.length > 0) { // ��Ҫ����
					for (int i = 0, len = linesBuffer.length; i < len; i++) {
						int t = (int) (i + offset);
						linesBuffer[i] = (byte)(linesBuffer[i] ^ t % 256 ^ pass[t % pass.length]); 
					}
				}
				@SuppressWarnings("resource")
				FileOutputStream fos = new FileOutputStream(filename, true);
				fos.write(linesBuffer);
				fos.flush();
				item.sb.delete(0, item.sb.length()); // �������
				result = true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
        }
        return result;
    }
        
    /**
     * ��ȡmd5�ַ���
     * @param text �ı�
     * @return ����Сдmd5���к�
     */
	public static String getMD5(String text) {
		String result = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(text.getBytes());
			StringBuffer sb = new StringBuffer();
			for (byte b : md.digest()) {
				sb.append(Integer.toHexString(((int)b & 0xff) + 0x100).substring(1));
			}
			result = sb.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * ������
	 */
	private Context context;
	public Context getContext() {
		return context;
	}

	/**
	 * ����洢�ľ��
	 */
	private StorageHandler storageHandler;
	
	/**
	 * ��ʼ��Ŀ¼��Ϣ
	 */
	private final byte MESSAGE_INIT = 1;
	
	/**
	 * ����Ϊ�ļ�����Ϣ
	 * @param obj item
	 */
	private final byte MESSAGE_SAVEFILE = 2;
	
	/**
	 * �ϱ��ļ�
	 * @param obj item
	 */
	private final byte MESSAGE_POSTFILE = 3;
	
	/**
	 * ����洢�ľ��
	 */
    private class StorageHandler extends Handler {
    	StorageHandler(Looper looper) {
            super(looper);
        }
    	
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	case MESSAGE_INIT:
            		/* debug start */
            		Log.i("NLOG", String.format("case MESSAGE_INIT"));
            		/* debug end */
                	File file = new File(rootDir + File.separatorChar);
            		if (!file.exists()) {
            			file.mkdirs();
            		}
            		break;
                case MESSAGE_SAVEFILE: {
            		/* debug start */
                    Log.i("NLOG", String.format("case MESSAGE_SAVEFILE"));
            		/* debug end */
                    
                	if (msg.obj == null) {
                		break;
                	}
                	
    				CacheItem cacheItem = (CacheItem)msg.obj;
        			synchronized(messages) { // �����Ϣ
        				String msgName = String.format("%s.%s", cacheItem.name, MESSAGE_SAVEFILE);
    			        messages.put(msgName, null);
        			}
        			saveFile(cacheItem); // ����� item.sb����
        			sendMessage_postFile(new PostItem(cacheItem.name, null));
                    break;
                }
                case MESSAGE_POSTFILE: {
            		/* debug start */
            		Log.i("NLOG", String.format("case MESSAGE_POSTFILE"));
            		/* debug end */
                    
                    PostItem postItem = (PostItem)msg.obj;
        			synchronized(messages) { // �����Ϣ
        				String msgName = String.format("%s.%s", postItem.name, MESSAGE_POSTFILE, postItem.locked != null);
    			        messages.put(msgName, null);
        			}
        			postFile(postItem);
                	break;
                }
            }
        }

    }
    /**
     * ��ʱ������־
     */
    private Timer sendTimer = null;
    private void updateTimer() {
        if (sendTimer != null) {
        	sendTimer.cancel();
        	sendTimer = null;
    	}
        sendTimer = new Timer();
        sendTimer.schedule(new TimerTask() {
        	/**
        	 * �ȴ����͵��ļ�
        	 */
        	private Pattern dataFilePattern = Pattern.compile("\\b_nlog(?:_(\\d+))?_(\\w+\\.[a-f0-9]{32})(?:\\.([a-z0-9]+))?\\.(locked|dat)$");
        	// '_nlog_1_wenku.01234567890123456789012345678901.h0123456.locked'
        	// '_nlog_1_wenku.01234567890123456789012345678901.dat'
        	
			@Override
			public void run() {
				if (onlywifi && !checkWifiConnected()) {
					return;
				}
				File file = new File(rootDir + File.separatorChar);
    			for (File subFile : file.listFiles()) {
            		/* debug start */
    				Log.i("NLOG", String.format("file : %s(%sbyte).", subFile.getName(), subFile.length()));
            		/* debug end */
    				
    				Matcher matcher = dataFilePattern.matcher(subFile.getName());
    				if (!matcher.find()) { // ������nlog�ļ���
    					continue;
    				}
    				
    				// �����ϱ�����Χ
					if (System.currentTimeMillis() - subFile.lastModified() >= saveMaxDays * 24 * 60 * 60 * 1000) {
	            		/* debug start */
	    				Log.i("NLOG", String.format("del file : %s(%sbyte).", subFile.getName(), subFile.length()));
	            		/* debug end */
						subFile.delete();
						continue;
					}
					
					String version = matcher.group(1);
    				String itemname = matcher.group(2); // ����
    				String extname = matcher.group(4); // ��չ��
    				if (fileVersion.equals(version)) { // �����ݵİ汾
    					subFile.delete();
    					continue;
    				}
    				// ��ʼ�����ļ�
    				if (sendMessage_postFile(new PostItem(itemname, "locked".equals(extname) ? subFile.getName() : null))) { // ���ͳɹ�
    					return;
    				}
    			}
			}
        }, 100, sendInterval * 1000);
    }
    
    /**
     * �����ύ�ļ�����Ϣ
     * @param item �ύ�item{ name, locked }
     * @return �����Ƿ�����Ϣ
     */
    private Boolean sendMessage_postFile(PostItem item) {
    	Boolean result = false;
		synchronized(messages) { // ��Ϣ���ڷ��͵�;��
			String msgName = String.format("%s.%s.%s", item.name, MESSAGE_POSTFILE, item.locked != null);
			Message m = messages.get(msgName);
			if (m == null) { // �Ƿ�����ͬ����Ϣ�ڴ���
				m = storageHandler.obtainMessage(MESSAGE_POSTFILE, 0, 0, item);
		        storageHandler.sendMessageDelayed(m, 5000);
		        messages.put(msgName, m);
		        result = true;
		        Log.i("NLOG", String.format("MESSAGE_POSTFILE '%s' message send", msgName));
			} else {
				Log.i("NLOG", String.format("MESSAGE_POSTFILE message sending..."));
			}
		}
		return result;
    }
    
    /**
     * ���ͱ����ļ�����Ϣ
     * @param item �ύ�item{ name }
     * @return �����Ƿ�����Ϣ
     */
    private Boolean sendMessage_saveFile(CacheItem item) {
    	Boolean result = false;
		synchronized(messages) { // ��Ϣ���ڷ��͵�;��
			String msgName = String.format("%s.%s", item.name, MESSAGE_SAVEFILE);
			Message m = messages.get(msgName);
			if (m == null) { // �Ƿ�����ͬ����Ϣ�ڴ���
				m = storageHandler.obtainMessage(MESSAGE_SAVEFILE, 0, 0, item);
		        storageHandler.sendMessageDelayed(m, 1000);
		        messages.put(msgName, m);
		        result = true;
		        Log.i("NLOG", String.format("MESSAGE_SAVEFILE '%s' message send", msgName));
			} else {
				Log.i("NLOG", String.format("MESSAGE_SAVEFILE message sending..."));
			}
		}
		return result;
    }

    /**
     * ���캯��
     * @param context ������
	 * @throws  
     */
    public NStorage(Context context) {
    	this.context = context;
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        deviceId = tm.getDeviceId();
        
        HandlerThread handlerThread = new HandlerThread("NSTORAGE_HANDLER",
        		Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
    	storageHandler = new StorageHandler(handlerThread.getLooper());	
        Message msg = storageHandler.obtainMessage(MESSAGE_INIT);
        storageHandler.sendMessageDelayed(msg, 100);
        updateTimer();
    }
}
