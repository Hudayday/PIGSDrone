package com.huday.overwatchdrone;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.InetAddresses;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class wifiDirectActivity extends AppCompatActivity {

    Button btnOnOff, btnDiscover, btnSendMessage;
    ListView listView;
    TextView read_msg_box, connectionStatus;
    EditText msg, IP;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    tcpClient mTcpClient;
    tcpServer mTcpServer;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    Socket socket;

    ServerClas serverClas;

    boolean isHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_direct);
        initialize();
        exqListner();

        Thread myThread = new Thread(new ServerClas(1919));
        myThread.start();

        Thread myDroneThread1 = new Thread(new ServerClasTest(1920));
        myDroneThread1.start();

        //Thread myDroneThread2 = new Thread(new ServerClas(1921));
        //myDroneThread2.start();
        //new serverTask().execute("");
    }

    private void exqListner() {
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ConnectTask().execute("");
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(wifiDirectActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Discovery fails to Started");
                    }
                });
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice device = deviceArray[position];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                if (ActivityCompat.checkSelfPermission(wifiDirectActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Connected: "+device.deviceAddress);
                    }

                    @Override
                    public void onFailure(int i) {
                        connectionStatus.setText("Connection Fails!");
                    }
                });
            }
        });

        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                    ExecutorService executor = Executors.newSingleThreadExecutor();
//                    String mes = msg.getText().toString();
//                    executor.execute(new Runnable() {
//                        @Override
//                        public void run() {
//                            if(mes!=null && isHost){
//                                try {
//                                    serverClas.write(mes.getBytes());
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }else if(mes!=null && !isHost){
//                                try {
//                                    clientClass.write(mes.getBytes());
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                        }
//                    });
                buttonClick();
            }
        });
    }

    public void buttonClick(){
        if (mTcpClient != null) {
            mTcpClient.sendMessage(msg.getText().toString());
        }
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
            mTcpClient.run();

            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //response received from server

            Log.d("test", "response " + values[0]);
            read_msg_box.setText(values[0]);
            //process server response here....

        }
    }


    class MyServer implements Runnable{

        ServerSocket ss;
        Socket mySocket;
        DataInputStream dis;
        //BufferedReader bufferedReader;
        String message;
        Handler handler = new Handler();


        @Override
        public void run() {
            try {
                ss = new ServerSocket(1919);

                while(true){
                    mySocket = ss.accept();
                    dis = new DataInputStream(mySocket.getInputStream());
                    message = dis.readLine();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //message = dis.readUTF();
                            read_msg_box.setText(message);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initialize(){
        btnOnOff = (Button)findViewById(R.id.onOff);
        btnDiscover = (Button)findViewById(R.id.discover);
        btnSendMessage = (Button)findViewById(R.id.sendButton);
        listView = (ListView)findViewById(R.id.wifiList);
        read_msg_box = (TextView)findViewById(R.id.readMsg);
        connectionStatus = (TextView)findViewById(R.id.wifiStatus);
        msg = (EditText)findViewById(R.id.sendMsg);
        IP = (EditText)findViewById(R.id.ipText);

        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
            if(!wifiP2pDeviceList.equals(peers)){
                peers.clear();
                peers.addAll(wifiP2pDeviceList.getDeviceList());

                int c = wifiP2pDeviceList.getDeviceList().size();
                deviceNameArray = new String[c];
                deviceArray = new WifiP2pDevice[c];

                int index = 0;
                for(WifiP2pDevice device : wifiP2pDeviceList.getDeviceList()){
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index ++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                listView.setAdapter(adapter);

                if(peers.size() == 0){
                    connectionStatus.setText("No Device Found");
                    return;
                }
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        //registerReceiver(mReceiver,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mReceiver);

    }

    public class ServerClas extends Thread {
        ServerSocket serverSocket;
        private InputStream inputStream;
        private OutputStream outputStream;
        private int port;

        ServerClas(int PORT) {
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
                                        connectionStatus.setText(tempMSG);
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

    public class ServerClasTest extends Thread{
        ServerSocket serverSocket;
        private InputStream inputStream;
        private OutputStream outputStream;
        private int port;

        ServerClasTest(int PORT){
            this.port = PORT;
        }

        public void write(byte[] bytes) throws IOException {
            outputStream.write(bytes);
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(1920);
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
                private BufferedReader mBufferIn;
                @Override
                public void run() {
                    byte[] buffer = new byte[1024];
                    int bytes;


                    while (socket!=null){
                        try {
                            bytes = inputStream.read(buffer);
                            mBufferIn = new BufferedReader(new InputStreamReader(inputStream));
                            if(bytes > 0){
                                int finalBytes = bytes;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        String tempMSG = null;
                                        try {
                                            tempMSG = mBufferIn.readLine();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        //String tempMSG = new String(buffer);
                                        read_msg_box.setText(tempMSG);
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

}