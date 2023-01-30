package com.example.fec;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.SENSOR_SERVICE;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.Build;
import android.os.StatFs;
import android.util.Log;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorEvent;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.Calendar;

public class LogWorker extends Worker {

    //private SensorManager sensorManager;
    //sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    public LogWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        //sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        String currTime=getCurrentTime();
        String batteryLevel=getBatteryLevel();
        String CPUfreq=getCurrentCPUFreqMHz();
        String availRAM=getCurrentRamUsage();
        String availStorage=getStorageInfo();
        String CPUTemperature=getCPUTemperatureCelsius();
        String acceleration=getAcceleration(getApplicationContext());
        String distance=getDistance(getApplicationContext());

        String fileContent=currTime+", "+
                batteryLevel+", "+
                CPUfreq+", "+
                availRAM+", "+
                acceleration+", "+
                distance+", "+
                CPUTemperature+", "+
                availStorage+";\n";

        boolean res=writeCSVfile(fileContent);
        if(res==true)
            return Result.success();
        else
            return Result.retry();
    }

    boolean writeCSVfile(String content)
    {
        String deviceName = String.valueOf(Build.DEVICE);
        String filename= deviceName+"_log.csv";
        String filecontent=content;
        boolean res=false;

        File dirDown = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dirDown,"DeviceInfo");
        file.mkdirs();

        FileOutputStream outputStream;
        try{
            File file1=new File(file,filename);
            outputStream=new FileOutputStream(file1,true);
            outputStream.write(filecontent.getBytes());
            outputStream.close();
            res=true;
        }catch (Exception e)
        {
            e.printStackTrace();
            res=false;
        }
        return res;
    }

    String getCurrentTime()
    {
        Calendar time=Calendar.getInstance();
        String day,month,year,hour24,minute,second;
        day=String.valueOf(time.get(Calendar.DAY_OF_MONTH));
        month=String.valueOf(time.get(Calendar.MONTH)+1);
        year=String.valueOf(time.get(Calendar.YEAR));
        hour24=String.valueOf(time.get(Calendar.HOUR_OF_DAY));
        minute=String.valueOf(time.get(Calendar.MINUTE));
        second=String.valueOf(time.get(Calendar.SECOND));
        String DateSeparater="-",TimeSeparater=":";
        String dateAndTime=year+DateSeparater+month+DateSeparater+day+" "+hour24+TimeSeparater+minute+TimeSeparater+second;
        return dateAndTime;
    }

    String getBatteryLevel()
    {
        IntentFilter ifilter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery=getApplicationContext().registerReceiver(null,ifilter);

        int level=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
        int scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
        float batteryP=level/(float)scale;
        return String.valueOf((int)(batteryP*100));
    }

    public String getCurrentCPUFreqMHz()
    {
        String currfreq="-1";
        try {

            RandomAccessFile reader = new RandomAccessFile( "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq", "r" );
            float temp=Float.parseFloat(reader.readLine())/1000;        //converting KHz value to MHz
            currfreq=String.valueOf(temp);

        } catch ( IOException ex ) {
            ex.printStackTrace();
        }

        return currfreq;
    }

    public String getCurrentRamUsage()
    {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager=(ActivityManager)getApplicationContext().getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        double avail=mi.availMem;
        double total=mi.totalMem;
        double usage=total-avail;
        double per=usage/(double)total * 100.0;
        DecimalFormat df=new DecimalFormat(".####");
        String res=df.format(per);
        return res;
    }

    public String getStorageInfo()
    {
        StatFs stat = new StatFs((Environment.getExternalStorageDirectory().getPath()));
        long bytesAvailable=stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        long availableInKB=bytesAvailable /(1024);
        return String.valueOf(availableInKB);
    }

    public String getAcceleration(Context context)
    {
        SensorManager sensorManager;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null)
        {
            Log.e("MainActivity", "Unable to get sensor service.");
            return null;
        }
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        float[] acceleration = new float[3];
        sensorManager.registerListener(new SensorEventListener()
        {
            @Override
            public void onSensorChanged(SensorEvent event)
            {
                acceleration[0] = event.values[0];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy)
            {
                //do none
            }
        }
        , accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        return String.valueOf(acceleration[0]);
    }

    public String getDistance(Context context)
    {
        LocationManager locationManager;
        Location currentLocation;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        double latitude = currentLocation.getLatitude();
        double longitude = currentLocation.getLongitude();

        Location startPoint=new Location("server");
        startPoint.setLatitude(12.9691256369786);
        startPoint.setLongitude(79.15590998754685);

        Location endPoint=new Location("mobile");
        endPoint.setLatitude(latitude);
        endPoint.setLongitude(longitude);

        double distance=startPoint.distanceTo(endPoint);
        return String.valueOf(distance);
    }

    public String getCPUTemperatureCelsius()
    {
        int temp = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone0/temp"));
            String line;
            if ((line = br.readLine()) != null) {
                temp = Integer.parseInt(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return String.valueOf(temp / 1000);

    }

}
