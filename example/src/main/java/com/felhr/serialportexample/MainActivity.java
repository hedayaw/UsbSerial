package com.felhr.serialportexample;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Set;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.support.v7.app.AppCompatActivity;

import java.lang.Math;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

//start here
    private SensorManager sensorManagerA;
    private SensorManager sensorManagerG;
    private Sensor sensorA;
    private Sensor sensorG;

    private UsbManager usbManager;
    private UsbDevice deviceFound;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbInterface usbInterfaceFound = null;
    private UsbEndpoint endpointOut = null;
    private UsbEndpoint endpointIn = null;

    private Button collectButton;
    private Button stopButton;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;

    boolean startData;

    private String x_valueA;
    private String y_valueA;
    private String z_valueA;
    private String x_valueG;
    private String y_valueG;
    private String z_valueG;
//end here



    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private UsbService usbService;
    private TextView display;
    private EditText editText;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //start here


        sensorManagerA = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorA = sensorManagerA.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManagerA.registerListener(this, sensorA, SensorManager.SENSOR_DELAY_NORMAL);

        sensorManagerG = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorG = sensorManagerG.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManagerG.registerListener(this, sensorG, SensorManager.SENSOR_DELAY_NORMAL);

        collectButton = findViewById(R.id.button2);
        collectButton.setOnClickListener(new View.OnClickListener() {

          @Override
          public void onClick(View v) {
            startData = true;
          }
        });
        stopButton = findViewById(R.id.button3);
        stopButton.setOnClickListener(new View.OnClickListener() {
          @Override
        	public void onClick(View v) {
        		startData = false;
        	}
        });

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mHandler = new MyHandler(this);
        //end here

        display = (TextView) findViewById(R.id.textView4);
        editText = (EditText) findViewById(R.id.editText1);
        Button sendButton = (Button) findViewById(R.id.button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editText.getText().toString().equals("")) {
                    String data = editText.getText().toString();
                if (usbService != null) { // if UsbService was correctly binded, Send data
                    usbService.write(data.getBytes());
                }
                }
            }
        });
    }

    @Override
	   public void onSensorChanged(SensorEvent sensorEvent) {
		     Sensor mySensor = sensorEvent.sensor;

		if (startData) {
			if ((mySensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
				float x = sensorEvent.values[0];
				float y = sensorEvent.values[1];
				float z = sensorEvent.values[2];

				String x_valueA = String.valueOf(x);
				String y_valueA = String.valueOf(y);
				String z_valueA = String.valueOf(z);

				final TextView xStringValueA = findViewById(R.id.textView5);
				xStringValueA.setText(x_valueA);

				final TextView yStringValueA = findViewById(R.id.textView6);
				yStringValueA.setText(y_valueA);

				final TextView zStringValueA = findViewById(R.id.textView7);
				zStringValueA.setText(z_valueA);

				sendCommand(x_valueA, y_valueA, z_valueA);
			}

			if (timestamp != 0) {
				final float dT = (sensorEvent.timestamp - timestamp) * NS2S;
				float axisX = sensorEvent.values[0];
				float axisY = sensorEvent.values[1];
				float axisZ = sensorEvent.values[2];

				String x_valueG = String.valueOf(axisX);
				String y_valueG = String.valueOf(axisY);
				String z_valueG = String.valueOf(axisZ);

				final TextView xStringValue = findViewById(R.id.textView14);
				xStringValue.setText(x_valueG);

				final TextView yStringValue = findViewById(R.id.textView15);
				yStringValue.setText(y_valueG);

				final TextView zStringValue = findViewById(R.id.textView16);
				zStringValue.setText(z_valueG);

				sendCommand(x_valueG, y_valueG, z_valueG);

				float omegaMagnitude = (float) Math.sqrt((axisX * axisX) + (axisY * axisY) + (axisZ * axisZ));

				float thetaOverTwo = omegaMagnitude * dT / 2.0f;
				float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
				float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
				deltaRotationVector[0] = sinThetaOverTwo * axisX;
				deltaRotationVector[1] = sinThetaOverTwo * axisY;
				deltaRotationVector[2] = sinThetaOverTwo * axisZ;
				deltaRotationVector[3] = cosThetaOverTwo;
			}

			timestamp = sensorEvent.timestamp;
			float[] deltaRotationMatrix = new float[9];
			SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);

		} else {
			final TextView xStringValueA = findViewById(R.id.textView5);
			xStringValueA.setText("--");

			final TextView yStringValueA = findViewById(R.id.textView6);
			yStringValueA.setText("--");

			final TextView zStringValueA = findViewById(R.id.textView7);
			zStringValueA.setText("--");

			final TextView xStringValue = findViewById(R.id.textView14);
			xStringValue.setText("--");

			final TextView yStringValue = findViewById(R.id.textView15);
			yStringValue.setText("--");

			final TextView zStringValue = findViewById(R.id.textView16);
			zStringValue.setText("--");
		}
	}

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it

		    sensorManagerA.registerListener(this, sensorA, SensorManager.SENSOR_DELAY_NORMAL);
		    sensorManagerG.registerListener(this, sensorG, SensorManager.SENSOR_DELAY_NORMAL);

		    Intent intent = getIntent();
		    String action = intent.getAction();

		    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			     setDevice(device);
		    } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			       if (deviceFound != null && deviceFound.equals(device)) {
				         setDevice(null);
			       }
		    }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
        sensorManagerA.unregisterListener(this);
		    sensorManagerG.unregisterListener(this);
    }

    private void setDevice(UsbDevice device) {
		usbInterfaceFound = null;
		endpointOut = null;
		endpointIn = null;

		for (int i = 0; i < device.getInterfaceCount(); i++) {
			UsbInterface usbif = device.getInterface(i);

			UsbEndpoint tOut = null;
			UsbEndpoint tIn = null;

			int tEndpointCnt = usbif.getEndpointCount();
			if (tEndpointCnt >= 2) {
				for (int j = 0; j < tEndpointCnt; j++) {
					if (usbif.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
						if (usbif.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_OUT) {
							tOut = usbif.getEndpoint(j);
						} else if (usbif.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_IN) {
							tIn = usbif.getEndpoint(j);
						}
					}
				}

				if (tOut != null && tIn != null) {
					usbInterfaceFound = usbif;
					endpointOut = tOut;
					endpointIn = tIn;
				}
			}
		}

		if (usbInterfaceFound == null) {
			return;
		}

		deviceFound = device;

		if (device != null) {
			UsbDeviceConnection connection = usbManager.openDevice(device);
			if (connection != null && connection.claimInterface(usbInterfaceFound, true)) {
				usbDeviceConnection = connection;
				Thread thread = new Thread((Runnable) this); //WHY
				thread.start();
			} else {
				usbDeviceConnection = null;
			}
		}
	}

	private void sendCommand(String x, String y, String z) { //(int control)
		synchronized (this) {
			if (usbDeviceConnection != null) {
				String data = x + " " + y + " " + z;
				byte[] message = data.getBytes();
//				message[0] = (byte)control;

				usbDeviceConnection.bulkTransfer(endpointOut, message, message.length, 0);
			}
		}
	}

	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		UsbRequest request = new UsbRequest();
		request.initialize(usbDeviceConnection, endpointIn);
		while (true) {
			request.queue(buffer, 1);
			if(usbDeviceConnection.requestWait() == request) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			} else {
				break;
			}
		}
	}

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    mActivity.get().display.append(data);
                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}
