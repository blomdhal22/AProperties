AProperties
===========

Change Log
----------

* 2015/11/26 : Created


Overview
--------

AProperties is a wrapper for Properties. Futher more, It can read System Property without proper permmision and It can store property value under "/data/data/(your package)/property", when key start with "me.persist.*".

### Feature

1. Get System Property
2. Set System Property, if you have a proper permission such as system.
3. Get / Set Property
4. Get / Set Persist Property
5. Register / Unregister Property Listener

Quick Start
-----------

1. Add AProperties.jar to your project class path
2. Configure AProperties using ${project-root}/assets/aproperties.prop

*Example:*

```java
public class MainActivity extends Activity {
    
    private static String TAG = MainActivity.class.getSimpleName();
    private static boolean D = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (D) Log.i(TAG, "onCreate()");
        
        AProperties.APropertiesListener listner = new AProperties.APropertiesListener() {
            
            @Override
            public void update(String key, String old, String newer) {
                
                Log.e(TAG, "key: " + key);
                Log.e(TAG, "old: " + old);
                Log.e(TAG, "newer: " + newer);
            }
        };
        
        // Register listener
        AProperties.registerAPropertiesListener(listner);
        
        // Current AProperties Status
        Log.i(TAG, AProperties.getInstance().toString());
        
        // Read System Property
        Log.i(TAG, "ro.build.type : " + AProperties.getSystemProperty(AProperties.KEY.SYS_RO_BUILD_TYPE));
        Log.i(TAG, "ro.board.platform : " + AProperties.getSystemProperty("ro.board.platform"));
        
        // Read my property
        Log.i(TAG, "me.ro.app.version : " + AProperties.getProperty("me.ro.app.version"));
        
        // Read my persist property
        // Read from /data/data/<your package>/property/<key>
        Log.i(TAG, "me.persist.sample : " + AProperties.getProperty("me.persist.sample"));
        
        // Set property
        AProperties.setProperty("me.sample.test", "true");
        
        // NOTE: me.ro.* cannot be modified.
        AProperties.setProperty("me.ro.app.version", "1.0.0");
        
        // Set persist property
        // File write to /data/data/<your package>/property/<key>
        // Preserve last value Even though rebooting.
        AProperties.setProperty("me.persist.sample", "0");
        
        Log.i(TAG, AProperties.getInstance().toString());
        
        // Unregister listener
        AProperties.unregisterPropertiesListener(listner);
    }
}
```

*result:*

```vim
01-09 13:16:23.630 I 1846     1846     MainActivity:               onCreate() 
01-09 13:16:23.660 I 1846     1846     MainActivity:               {me.ro.app.version=0.0.1, ro.build.version.release=4.0.4, ro.build.type=userdebug, ro.board.platform=imx6, ro.hardware=freescale}, {me.persist.sample=1} 
01-09 13:16:23.660 I 1846     1846     MainActivity:               ro.build.type : userdebug 
01-09 13:16:23.660 I 1846     1846     MainActivity:               ro.board.platform : imx6 
01-09 13:16:23.660 I 1846     1846     MainActivity:               me.ro.app.version : 0.0.1 
01-09 13:16:23.660 I 1846     1846     MainActivity:               me.persist.sample : 1 
01-09 13:16:23.660 E 1846     1846     MainActivity:               key: me.sample.test 
01-09 13:16:23.660 E 1846     1846     MainActivity:               old:  
01-09 13:16:23.660 E 1846     1846     MainActivity:               newer: true 
01-09 13:16:23.660 E 1846     1846     MainActivity:               key: me.persist.sample 
01-09 13:16:23.660 E 1846     1846     MainActivity:               old: 1 
01-09 13:16:23.660 E 1846     1846     MainActivity:               newer: 0 
01-09 13:16:23.660 I 1846     1846     MainActivity:               {me.sample.test=true, me.ro.app.version=0.0.1, ro.build.version.release=4.0.4, ro.build.type=userdebug, ro.board.platform=imx6, ro.hardware=freescale}, {me.persist.sample=0} 
```

*Run time, persist property:*

```vim
root@android:/data/data/com.github.alogsample/property # ll
-rw-r--r-- app_90   app_90          1 1970-01-09 13:16 me.persist.sample

root@android:/data/data/com.github.alogsample/property # cat me.persist.sample
0
```


Configuration Property
-----------------------

*Example:*

```vim
# General configuration file
#
# Naming Convention:
# For distinguish from System Properties.
#     <domain>.<attribute>.<identity>
#  ex) me.ro.app.version=0.0.1

# General Configurations
# ==========================================================
me.ro.app.version=0.0.1
```