package com.example.fec;

import static android.content.Context.ACTIVITY_SERVICE;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DecimalFormat;
import java.util.Calendar;

public class LogWorker extends Worker {

    public LogWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String currTime=getCurrentTime();
        String batteryLevel=getBatteryLevel();
        String CPUfreq=getCurrentCPUFreqMHz();
        String availRAM=getCurrentRamUsage();
        String availStorage=getStorageInfo();
        String fileContent=currTime+", "+
                batteryLevel+", "+
                CPUfreq+", "+
                availRAM+", "+
                availStorage+";\n";
        boolean res=writeCSVfile(fileContent);
        if(res==true)
            return Result.success();
        else
            return Result.retry();
    }

    boolean writeCSVfile(String content)
    {
        String filename="deviceLogFile.csv";
        String filecontent=content;
        boolean res=false;

        File dirDown = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dirDown, "DeviceInfo");
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

}
