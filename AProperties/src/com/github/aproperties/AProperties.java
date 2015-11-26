package com.github.aproperties;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import android.util.Log;

/**
 */
public final class AProperties {

    private static final String TAG = AProperties.class.getSimpleName();
    private static final boolean D = true;

    public static class KEY {
        // System Properties which you want to handle.
        public static final String SYS_RO_BOARD_PLATFORM = "ro.board.platform";
        public static final String SYS_RO_BUILD_PRODUCT = "ro.build.product";
        public static final String SYS_RO_BUILD_TYPE = "ro.build.type";
        public static final String SYS_RO_BUILD_VERSION_RELEASE = "ro.build.version.release";
        public static final String SYS_RO_HARDWARE = "ro.hardware";
        public static final String SYS_RO_HW_ORIENTATION = "ro.hw.orientation";

        // Your Properties which you want to handle.
        public static final String AAP_RO_APP_VERSION = "me.ro.app.version";

        // Your Persist Properties which you want to handle.
        public static final String AAP_PERSIST_SAMPLE = "me.persist.sample";
    } // KEY
    
    private static class APropertiesHolder {
        public static AProperties SINGLETON = new AProperties();
    }
    
    public static AProperties getInstance() {
        return APropertiesHolder.SINGLETON;
    }

    private static final Properties mProp = new Properties();
    private static final Properties mPersist = new Properties();

    private static final String RO_PREFIX = "ro.";
    private static final String ME_RO_PREFIX = "me.ro.";
    private static final String ME_PERSIST_PREFIX = "me.persist.";

    // ex) /data/data/
    private static final String DATA_DIR = "/data/data";

    // ex) /data/data/com.example.android.app/
    private static final String BASE_DIR = DATA_DIR + File.separator + getAppName();
    
    private static final String ASSETS_DIR = "assets";
    private static final String PROP_FILE = "aproperties.prop";
    private static final String CONFIG_PATH = ASSETS_DIR + File.separator + PROP_FILE;
    private static final String PERSIST_DIR = AProperties.getBaseDir() + File.separator + "property";
    
