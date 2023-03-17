package com.huday.overwatchdrone;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class dronesAutoActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {
    final private static int MY_PERMISSION_ACCESS_COURSE_LOCATION = 114;
    final private static int MY_PERMISSION_ACCESS_FINE_LOCATION = 514;
    final private static int MY_PERMISSION_ACCESS_BACKGROUND_LOCATION = 1919;

    final static String drone1url = "http://192.168.0.100:8081/cam.mjpg";
    final static String drone2url = "http://192.168.0.209:8081/cam.mjpg";
    final static String drone3url = "http://192.168.0.101:8081/cam.mjpg";

    private GoogleMap mMap;
    LocationManager locationManager;

    private Marker currentLocation, curMarker, Drone1Marker, Drone2Marker, Drone3Marker;
    private LatLng cur, curLatLng, drone1LatLng, drone2LatLng, drone3LatLng;
    private boolean autoCenter = true;
    double lat, longt;

    private String ACK,BATTERY,GPS_LAT,GPS_LON,GPS_ALT,YAW,CON; //from drone
    private String COM,RADIUS,P_GPS_LAT,P_GPS_LON; //from phone

    String locationProvider;

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

    //Camera of drone
    private WebView droneCam1;
    private WebView droneCam2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drones_auto);

        //initializeLocationManager();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        final FloatingActionButton switchButton = (FloatingActionButton) this.findViewById(R.id.switchButton);
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
        //droneCam1.loadUrl("http://192.168.0.100:8081/cam.mjpg");
        droneCam2.loadUrl(drone1url);

        //Marker
        bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.mipmap.drone_marker);
        b = bitmapdraw.getBitmap();
        droneMapMarker = Bitmap.createScaledBitmap(b, width, height, false);

        bitmapDrone1 = (BitmapDrawable)getResources().getDrawable(R.mipmap.drone1);
        b1 = bitmapdraw.getBitmap();
        drone1Marker = Bitmap.createScaledBitmap(b1, width, height, false);

        bitmapDrone2 = (BitmapDrawable)getResources().getDrawable(R.mipmap.drone2);
        b2 = bitmapdraw.getBitmap();
        drone2Marker = Bitmap.createScaledBitmap(b2, width, height, false);

        bitmapDrone3 = (BitmapDrawable)getResources().getDrawable(R.mipmap.drone3);
        b3 = bitmapdraw.getBitmap();
        drone3Marker = Bitmap.createScaledBitmap(b3, width, height, false);


        //TEMP
        COM = "0";
        RADIUS = "000";
        GPS_ALT = "000003";
        GPS_LAT = "00000000000";
        GPS_LON = "00000000000";

        user_loc = (TextView) this.findViewById(R.id.user_loc);
        drone1_loc = (TextView) this.findViewById(R.id.drone_1_loc);
        drone2_loc = (TextView) this.findViewById(R.id.drone_2_loc);
        drone3_loc = (TextView) this.findViewById(R.id.drone_3_loc);
        batteryView = (TextView) this.findViewById(R.id.batteryView);

        debug1 = (TextView) this.findViewById(R.id.textDebug1);
        debug2 = (TextView) this.findViewById(R.id.textDebug2);
        debug3 = (TextView) this.findViewById(R.id.textDebug3);
        debug4 = (TextView) this.findViewById(R.id.textDebug4);

        Thread myThread = new Thread(new ServerForPC(1919));
        myThread.start();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map2);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(dronesAutoActivity.this, dronesManualActivity.class);
                //startActivity(intent);
                autoCenter = !autoCenter;
                Toast.makeText(dronesAutoActivity.this, "Auto center switched", Toast.LENGTH_LONG).show();
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

                Toast.makeText(dronesAutoActivity.this, "Switch to drone 1 camera", Toast.LENGTH_SHORT).show();
            }
        });

        bcsw2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                droneCam2.loadUrl(drone2url);

                Toast.makeText(dronesAutoActivity.this, "Switch to drone 2 camera", Toast.LENGTH_SHORT).show();
            }
        });

        bcsw3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                droneCam2.loadUrl(drone3url);

                Toast.makeText(dronesAutoActivity.this, "Switch to drone 3 camera", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void changeBatteryInfo(String num){
        batteryView.setText("Battery Level: "+num+"%");
    }

    public String commandPack(String command){
        switch (command){
            case "LAND": COM = "0"; break;
            case "TAKEOFF": COM = "1"; break;
            case "STOP": COM = "2"; break;
            case "ORBIT": COM = "3"; break;
            case "EXPLORE": COM = "4"; break;
            case "RADAR": COM = "5"; break;
            case "TRACKING": COM = "6"; break;
            default: COM = "0"; break;
        }
        return COM+GPS_LAT+GPS_LON+GPS_ALT+RADIUS+P_GPS_LAT+P_GPS_LON;
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
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        // Update user location
        user_loc.setText(String.format("%.4f",latitude)+","+String.format("%.4f",longitude));
        P_GPS_LON = (Double.toString(longitude)+"00000").substring(0,11);
        P_GPS_LAT = (Double.toString(latitude)+"00000").substring(0,11);
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
        P_GPS_LON = String.format("%.8f",longitude).substring(0,11);
        P_GPS_LAT = String.format("%.8f",latitude).substring(0,11);

        cur = new LatLng(latitude, longitude);
        currentLocation = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).position(cur).title("User Location"));

        if (autoCenter) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(cur));
        }
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

    private void updateInfo(String msg){
        //Toast.makeText(dronesAutoActivity.this, msg, Toast.LENGTH_SHORT).show();
        //Battery Type
        if(msg.length()<39) { //Invalid Input
            ACK = "INVALID INPUT";
            return;
        }
        ACK = msg.substring(0,1);
        BATTERY = msg.substring(1,4);
        GPS_LAT = msg.substring(4,15);
        GPS_LON = msg.substring(15,26);
        GPS_ALT = msg.substring(26,32);
        YAW = msg.substring(32,38);
        CON = msg.substring(38,39);
        // processing data
        //try
        //lat = Double.parseDouble(GPS_LAT);
        //longt = Double.parseDouble(GPS_LON);
        // Debug Info
        debug1.setText("ACK: "+ACK);
        debug2.setText("CON: "+CON);
        debug3.setText("GPS_LAT: "+GPS_LAT);
        debug4.setText("GPS_LON: "+GPS_LON);
        changeBatteryInfo(BATTERY);
    }

    //update Location on Google Map based on the Location Info
    private void updateLocationOnMap(String loc){
        String type = loc.substring(0,1);
        String latstr = loc.substring(1,12);
        String longtstr = loc.substring(13);
        lat = Double.parseDouble(latstr);
        longt = Double.parseDouble(longtstr);

        switch (type){
            case "a":{ //Drone 1
                drone1LatLng = new LatLng(lat,longt);
                drone1_loc.setText(latstr.substring(0,8) + "," + longtstr.substring(0,8));
                if(Drone1Marker!=null)
                    Drone1Marker.remove();
                Drone1Marker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(drone1Marker))
                        .snippet("Drone 1 Location")
                        .position(drone1LatLng).title("Current Drone 1 Location"));
                break;
            }
            case "b":{ //Drone 2
                drone2LatLng = new LatLng(lat,longt);
                drone2_loc.setText(latstr.substring(0,8) + "," + longtstr.substring(0,8));
                if(Drone2Marker!=null)
                    Drone2Marker.remove();
                Drone2Marker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(drone2Marker))
                        .snippet("Drone 2 Location")
                        .position(drone2LatLng).title("Current Drone 2 Location"));
                break;
            }
            case "c":{ //Drone 3
                drone3LatLng = new LatLng(lat,longt);
                drone3_loc.setText(latstr.substring(0,8) + "," + longtstr.substring(0,8));
                if(Drone3Marker!=null)
                    Drone3Marker.remove();
                Drone3Marker = mMap.addMarker(new MarkerOptions()
                        .icon(BitmapDescriptorFactory.fromBitmap(drone3Marker))
                        .snippet("Drone 3 Location")
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

        locationManager.removeUpdates(this);
        //mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
    }

}