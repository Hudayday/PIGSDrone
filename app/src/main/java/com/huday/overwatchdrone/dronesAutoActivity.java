package com.huday.overwatchdrone;

import static android.content.ContentValues.TAG;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Observable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.icu.util.Calendar;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Result;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.huday.overwatchdrone.detector.Yolov5TFLiteDetector;
import com.huday.overwatchdrone.utils.ImageProcess;
import com.huday.overwatchdrone.utils.Recognition;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.huday.overwatchdrone.detector.Yolov5TFLiteDetector;

public class dronesAutoActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {
    final private static int MY_PERMISSION_ACCESS_COURSE_LOCATION = 114;
    final private static int MY_PERMISSION_ACCESS_FINE_LOCATION = 514;
    final private static int MY_PERMISSION_ACCESS_BACKGROUND_LOCATION = 1919;

    final private static int DEFAULT_ALTITUDE_VAL = 3;

    final private static int NUMBER_OF_DRONES = 3;

    final static String drone1url = "http://192.168.0.101:8081/cam.mjpg";
//    final static String drone1url = "http://192.168.0.145:8081/cam.mjpg";
    final static String drone2url = "http://192.168.0.101:8082/cam.mjpg";
    final static String drone3url = "http://192.168.0.101:8083/cam.mjpg";

    private FrameLayout flc;

    private GoogleMap mMap;
    LocationManager locationManager;

    private Marker currentLocation, curMarker, Drone1Marker, Drone2Marker, Drone3Marker;
    private LatLng cur, curLatLng, drone1LatLng, drone2LatLng, drone3LatLng;
    private boolean autoCenter = true;
    double lat, longt;

    private String[] ACK,BATTERY,GPS_LAT,GPS_LON,GPS_ALT,YAW,CON; //from drone
    private String COM,RADIUS,SEND_GPS_LAT,SEND_GPS_LON,SEND_GPS_ALT,P_GPS_LAT,P_GPS_LON; //from phone

    String locationProvider;

    Thread myThread;

    //drone log recorder
    StringBuffer csvFlightData;

    //Camera 0, 1, 2
    int currentDrone;

    int altitudeValue = DEFAULT_ALTITUDE_VAL;

    //Drone Marker
    int height = 80;
    int width = 80;
    BitmapDrawable bitmapdraw, bitmapDrone1, bitmapDrone2, bitmapDrone3;
    Bitmap b, b1, b2, b3;
    Bitmap droneMapMarker, drone1Marker, drone2Marker, drone3Marker;

    //buttons
    Button btnConnectSystem;
    Button bc1;
    Button bc2; //command 2
    Button bc7;
    Button bc3, bc4, bc5, bc6;
    Button bcsw1, bcsw2, bcsw3;//switch camera

    //Info
    TextView user_loc;
    TextView batteryView;
    TextView drone1_loc, drone2_loc, drone3_loc;
    //Debug
    TextView debug1,debug2,debug3,debug4; //ACK, CON, LAT, LON

    //drone Communication
    tcpClient mTcpClient;

    SimpleDateFormat simpleDateFormat;

    //Camera of drone
    private WebView droneCam1;
    private WebView droneCam2;