    private static final CopyOnWriteArrayList<APropertiesListener> mAPropertiesListeners = new CopyOnWriteArrayList<APropertiesListener>();

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Pre load {
    static {

        try {
            Class<?> clazz = AProperties.class.getClassLoader().loadClass(
                    "android.os.SystemProperties");

            Class[] types = new Class[1];
            types[0] = String.class;

            Object[] params = new Object[1];

            Method get = clazz.getMethod("get", types);
            String value = null;

            // ex: s5pc110
            params[0] = KEY.SYS_RO_BOARD_PLATFORM;
            value = (String) get.invoke(clazz, params);
            mProp.setProperty(KEY.SYS_RO_BOARD_PLATFORM, value);

            // ex: crespo
            params[0] = KEY.SYS_RO_BUILD_PRODUCT;
            value = (String) get.invoke(clazz, params);
            mProp.setProperty(KEY.SYS_RO_BUILD_PRODUCT, value);

            // ex: userdebug, user
            params[0] = KEY.SYS_RO_BUILD_TYPE;
            value = (String) get.invoke(clazz, params);
            mProp.setProperty(KEY.SYS_RO_BUILD_TYPE, value);

            // ex: 4.0.4
            params[0] = KEY.SYS_RO_BUILD_VERSION_RELEASE;
            value = (String) get.invoke(clazz, params);
            mProp.setProperty(KEY.SYS_RO_BUILD_VERSION_RELEASE, value);

            // ex: herring
            params[0] = KEY.SYS_RO_HARDWARE;
            value = (String) get.invoke(clazz, params);
            mProp.setProperty(KEY.SYS_RO_HARDWARE, value);

            // read general config
            AProperties.loadGeneralConfig();

            // read persist properties
            AProperties.loadPersistProperties();
            
            // check persist property in general properties.
            AProperties.checkPersistProperties();
            
            if (D) {
                Log.d(TAG, mProp.toString());
                Log.d(TAG, mPersist.toString());
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    
    // read general config
    private static void loadGeneralConfig() {
        
        InputStream configProp = null;
        
        try {
            configProp = findConfigAsStream(CONFIG_PATH);
            
            if (configProp != null)
                mProp.load(configProp);
            
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (configProp != null) {
                try {
                    configProp.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // read persist properties
    private static void loadPersistProperties() {
        
        File dir = new File(PERSIST_DIR);
        File[] flist = null;
        BufferedReader reader = null;
        
        try {
            if (dir.exists() == false)
                dir.mkdir();
            
            flist = dir.listFiles();
            
            if (flist != null) {
                
                for (File f : flist) {
                    String key = f.getName();
                    String val = "";
                    
                    reader = new BufferedReader(new FileReader(f));
                    
                    val = reader.readLine();
                    
                    if (D) Log.d(TAG, "loadPersistProperties(), Persist key / value : " + key + "/ " + val);
                    
                    AProperties.setProperty(key, val);
                    
                    reader.close();
                    reader = null;
                    
                }
            } else
                Log.w(TAG, "Persist property file does not exists.");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // check persist property in general properties.
    private static void checkPersistProperties() {

        String[] keys = new String[mProp.keySet().size()];

        mProp.keySet().toArray(keys);

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            String val = "";

            if (key.startsWith(ME_PERSIST_PREFIX)) {

                val = (String) mProp.remove(key);

                if (D) Log.d(TAG, "Remove general prop, key / value : " + key + " / " + val);

                // put to persist
                if (mPersist.get(key) == null) {
                    AProperties.setProperty(key, val);
                }
            }
        }
    }
    
    private static InputStream findConfigAsStream(String path) {
        return AProperties.class.getClassLoader().getResourceAsStream(path);
    }
    // Pre load }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // AProperties {
    private AProperties() { }
    
    public static Object setProperty(String key, String value) {

        Object[] local = null;
        Object old = null;

        if (key == null)
            return null;
        if (value == null)
            return null;

        /* me.ro.* properties may NEVER be modified once set */
        if (key.startsWith(ME_RO_PREFIX)) {
            Log.w(TAG, ME_RO_PREFIX +
                    "* properties may NEVER be modified once set");
            return null;
        }

        /* ro.* properties may NEVER be modified once set */
        if (key.startsWith(RO_PREFIX)) {
            Log.w(TAG, RO_PREFIX +
                    "* properties may NEVER be modified once set");
            return null;
        }

        /* aap.persist.* store property */
        if (key.startsWith(ME_PERSIST_PREFIX)) {
            old = mPersist.setProperty(key, value);

            AProperties.writePersistProperty(key, value);

            if (D)
                Log.d(TAG, "set persist : " + mPersist.toString());
            
        } else {
            old = mProp.setProperty(key, value);
        }

        if (old == null)
            old = "";

        synchronized (AProperties.class) {
            local = mAPropertiesListeners.toArray();

            for (Object e : local) {

                if (e != null)
                    ((APropertiesListener) e)
                            .update(key, (String) old, value);
            }
        }

        return old;
    }
    
    private static void writePersistProperty(String key, String value) {
        
        final String path = PERSIST_DIR + File.separator + key;
        
        try {
            AProperties.store(path, value.getBytes());
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static String getProperty(String key) {

        if(key.startsWith(ME_PERSIST_PREFIX))
            return mPersist.getProperty(key);

        return mProp.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {

        if(key.startsWith(ME_PERSIST_PREFIX))
            return mPersist.getProperty(key, defaultValue);

        return mProp.getProperty(key, defaultValue);
    }
    // AAP Property }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Support read/write System Property {
    public static String getSystemProperty(String key) {

        String value = null;

        try {
            Class<?> clazz = AProperties.class.getClassLoader().loadClass(
                    "android.os.SystemProperties");

            Class[] types = new Class[1];
            types[0] = String.class;

            Object[] params = new Object[1];

            Method get = clazz.getMethod("get", types);

            params[0] = key;
            value = (String) get.invoke(clazz, params);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return "".equals(value) == true ? null : value;
    }

    public static String getSystemProperty(String key, String defaultValue) {

        String value = AProperties.getSystemProperty(key);

        return (value == null) ? defaultValue : value;
    }
    
    /**
     * NOTE: Only supports When you have a proper permission such as system.
     * 
     * @param key
     * @param value
     */
    public static void setSystemProperty(String key, String value) {

        try {
            Class<?> clazz = AProperties.class.getClassLoader().loadClass(
                    "android.os.SystemProperties");

            Method m = clazz.getMethod("set", new Class[] {String.class, String.class}) ;

            Object[] params = new Object[2];
            params[0] = key;
            params[1] = value;

            m.invoke(clazz, params);
            
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
    // Support read/write System Property }

    @Override
    public String toString() {
        return mProp.toString() + ", " + mPersist.toString();
    }

    public static void registerAPropertiesListener(
            APropertiesListener l) {

        if (l == null)
            throw new NullPointerException();

        if (!mAPropertiesListeners.contains(l))
            mAPropertiesListeners.add(l);
    }

    public static void unregisterPropertiesListener(
            APropertiesListener l) {
        mAPropertiesListeners.remove(l);
    }

    // Observer
    public interface APropertiesListener {
        public void update(String key, String old, String newer);
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    private static String getBaseDir() {
        return BASE_DIR;
    }
    
    private static void store(File file, byte[] raw) throws FileNotFoundException, IOException {
        
        OutputStream out = null;
        
        try {
            // permission, 644
            file.setReadable(true, false);
            
            out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(raw);
            out.flush();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Store byte array to path.
     * 
     * DATA_DIR is "/data/data/
     * BASE_DIR is "/data/data/<app name>/"
     * 
     */
    private static void store(String filename, byte[] raw) throws FileNotFoundException, IOException {
        
        final String OUTPUT_PATH = filename.startsWith(BASE_DIR) == true ? filename
                : BASE_DIR + filename;
        
        AProperties.store(new File(OUTPUT_PATH), raw);
    }
    
    /**
     * Store byte array to path.
     * 
     * DATA_DIR is "/data/data/
     * BASE_DIR is "/data/data/<app name>/"
     * 
     */
    private static void store(String parent, String file, byte[] raw) throws FileNotFoundException, IOException {
        
        final String PARENT = new StringBuilder(BASE_DIR).append(File.separator).append(parent).toString();
        
        File d = new File(PARENT);
        if (d.exists() == false) {
            d.mkdirs();
            d.setReadable(true, false);
        }
        
        File f = new File(d, file);

        AProperties.store(f, raw);
    }
    
    /**
     * @return Current Process ID
     */
    private static int myPid() {
        return android.os.Process.myPid();
    }
    
    /**
     * Get App name(package name) name by current process id.
     * 
     * @return App Name, ex) com.example.apidemo
     */
    private static String getAppName() {
        return getAppName(myPid());
    }
    
    /**
     * Get AppName(package name) Name by specific process id.
     * 
     * @return App Name, ex) com.example.me
     */
    private static String getAppName(int pid) {
        final String PATH = "/proc/" + pid + "/cmdline";

        BufferedReader cmdlineReader = null;
        StringBuilder processName = new StringBuilder();
        int c = 0;
        
        try {
            cmdlineReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(PATH)));
            
            while ((c = cmdlineReader.read()) > 0) {
                processName.append((char)c);
            }
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // When read() IOException, cmdlineReader could be resource leak.
            try {
                if (cmdlineReader != null)
                    cmdlineReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return processName.toString();
    }
}
