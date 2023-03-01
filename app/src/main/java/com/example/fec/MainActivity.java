package com.example.fec;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationRequest;
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
import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static android.util.Log.e;

import com.google.firebase.FirebaseApp;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.FirebaseException;
import com.google.firebase.storage.StorageException;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnCompleteListener;

public class MainActivity extends AppCompatActivity {
    TextView tv_info,tv_fileLocation,tv_guide;
    Button btn_stop,btn_share,btn_browser,btn_uploadExcel;
    SharedPreferences sp;
    Map<String,?>  deviceInfoMap;
    public String deviceName;
    public  boolean isDeviceInfoFileWritten=false;
    public static final int cellCount=2;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();
        /*locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);*/


        // Initialize Firebase Auth

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();

        mAuth.signInAnonymously().addOnSuccessListener(this, new  OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        Log.d("Firebase SignIn", "signInAnonymously:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {

                        Log.e("Firebase Signin", "signInAnonymously:FAILURE", exception);
                        Toast.makeText(MainActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });

        deviceName = String.valueOf(Build.DEVICE);

        tv_info=(TextView)findViewById(R.id.main_tv_deviceInfo);
        btn_share=(Button)findViewById(R.id.main_btn_share);
        btn_stop=(Button)findViewById(R.id.main_btn_save);
        btn_browser=(Button)findViewById(R.id.btn_startBrowser);
        tv_fileLocation=(TextView)findViewById(R.id.main_tv_file);
        tv_guide=(TextView)findViewById(R.id.main_tv_guide);
        tv_guide.setText(R.string.guide);
        btn_uploadExcel = (Button) findViewById(R.id.excel);

        btn_browser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("BROWSE","onclickcalled");
                startBrowser(v);
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

    public void startBrowser(View v)
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
        File dirDown = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dirDown, "DeviceInfo");
        if (!file.exists()) {
            file.mkdir();
        } else {
            System.out.println("File already exists.");
        }

        FileOutputStream outputStream;
        try{
            File file1=new File(file,filename);
            outputStream=new FileOutputStream(file1);
            outputStream.write(filecontent.getBytes());
            outputStream.close();
            isDeviceInfoFileWritten = true;
            editor.putString("Device Info Logged","true");
            Toast.makeText(this,"Device Info logged to:"+String.valueOf(file1),Toast.LENGTH_LONG).show();
            tv_fileLocation.setText("Location of Log Files: "+String.valueOf(file1));
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void setBtn_share(View v)
    {
        File dirDown = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File directory = new File(dirDown, "DeviceInfo");

        File file = new File(directory, deviceName+"_log.csv");

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, "com.example.fec.MainActivity", file));
        shareIntent.setType("text/csv");
        startActivity(Intent.createChooser(shareIntent, "Share file using"));

    }

    public void setBtn_uploadExcel(View v)
    {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();

        StorageReference deviceLogFileRef = storageRef.child(deviceName+"_log.csv");
        StorageReference fileRef = storageRef.child("files/" + deviceName + "_log.csv");

        fileRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                // If the file exists, delete it
                fileRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // File deleted successfully
                        // Now upload the new file
                        uploadFile(storageRef);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to delete file
                        Log.e("Deletion", "Failed to delete file", e);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // If the file does not exist, upload a new file
                if (e instanceof StorageException && ((StorageException) e).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                    uploadFile(storageRef);
                } else {
                    // Failed to get file metadata
                    Log.e("Search File", "Failed to get file metadata", e);
                }
            }
        });


        /*FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        uploadFile(storageRef);*/

    }

    private void uploadFile(StorageReference storageRef) {
        // Create a reference to the local file
        File dirDown = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File dir = new File(dirDown, "DeviceInfo");
        File file = new File(dir, deviceName+"_log.csv");

        // Create a reference to "data.csv"
        StorageReference deviceLogFileRef = storageRef.child(deviceName+"_log.csv");
        // Create a reference to 'files/data.csv'
        StorageReference filesRef = storageRef.child("files/" + deviceName + "_log.csv");
        // Create the file metadata
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("text/csv")
                .build();

        // Upload file to Firebase Storage
        UploadTask uploadTask = filesRef.putFile(Uri.fromFile(file), metadata);

        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle Failures
                Toast.makeText(MainActivity.this,"Upload Failed",Toast.LENGTH_SHORT).show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Handle successful uploads
                Toast.makeText(MainActivity.this,"File Uploaded Successfully!",Toast.LENGTH_SHORT).show();
            }
        });
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
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        // this method is called when all permissions are granted
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            // do you work now
                            Toast.makeText(MainActivity.this, "All the permissions are granted..", Toast.LENGTH_SHORT).show();
                            if(Environment.isExternalStorageManager())
                            {
                               /* internal = new File("/sdcard");
                                internalContents = internal.listFiles();*/
                            }
                            else
                            {
                                Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                startActivity(permissionIntent);
                            }
                        }
                        // check for permanent denial of any permission
                        if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permanently, we will show user a dialog message.
                            //showSettingsDialog();
                            int a =0;
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

    public void updateUI(FirebaseUser account){

        if(account != null){
            Log.e("Sign In Status: ","Successful");
        }else
        {
            Log.e("Sign In Status: ","Filed");
        }

    }


}
