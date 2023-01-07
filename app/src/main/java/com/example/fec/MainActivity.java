package com.example.fec;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.work.Configuration;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import android.app.AlertDialog;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static android.util.Log.e;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

public class MainActivity extends AppCompatActivity {
    TextView tv_info,tv_fileLocation,tv_guide;
    Button btn_stop,btn_share,btn_browser;
    SharedPreferences sp;
    Map<String,?>  deviceInfoMap;
    public  boolean isDeviceInfoFileWritten=false;
    public static final int cellCount=2;
    Button excel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();

        /*if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            int temp = 0;
        }
        else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},101);
        }*/

        tv_info=(TextView)findViewById(R.id.main_tv_deviceInfo);
        btn_share=(Button)findViewById(R.id.main_btn_share);
        btn_stop=(Button)findViewById(R.id.main_btn_save);
        btn_browser=(Button)findViewById(R.id.btn_startBrowser);
        tv_fileLocation=(TextView)findViewById(R.id.main_tv_file);
        tv_guide=(TextView)findViewById(R.id.main_tv_guide);
        tv_guide.setText(R.string.guide);
        excel = (Button) findViewById(R.id.excel);
        btn_browser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("BROWSE","onclickcalled");
                startBrower(v);
            }
        });
        sp=getSharedPreferences("Device_Info",MODE_PRIVATE);
        String brand=Build.BRAND;
        SharedPreferences.Editor editor= sp.edit();
        editor.putString("Brand",Build.BRAND);
        editor.putString("Device",Build.DEVICE);
        editor.putString("Hardware",Build.HARDWARE);
        editor.putString("Manufacturer",Build.MANUFACTURER);
        editor.putString("Model",Build.MODEL);
        editor.putString("Base OS",Build.VERSION.BASE_OS);
        editor.putString("Release",Build.VERSION.RELEASE);
        editor.putString("SDK",String.valueOf(Build.VERSION.SDK_INT));
        editor.putString("Security Patch",Build.VERSION.SECURITY_PATCH);
        editor.putString("Device info logged?","false");
        editor.commit();
        SharedPreferences sp=getSharedPreferences("Device_Info",MODE_PRIVATE);
        deviceInfoMap=sp.getAll();
        displayDeviceInfo();
        if(sp.getString("Device info logged?","").toString().equals("false"))
            writetofile();

        startLogging();

    }

    void displayDeviceInfo()
    {
        tv_info.setText(deviceInfoMap.toString());
    }

    public void startBrower(View v)
    {
        Log.e("BROWSE","onclickcalled");
        Intent startBrowser=new Intent(Intent.ACTION_VIEW);
        String urlToDontkillmyapp="https://dontkillmyapp.com";
        startBrowser.setData(Uri.parse(urlToDontkillmyapp));
        startActivity(startBrowser);
    }

    void writetofile()
    {
        String filename="deviceInfoFile.txt";
        String filecontent=deviceInfoMap.toString();
        System.out.println(filecontent);
        SharedPreferences sp=getSharedPreferences("Device_Info",MODE_PRIVATE);
        SharedPreferences.Editor editor=sp.edit();
        File file=new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"DeviceInfo");
        if(!file.mkdirs())
            Log.e("Error: ","Directory not created");
        FileOutputStream outputStream;
        try{
            File file1=new File(file,filename);
            outputStream=new FileOutputStream(file1);
            outputStream.write(filecontent.getBytes());
            outputStream.close();
            editor.putString("Device Info Logged","true");
            Toast.makeText(this,"Device Info logged to:"+String.valueOf(file1),Toast.LENGTH_LONG).show();
            //isDeviceInfoFileWritten=true;
            tv_fileLocation.setText("Location of Log Files: "+String.valueOf(file1));
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void setBtn_share(View v)
    {
        File directory=new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"DeviceInfo");
        File infoFile=new File(directory,"deviceInfoFile.txt");
        File logFile=new File(directory,"deviceLogFile.csv");
        Uri infoFileUri= FileProvider.getUriForFile(this,"com.example.fec.MainActivity",infoFile);
        this.grantUriPermission("com.example.fec",infoFileUri,Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        Uri logFileUri= FileProvider.getUriForFile(this,"com.example.fec.MainActivity",logFile);
        this.grantUriPermission("com.example.fec",logFileUri,Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);


        ArrayList<Uri> fileUri=new ArrayList<Uri>();
        fileUri.add(infoFileUri);
        fileUri.add(logFileUri);

        Intent shareIntent=new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,fileUri);
        shareIntent.setType("text/*");
        startActivity(Intent.createChooser(shareIntent,"Send files..."));
    }

    public void startLogging()
    {
        PeriodicWorkRequest logWork=new PeriodicWorkRequest.Builder(LogWorker.class,15, TimeUnit.MINUTES)
                .build();
        Log.e("myworkinitializer","periodic work created");
        try {


            WorkManager wm = WorkManager.getInstance(this);
            wm.enqueueUniquePeriodicWork("LOGWORK", ExistingPeriodicWorkPolicy.REPLACE, logWork);
            Log.e("myworkinitializer", "periodic work queued");
        }
        catch (IllegalStateException e)
        {
            Log.e("startlogging",e.getMessage()+"\n"+e.getCause());
        }
    }


    public void stopLogging(View v)
    {
        WorkManager.getInstance(getApplicationContext()).cancelUniqueWork("LOGWORK");
        Toast.makeText(this,"Logging stopped. Restart app to begin logging again",Toast.LENGTH_SHORT).show();
    }

    private void requestPermissions() {
        // below line is use to request permission in the current activity.
        // this method is use to handle error in runtime permissions
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        // this method is called when all permissions are granted
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            // do you work now
                            Toast.makeText(MainActivity.this, "All the permissions are granted..", Toast.LENGTH_SHORT).show();
                        }
                        // check for permanent denial of any permission
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permanently, we will show user a dialog message.
                            showSettingsDialog();
                        }
                    }
                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        // this method is called when user grants some permission and denies some of them.
                        permissionToken.continuePermissionRequest();
                    }
                }).withErrorListener(error -> {
                    // we are displaying a toast message for error message.
                    Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show();
                })
                // below line is use to run the permissions on same thread and to check the permissions
                .onSameThread().check();
    }
    private void showSettingsDialog() {
        // we are displaying an alert dialog for permissions
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        // below line is the title for our alert dialog.
        builder.setTitle("Need Permissions");

        // below line is our message for our dialog
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", (dialog, which) -> {
            // this method is called on click on positive button and on clicking shit button
            // we are redirecting our user from our app to the settings page of our app.
            dialog.cancel();
            // below is the intent from which we are redirecting our user.
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, 101);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // this method is called when user click on negative button.
            dialog.cancel();
        });
        // below line is used to display our dialog
        builder.show();
    }


}