    private ImageView boxLabelCanvas;
    //CV
    ImageProcess imageProcess;
    Yolov5TFLiteDetector yolov5TFLiteDetector;
    Thread cvThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drones_auto);

        //write file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
        }

        //initializeLocationManager();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        final FloatingActionButton focusButton = (FloatingActionButton) this.findViewById(R.id.focusButton);
        final FloatingActionButton drone1FocusButton = (FloatingActionButton) this.findViewById(R.id.drone1FocusButton);
        final FloatingActionButton drone2FocusButton = (FloatingActionButton) this.findViewById(R.id.drone2FocusButton);
        final FloatingActionButton altitudeButton = (FloatingActionButton) this.findViewById(R.id.drone3FocusButton);
        final FloatingActionButton imageCaptureButton = (FloatingActionButton) this.findViewById(R.id.imageCaptureButton);

        flc = (FrameLayout) this.findViewById(R.id.frameLayoutcamera);
        btnConnectSystem = (Button) this.findViewById(R.id.btnConnectSystem);
        bc1 = (Button) this.findViewById(R.id.btnC1);
        bc2 = (Button) this.findViewById(R.id.btnC2);
        bc3 = (Button) this.findViewById(R.id.btnC3);
        bc4 = (Button) this.findViewById(R.id.btnC4);
        bc5 = (Button) this.findViewById(R.id.btnC5);
        bc6 = (Button) this.findViewById(R.id.btnC6);
        bc7 = (Button) this.findViewById(R.id.btnC7);

        bcsw1 = (Button) this.findViewById(R.id.btnsw1);
        bcsw2 = (Button) this.findViewById(R.id.btnsw2);
        bcsw3 = (Button) this.findViewById(R.id.btnsw3);

        //droneCam1 = (WebView) findViewById(R.id.droneCam1);
        droneCam2 = (WebView) findViewById(R.id.droneCam2);
        droneCam2.getSettings().setLoadWithOverviewMode(true);
        droneCam2.getSettings().setUseWideViewPort(true);
        //droneCam1.loadUrl("http://192.168.0.100:8081/cam.mjpg");
        droneCam2.loadUrl(drone1url);
        currentDrone = 0;

        //Marker
        bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.mipmap.drone_marker);
        b = bitmapdraw.getBitmap();
        droneMapMarker = Bitmap.createScaledBitmap(b, width, height, false);

        bitmapDrone1 = (BitmapDrawable)getResources().getDrawable(R.mipmap.drone1);
        b1 = bitmapDrone1.getBitmap();
        drone1Marker = Bitmap.createScaledBitmap(b1, width, height, false);

        bitmapDrone2 = (BitmapDrawable)getResources().getDrawable(R.mipmap.drone2);
        b2 = bitmapDrone2.getBitmap();
        drone2Marker = Bitmap.createScaledBitmap(b2, width, height, false);

        bitmapDrone3 = (BitmapDrawable)getResources().getDrawable(R.mipmap.drone3);
        b3 = bitmapDrone3.getBitmap();
        drone3Marker = Bitmap.createScaledBitmap(b3, width, height, false);

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        //record flight data
        CSVInitialization();

        //CV
        boxLabelCanvas = (ImageView)this.findViewById(R.id.box_label_canvas);
        imageProcess = new ImageProcess();
        initModel("yolov5s");


        //TEMP
        COM = "0";
        RADIUS = "000";
        GPS_ALT = new String[]{"000001","000002","000003"};
        GPS_LAT = new String[]{"00000000001","00000000002","00000000003"};
        GPS_LON = new String[]{"00000000001","00000000002","00000000003"};
        BATTERY = new String[]{"100","100","100"};
        CON = new String[NUMBER_OF_DRONES];
        YAW = new String[NUMBER_OF_DRONES];
        ACK = new String[NUMBER_OF_DRONES];

        SEND_GPS_ALT = "000000";

        user_loc = (TextView) this.findViewById(R.id.user_loc);
        drone1_loc = (TextView) this.findViewById(R.id.drone_1_loc);
        drone2_loc = (TextView) this.findViewById(R.id.drone_2_loc);
        drone3_loc = (TextView) this.findViewById(R.id.drone_3_loc);
        batteryView = (TextView) this.findViewById(R.id.batteryView);

        debug1 = (TextView) this.findViewById(R.id.textDebug1);
        debug2 = (TextView) this.findViewById(R.id.textDebug2);
        debug3 = (TextView) this.findViewById(R.id.textDebug3);
        debug4 = (TextView) this.findViewById(R.id.textDebug4);

        myThread = new Thread(new ServerForPC(1919));
        myThread.start();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map2);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        focusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(dronesAutoActivity.this, dronesManualActivity.class);
                //startActivity(intent);
                if(cur != null)
                mMap.moveCamera(CameraUpdateFactory.newLatLng(cur));
                Toast.makeText(dronesAutoActivity.this, "Focus on user", Toast.LENGTH_SHORT).show();
            }
        });

        drone1FocusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(dronesAutoActivity.this, dronesManualActivity.class);
                //startActivity(intent);
                if(drone1LatLng != null)
                mMap.moveCamera(CameraUpdateFactory.newLatLng(drone1LatLng));
                Toast.makeText(dronesAutoActivity.this, "Focus on Drone 1", Toast.LENGTH_SHORT).show();
            }
        });

        drone2FocusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(dronesAutoActivity.this, dronesManualActivity.class);
                //startActivity(intent);
                if(drone2LatLng != null)
                mMap.moveCamera(CameraUpdateFactory.newLatLng(drone2LatLng));
                Toast.makeText(dronesAutoActivity.this, "Focus on Drone 2", Toast.LENGTH_SHORT).show();
            }
        });
        
        
        altitudeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(dronesAutoActivity.this, dronesManualActivity.class);
                //startActivity(intent);
                //if(drone3LatLng != null)
                //mMap.moveCamera(CameraUpdateFactory.newLatLng(drone3LatLng));
                //Toast.makeText(dronesAutoActivity.this, "Focus on Drone 3", Toast.LENGTH_SHORT).show();

                altitudeModifyDialog();
            }
        });

        imageCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //create folder for saved image
                String folder= "PIGS";

                File f = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                ).getAbsolutePath(), folder);
                if (!f.exists()) {
                    f.mkdirs();
                }
                Calendar calendar = Calendar.getInstance();
                Date now = calendar.getTime();
                String timestamp = simpleDateFormat.format(now);
                //capture image
                Bitmap bitmap = Bitmap.createBitmap(droneCam2.getMeasuredWidth(), droneCam2.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                Canvas bitmapHolder = new Canvas(bitmap);
                droneCam2.draw(bitmapHolder);
                try {
                    String fileName = Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                    ).getAbsolutePath() + "/PIGS/webview_screenshot_"+timestamp.replaceAll(":", "_")+".jpg";
                            //dronesAutoActivity.this.getExternalFilesDir(null).getAbsolutePath() + "/webview_screenshot.jpg";
                    FileOutputStream fos = new FileOutputStream(fileName);
                    //get bitmap
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, fos);
                    bitmap.recycle();
                    fos.close();
                    Toast.makeText(dronesAutoActivity.this, "image saved at" + Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                    ).getAbsolutePath() + "/PIGS", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                } finally {
                    //bitmap.recycle();
                }
            }
        });

        btnConnectSystem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ConnectTask().execute("");
            }
        });

        bc1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(commandPack("LAND"));
                }

                Toast.makeText(dronesAutoActivity.this, "Command Land", Toast.LENGTH_SHORT).show();
            }
        });

        bc2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(commandPack("STOP"));
                }

                Toast.makeText(dronesAutoActivity.this, "Command Stop", Toast.LENGTH_SHORT).show();
                //Test remove later [TODO]
                YOLODetection();

            }
        });

        bc7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(commandPack("TAKEOFF"));
                }

                Toast.makeText(dronesAutoActivity.this, "Command Take off", Toast.LENGTH_SHORT).show();
            }
        });

        bc3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(commandPack("ORBIT"));
                }

                Toast.makeText(dronesAutoActivity.this, "Command Orbit", Toast.LENGTH_SHORT).show();
            }
        });

        bc4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(commandPack("EXPLORE"));
                }

                Toast.makeText(dronesAutoActivity.this, "Command Explore", Toast.LENGTH_SHORT).show();
            }
        });

        bc5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(commandPack("RADAR"));
                }

                Toast.makeText(dronesAutoActivity.this, "Command Radar", Toast.LENGTH_SHORT).show();
            }
        });

        bc6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTcpClient != null) {
                    mTcpClient.sendMessage(commandPack("TRACKING"));
                }

                Toast.makeText(dronesAutoActivity.this, "Command Tracking", Toast.LENGTH_SHORT).show();
            }
        });

        bcsw1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                droneCam2.loadUrl(drone1url);
                currentDrone = 0;
                Toast.makeText(dronesAutoActivity.this, "Switch to drone 1 camera", Toast.LENGTH_SHORT).show();
            }
        });

        bcsw2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                droneCam2.loadUrl(drone2url);
                currentDrone = 1;
                Toast.makeText(dronesAutoActivity.this, "Switch to drone 2 camera", Toast.LENGTH_SHORT).show();
            }
        });

        bcsw3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                droneCam2.loadUrl(drone3url);
                currentDrone = 2;
                Toast.makeText(dronesAutoActivity.this, "Switch to drone 3 camera", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void changeBatteryInfo(String num, int droneID){
        if(droneID == currentDrone)
            batteryView.setText("Battery Level: "+num+"%");
    }

    public String commandPack(String command){
        int droneID = 0;
        switch (command){
            case "LAND": COM = "0"; break;
            case "TAKEOFF": COM = "1"; break;
            case "STOP": COM = "2"; break;
            case "ORBIT": COM = "3"; break;
            case "EXPLORE": COM = "2"; break;
            case "RADAR": COM = "5"; break;
            case "TRACKING": COM = "6"; break;
            default: COM = "0"; break;
        }
        if(SEND_GPS_LAT == null || SEND_GPS_LON == null || SEND_GPS_ALT == null)
            return COM+GPS_LAT[droneID]+GPS_LON[droneID]+GPS_ALT[droneID]+RADIUS+P_GPS_LAT+P_GPS_LON;
        else
            return COM+SEND_GPS_LAT+SEND_GPS_LON+SEND_GPS_ALT+RADIUS+P_GPS_LAT+P_GPS_LON;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent home = new Intent(dronesAutoActivity.this, MainActivity.class);
            //home.setFlags((Intent.FLAG_ACTIVITY_CLEAR_TOP));
            startActivity((home));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     *
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //Initialize Google Play Services

        mMap.setMyLocationEnabled(true);

        final float zoomSize = 1.31f;
        LatLng sbMap = new LatLng(34.41567, -119.8455);
        // Add UCSB map layer
        GroundOverlayOptions UCSBMap = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.mipmap.ucsb_map))
                .transparency(0.1f)
                .position(sbMap, 2098f/zoomSize, 3275f/zoomSize);
        mMap.addGroundOverlay(UCSBMap);

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            Marker commandLocationMarker;

            @Override
            public void onMapLongClick(LatLng latLng) {

                SEND_GPS_LAT = String.format("%012.8f",latLng.latitude).substring(0,12).replace(".","");
                SEND_GPS_LON = String.format("%012.8f",latLng.longitude).substring(0,12).replace(".","");

                if(commandLocationMarker != null){
                    commandLocationMarker.remove();
                }

                commandLocationMarker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(droneMapMarker))
                        .snippet(SEND_GPS_LAT+","+SEND_GPS_LON)
                        .position(latLng).title("Drone Command Location"));

                Toast.makeText(dronesAutoActivity.this, "Drone Command Location Set", Toast.LENGTH_SHORT).show();
            }
        });

        // Add a marker in Sydney and move the camera
        LatLng UCSB = new LatLng(34.413084, -119.840212);

        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(droneMapMarker))
                .snippet("Test Drone")
                .position(UCSB).title("Test Drone"));


        mMap.moveCamera(CameraUpdateFactory.newLatLng(UCSB));
        float zoomLevel = 16.0f; //This goes up to 21
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(UCSB, zoomLevel));

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        double latitude = 0;
        double longitude = 0;
        if(location!=null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
        // Update user location
        user_loc.setText(String.format("%.4f",latitude)+","+String.format("%.4f",longitude));
        P_GPS_LON = String.format("%012.8f",longitude).substring(0,12).replace(".","");
        P_GPS_LAT = String.format("%012.8f",latitude).substring(0,12).replace(".","");

        cur = new LatLng(location.getLatitude(),location.getLongitude());
        currentLocation = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).position(cur).title("User Location"));
    }


    @Override
    public void onLocationChanged(Location location) {
        if(currentLocation!=null)
            currentLocation.remove();
        // [TODO] Implement behavior when a location update is received
        double latitude = 0;
        double longitude = 0;
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        //Toast.makeText(dronesAutoActivity.this, "Location Changed", Toast.LENGTH_SHORT).show();
        // Update user location
        user_loc.setText(String.format("%.4f",latitude)+","+String.format("%.4f",longitude));
        P_GPS_LON = String.format("%012.8f",longitude).substring(0,12).replace(".","");
        P_GPS_LAT = String.format("%012.8f",latitude).substring(0,12).replace(".","");

        cur = new LatLng(latitude, longitude);
        currentLocation = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).position(cur).title("User Location"));

        //if (autoCenter) {
        //    mMap.moveCamera(CameraUpdateFactory.newLatLng(cur));
        //}
    }



    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public class ConnectTask extends AsyncTask<String, String, tcpClient> {

        @Override
        protected tcpClient doInBackground(String... message) {

            //we create a TCPClient object
            mTcpClient = new tcpClient(new tcpClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    publishProgress(message);
                }
            });
            //Toast.makeText(dronesAutoActivity.this, "CONNECT!", Toast.LENGTH_SHORT).show();
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server

            Log.d("test", "response " + values[0]);
            //user_loc.setText(values[0]);
            Toast.makeText(dronesAutoActivity.this, values[0] + " sent!", Toast.LENGTH_SHORT).show();
            //process server response here....

        }
    }

    private void updateInfo(String msg) throws NumberFormatException {
        //Toast.makeText(dronesAutoActivity.this, msg, Toast.LENGTH_SHORT).show();
        //Battery Type
        if(msg.length()<40) { //Invalid Input
            ACK[0] = "INVALID INPUT";
            return;
        }

        int droneID = 0;

        ACK[droneID] = msg.substring(0,1);
        BATTERY[droneID] = msg.substring(1,4);
        GPS_LAT[droneID] = msg.substring(4,15);
        GPS_LON[droneID] = msg.substring(15,26);
        GPS_ALT[droneID] = msg.substring(26,32);
        YAW[droneID] = msg.substring(32,38);
        CON[droneID] = msg.substring(38,39);

        updateLocationOnMap(droneID);
        // processing data
        //try
        //lat = Double.parseDouble(GPS_LAT);
        //longt = Double.parseDouble(GPS_LON);
        // Debug Info
        changeBatteryInfo(BATTERY[droneID],droneID);
        updateCSVFile(droneID);
        debug1.setText("ACK: "+ACK[droneID]);
        debug2.setText("CON: "+CON[droneID]);
        debug3.setText("GPS_LAT: "+GPS_LAT[droneID]);
        debug4.setText("GPS_LON: "+GPS_LON[droneID]);
    }

    //update Location on Google Map based on the Location Info
    private void updateLocationOnMap(int droneID){

        switch (droneID){
            case 0:{ //Drone 1
                drone1LatLng = new LatLng(lat,longt);
                drone1_loc.setText(GPS_LAT[droneID].substring(0,8) + "," + GPS_LON[droneID].substring(0,8));
                if(Drone1Marker!=null)
                    Drone1Marker.remove();
                Drone1Marker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(drone1Marker))
                        .snippet("Drone 1")
                        .position(drone1LatLng).title("Current Drone 1 Location"));
                break;
            }
            case 1:{ //Drone 2
                drone2LatLng = new LatLng(lat,longt);
                drone2_loc.setText(GPS_LAT[droneID].substring(0,8) + "," + GPS_LON[droneID].substring(0,8));
                if(Drone2Marker!=null)
                    Drone2Marker.remove();
                Drone2Marker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(drone2Marker))
                        .snippet("Drone 2")
                        .position(drone2LatLng).title("Current Drone 2 Location"));
                break;
            }
            case 2:{ //Drone 3
                drone3LatLng = new LatLng(lat,longt);
                drone3_loc.setText(GPS_LAT[droneID].substring(0,8) + "," + GPS_LON[droneID].substring(0,8));
                if(Drone3Marker!=null)
                    Drone3Marker.remove();
                Drone3Marker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(drone3Marker))
                        .snippet("Drone 3")
                        .position(drone3LatLng).title("Current Drone 3 Location"));
                break;
            }
            default:{
                break;
            }
        }
    }

    public class ServerForPC extends Thread {
        ServerSocket serverSocket;
        private InputStream inputStream;
        private OutputStream outputStream;
        private Socket socket;
        private int port;

        ServerForPC(int PORT) {
            this.port = PORT;
        }

        public void write(byte[] bytes) throws IOException {
            outputStream.write(bytes);
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(1919);
                socket = serverSocket.accept();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                BufferedReader mBufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            } catch (IOException e) {
                e.printStackTrace();
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[1024];
                    int bytes;

                    while (socket != null) {
                        try {
                            bytes = inputStream.read(buffer);
                            if (bytes > 0) {
                                int finalBytes = bytes;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        String tempMSG = new String(buffer);
                                        updateInfo(tempMSG);
                                    }
                                });
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        // Method to close the socket
        public void closeSocket() {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void CSVInitialization(){
        csvFlightData = new StringBuffer();
        String[] title ={"time","droneID","Latitude","Longtitude","Altitude","Battery"};
        for(int i = 0; i < title.length; i++){
            csvFlightData.append(title[i]+",");
        }
    }

    private void updateCSVFile(int droneID){
        csvFlightData.append("\n");
        String[] droneData ={(new java.sql.Timestamp(System.currentTimeMillis())).toString(),String.valueOf(droneID),GPS_LAT[droneID],GPS_LON[droneID],GPS_ALT[droneID],BATTERY[droneID]};
        for(int i = 0; i < droneData.length; i++){
            csvFlightData.append(droneData[i]+",");
        }
    }

    public void createCSVFile() throws FileNotFoundException {
        //create folder for csv
        String folder= "PIGSrecords";

        File f = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
        ).getAbsolutePath(), folder);
        if (!f.exists()) {
            f.mkdirs();
        }
        String fileName = "flight_record_"+(new Long(System.currentTimeMillis()/1000)).toString()+".csv";
        try {
            FileOutputStream out = openFileOutput(fileName, Context.MODE_PRIVATE);
            out.write((csvFlightData.toString().getBytes()));
            out.close();
            File fileLocation = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS
            ).getAbsolutePath()+"/PIGSrecords", fileName);

            FileOutputStream fos = new FileOutputStream(fileLocation);
            fos.write(csvFlightData.toString().getBytes());
            Toast.makeText(dronesAutoActivity.this, "Create CSV Succeed", Toast.LENGTH_SHORT).show();
            /*
            Uri path = Uri.fromFile(fileLocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "output data"));
            */

        } catch (IOException e) {
            e.printStackTrace();
            Log.w(TAG, "makeCSV: "+e.toString());
        }

    }

    public void altitudeModifyDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(dronesAutoActivity.this);

        builder.setMessage("Set new Altitude for drones")
                .setTitle("Altitude Modification");

        LayoutInflater inflater = this.getLayoutInflater();
        View view=inflater.inflate(R.layout.altitude_dialog,null);
        builder.setView(view);
        SeekBar seekBar = (SeekBar) view.findViewById(R.id.seekBar);
        TextView seekBarText = (TextView) view.findViewById(R.id.seekBarText);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarText.setText("Current Value:" + progress);
                altitudeValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        // Add the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                SEND_GPS_ALT = String.format("%06d",altitudeValue);
                Toast.makeText(dronesAutoActivity.this, "Altitude Set to: "+SEND_GPS_ALT, Toast.LENGTH_SHORT).show();
                // User clicked OK button
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Set other dialog properties

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void initModel(String modelName) {
        // 加载模型
        try {
            this.yolov5TFLiteDetector = new Yolov5TFLiteDetector();
            this.yolov5TFLiteDetector.setModelFile(modelName);
//            this.yolov5TFLiteDetector.addNNApiDelegate();
            this.yolov5TFLiteDetector.addGPUDelegate();
            this.yolov5TFLiteDetector.initialModel(this);
            Log.i("model", "Success loading model" + this.yolov5TFLiteDetector.getModelFile());
        } catch (Exception e) {
            Log.e("image", "load model error: " + e.getMessage() + e.toString());
        }
    }

    private void YOLODetection(){

        long start = System.currentTimeMillis();
        int imageHeight = flc.getHeight();
        int imageWidth = flc.getWidth();

        Bitmap imageBitmap = Bitmap.createBitmap(droneCam2.getWidth(), droneCam2.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas bitmapHolder = new Canvas(imageBitmap);
        droneCam2.draw(bitmapHolder);

        // 原图bitmap
        //Bitmap imageBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        //imageBitmap.setPixels(rgbBytes, 0, imageWidth, 0, 0, imageWidth, imageHeight);

        // 模型输入的bitmap
        Matrix previewToModelTransform =
                imageProcess.getTransformationMatrix(
                        imageBitmap.getWidth(), imageBitmap.getHeight(),
                        yolov5TFLiteDetector.getInputSize().getWidth(),
                        yolov5TFLiteDetector.getInputSize().getHeight(),
                        0, false);
        Bitmap modelInputBitmap = Bitmap.createBitmap(imageBitmap, 0, 0,
                imageBitmap.getWidth(), imageBitmap.getHeight(),
                previewToModelTransform, false);

        Matrix modelToPreviewTransform = new Matrix();
        previewToModelTransform.invert(modelToPreviewTransform);

        ArrayList<Recognition> recognitions = yolov5TFLiteDetector.detect(modelInputBitmap);
//            ArrayList<Recognition> recognitions = yolov5TFLiteDetector.detect(imageBitmap);

        Bitmap emptyCropSizeBitmap = Bitmap.createBitmap(imageWidth, imageHeight,  Bitmap.Config.ARGB_8888);
        Canvas cropCanvas = new Canvas(emptyCropSizeBitmap);
//            Paint white = new Paint();
//            white.setColor(Color.WHITE);
//            white.setStyle(Paint.Style.FILL);
//            cropCanvas.drawRect(new RectF(0,0,previewWidth, previewHeight), white);
        // 边框画笔
        Paint boxPaint = new Paint();
        boxPaint.setStrokeWidth(5);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setColor(Color.RED);
        // 字体画笔
        Paint textPain = new Paint();
        textPain.setTextSize(50);
        textPain.setColor(Color.RED);
        textPain.setStyle(Paint.Style.FILL);

        for (Recognition res : recognitions) {
            RectF location = res.getLocation();
            String label = res.getLabelName();
            float confidence = res.getConfidence();
            modelToPreviewTransform.mapRect(location);
            cropCanvas.drawRect(location, boxPaint);
            cropCanvas.drawText(label + ":" + String.format("%.2f", confidence), location.left, location.top, textPain);
        }
        long end = System.currentTimeMillis();
        long costTime = (end - start);

        //set Bitmap
        boxLabelCanvas.setImageBitmap(emptyCropSizeBitmap);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStart() throws SecurityException {
        super.onStart();

        //Criteria criteria = new Criteria();

        // Getting the name of the best provider
        //String provider = locationManager.getBestProvider(criteria, true);

        // see what permissions are needed for this app)
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSION_ACCESS_COURSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    MY_PERMISSION_ACCESS_BACKGROUND_LOCATION);
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        //locationManager.registerGnssStatusCallback(gnssStatusCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            createCSVFile();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(dronesAutoActivity.this, "Create CSV Failed", Toast.LENGTH_SHORT).show();
        }
        ((ServerForPC) myThread).closeSocket();
        locationManager.removeUpdates(this);

        //mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
    }

}