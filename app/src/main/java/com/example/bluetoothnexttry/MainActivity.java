package com.example.bluetoothnexttry;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView mReadBuffer;
    private TextView mCurrentCOne;
    private TextView mCurrentCTwo;
    private TextView mCurrentHFConfigK;
    private TextView mCurrentHFConfigUp;
    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private Button mDiscoverBtn;
    private Button mRefreshBtn;
    private Button mStartBtn;
    private Button mStopBtn;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;
    private Button mSendCommandBtn;
    private EditText mCommand;
    private EditText mCalibrationOne;
    private EditText mCalibrationTwo;
    private EditText mHFConfigK;
    private EditText mHFConfigUp;

    private Handler mHandler; // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    private String header;


    public class pubStructs {
        public File filepath;
        public File getFilepath(){
            return filepath;
        }
    }
    public pubStructs pB;

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        mReadBuffer = (TextView) findViewById(R.id.readBuffer);
        mCurrentCOne = (TextView)findViewById(R.id.currentCOneTextView);
        mCurrentCTwo = (TextView)findViewById(R.id.currentCTwoTextView);
        mCurrentHFConfigK = (TextView)findViewById(R.id.currentHFConfigK);
        mCurrentHFConfigUp = (TextView)findViewById(R.id.currentHFConfigUp);
        mRefreshBtn = (Button)findViewById(R.id.refreshButton);
        mScanBtn = (Button)findViewById(R.id.scan);
        mOffBtn = (Button)findViewById(R.id.off);
        mDiscoverBtn = (Button)findViewById(R.id.discover);
        mListPairedDevicesBtn = (Button)findViewById(R.id.PairedBtn);
        mSendCommandBtn = (Button)findViewById(R.id.sendCommand);
        mStartBtn = (Button)findViewById(R.id.startButton);
        mStopBtn = (Button)findViewById(R.id.stopButton);
        mCalibrationOne = (EditText)findViewById(R.id.calibrationOne);
        mCalibrationTwo = (EditText)findViewById(R.id.calibrationTwo);
        mHFConfigK = (EditText)findViewById(R.id.hfConfigK);
        mHFConfigUp = (EditText)findViewById(R.id.hfConfigUp);

        mBTArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        createLogFile();

        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "ASCII");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    int previousIndex = 0;
                    for (int index = readMessage.indexOf(0xA);
                         index >= 0;
                         index = readMessage.indexOf(0xA, index + 1)){
                        String message = readMessage.substring(previousIndex, index);
                        if(message.length() > 2){
                            refreshInformation(message);
                        }

                        previousIndex = index -1;
                    }

                    mReadBuffer.setText(readMessage);
                    Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();;
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        mBluetoothStatus.setText("Connection Failed");
                }
            }
        };

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

            mSendCommandBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if(mConnectedThread != null) { //First check to make sure thread created
                        if(mCalibrationOne.getText().toString().length() != 0){
                            String tmp = "10" + mCalibrationOne.getText().toString();
                            mConnectedThread.write(tmp);
                        }
                        if(mCalibrationTwo.getText().toString().length() != 0){
                            String tmp = "11" + mCalibrationTwo.getText().toString();
                            mConnectedThread.write(tmp);
                        }
                        if(mHFConfigK.getText().toString().length() != 0){
                            String tmp = "13" + mHFConfigK.getText().toString();
                            mConnectedThread.write(tmp);
                        }
                        if(mHFConfigUp.getText().toString().length() != 0){
                            String tmp = "14" + mHFConfigUp.getText().toString();
                            mConnectedThread.write(tmp);
                        }
                    }
                }
            });

            mStartBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mConnectedThread.write("120");
                }
            });

            mStopBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mConnectedThread.write("121");
                }
            });
            
            mRefreshBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if(mConnectedThread != null) { //First check to make sure thread created

                        mConnectedThread.write("00");
                        mConnectedThread.write("01");
                        mConnectedThread.write("02");
                        mConnectedThread.write("03");
                        mConnectedThread.write("04");
                    }
                }
            });

            mScanBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });

            mOffBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    bluetoothOff(v);
                }
            });

            mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    listPairedDevices(v);
                }
            });

            mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover(v);
                }
            });
        }
    }


    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();

        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on", Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data){
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                mBluetoothStatus.setText("Enabled");
            }
            else
                mBluetoothStatus.setText("Disabled");
        }
    }

    public void writeToFile(String mySituation, String myMessage)
    {
        if(!pB.filepath.exists()){
            createLogFile();
        }
        try {
            FileWriter writer = new FileWriter(pB.filepath);
            writer.append(mySituation + "\t" + myMessage + "\t" + DateFormat.format("ss-mm-kk-dd-MM-yyyy",System.currentTimeMillis()).toString());
            writer.flush();
            writer.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public void createLogFile()
    {
        try {
            header = DateFormat.format("MM-dd-yyyyy-h-mmssaa", System.currentTimeMillis()).toString();
            File root = new File(Environment.getExternalStorageDirectory(), "EierLogs");
            if (!root.exists()) {
                root.mkdirs();
            }
            pB.filepath = new File(root, header + ".txt");
        }
        catch (Exception e)  {
            e.printStackTrace();

        }
    }

    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off", Toast.LENGTH_SHORT).show();
    }

    private void discover(View view){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){
            mBTAdapter.cancelDiscovery();
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(getApplicationContext(), "New Device found", Toast.LENGTH_SHORT).show();
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

            if(!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0,info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread()
            {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if(fail == false) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            byte[] toSend = new byte[1024];
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                        bytes = mmInStream.available(); // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        /*int lastMarker = 0;
                        int marker = 0;
                        for(int i=0; i< buffer.length; i++){
                            toSend[i - marker] = buffer[i];
                            if(buffer[i] == 0xA){
                                marker = i;



                                mHandler.obtainMessage(MESSAGE_READ, marker - lastMarker, -1, toSend)
                                        .sendToTarget(); // Send the obtained bytes to the UI activity
                                java.util.Arrays.fill(toSend, (byte)0x0);
                                lastMarker = marker;
                                marker++;
                            }
                        }*/
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
                mmOutStream.write(0xA);
                mmOutStream.flush();
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }

    }
    public void refreshInformation(String message){
        switch (message.substring(0,1))
        {
            case "0":
                mCurrentCOne.setText(message.substring(1));
                writeToFile("Config 1:", message.substring(1));
                break;
            case "1":
                mCurrentCTwo.setText(message.substring(1));
                writeToFile("Config 2:", message.substring(1));
                break;
            case "2":
                break;
            case "3":
                mCurrentHFConfigK.setText(message.substring(1));
                writeToFile("HF Config K:", message.substring(1));
                break;
            case "4":
                mCurrentHFConfigUp.setText(message.substring(1));
                writeToFile("HF Config Up:", message.substring(1));
                break;
            case "5":
                break;
            default:
                /*Some textView Showing an Error has happened*/
                break;
        }
    }
}
