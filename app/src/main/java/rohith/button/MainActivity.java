package rohith.button;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;


public class MainActivity extends Activity {
    TextView textInfo;
    /*TextView textInfoInterface;
    TextView textEndPoint;

    TextView textDeviceName; */
    TextView textStatus;

   /* Spinner spInterface;
    ArrayList<String> listInterface;
    ArrayList<UsbInterface> listUsbInterface;
    ArrayAdapter<String> adapterInterface;

    Spinner spEndPoint;
    ArrayList<String> listEndPoint;
    ArrayList<UsbEndpoint> listUsbEndpoint;
    ArrayAdapter<String> adapterEndpoint; */

    private static final int targetVendorID= 0x10C4;
    private static final int targetProductID = 0xEA60;
    UsbDevice deviceFound = null;
    UsbSerialDevice serialPort;
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                textInfo.setText("");
                textInfo.setText(data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();

            }


        }
    };

    private static final String ACTION_USB_PERMISSION =
            "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    UsbInterface usbInterface;
    UsbDeviceConnection usbDeviceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = (TextView)findViewById(R.id.textstatus);

       /* textDeviceName = (TextView)findViewById(R.id.textdevicename);
        spInterface = (Spinner)findViewById(R.id.spinnerinterface);
        spEndPoint = (Spinner)findViewById(R.id.spinnerendpoint); */
        textInfo = (TextView) findViewById(R.id.info);
       /* textInfoInterface = (TextView)findViewById(R.id.infointerface);
        textEndPoint = (TextView)findViewById(R.id.infoendpoint); */

        //register the broadcast receiver
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        connectUsb();
    }

    @Override
    protected void onDestroy() {
        releaseUsb();
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbDeviceReceiver);
        super.onDestroy();
    }

    private void connectUsb(){

        Toast.makeText(MainActivity.this,
                "Connect a USB Device",
                Toast.LENGTH_LONG).show();
        textStatus.setText("connectUsb()");

        checkDeviceInfo();
        if(deviceFound != null){
            doRawDescriptors();
        }
    }

    private void releaseUsb(){

        Toast.makeText(MainActivity.this,
                "releaseUsb()",
                Toast.LENGTH_LONG).show();
        textStatus.setText("releaseUsb()");

        if(usbDeviceConnection != null){
            if(usbInterface != null){
                usbDeviceConnection.releaseInterface(usbInterface);
                usbInterface = null;
            }
            usbDeviceConnection.close();
            usbDeviceConnection = null;
        }
        deviceFound = null;
    }

    private void doRawDescriptors(){
        UsbDevice deviceToRead = deviceFound;
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        Boolean permitToRead = manager.hasPermission(deviceToRead);

        if(permitToRead){
            doReadRawDescriptors(deviceToRead);
        }else{
            manager.requestPermission(deviceToRead, mPermissionIntent);
            Toast.makeText(MainActivity.this,
                    "Permission: " + permitToRead,
                    Toast.LENGTH_LONG).show();
            textStatus.setText("Permission: " + permitToRead);
        }
    }

    private final BroadcastReceiver mUsbReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (ACTION_USB_PERMISSION.equals(action)) {

                        Toast.makeText(MainActivity.this,
                                "ACTION_USB_PERMISSION",
                                Toast.LENGTH_LONG).show();
                        textStatus.setText("ACTION_USB_PERMISSION");

                        synchronized (this) {
                            UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                                if(device != null){
                                    doReadRawDescriptors(device);
                                }
                            }
                            else {
                                Toast.makeText(MainActivity.this,
                                        "permission denied for device " + device,
                                        Toast.LENGTH_LONG).show();
                                textStatus.setText("permission denied for device " + device);
                            }
                        }
                    }
                }
            };

    private final BroadcastReceiver mUsbDeviceReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {

                        deviceFound = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        Toast.makeText(MainActivity.this,
                                "ACTION_USB_DEVICE_ATTACHED: \n" /*+
                                        deviceFound.toString()*/,
                                Toast.LENGTH_LONG).show();
                        textStatus.setText("ACTION_USB_DEVICE_ATTACHED: \n"/* +
                                deviceFound.toString()*/);
                        checkDeviceInfoSkipDeviceSearching();
                        doRawDescriptors();
                    }else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {

                        UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                        Toast.makeText(MainActivity.this,
                                "ACTION_USB_DEVICE_DETACHED: \n" /*+
                                        device.toString()*/,
                                Toast.LENGTH_LONG).show();
                        textStatus.setText("ACTION_USB_DEVICE_DETACHED: \n" /*+
                                device.toString()*/);

                        if(device!=null){
                            if(device == deviceFound){
                                releaseUsb();
                            }
                        }

                        textInfo.setText("");
                        /*textInfoInterface.setText("");
                        textEndPoint.setText("");

                        listInterface.clear();
                        listUsbInterface.clear();
                        adapterInterface.notifyDataSetChanged();

                        listEndPoint.clear();
                        listUsbEndpoint.clear();
                        adapterEndpoint.notifyDataSetChanged(); */
                    }
                }

            };

    private void doReadRawDescriptors(UsbDevice device) {
        final int STD_USB_REQUEST_GET_DESCRIPTOR = 0x06;
        final int LIBUSB_DT_STRING = 0x03;

        boolean forceClaim = true;

        byte[] buffer = new byte[255];
        int indexManufacturer = 14;
        int indexProduct = 15;
        String stringManufacturer = "";
        String stringProduct = "";

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        usbDeviceConnection = manager.openDevice(device);
        serialPort = UsbSerialDevice.createUsbSerialDevice(device, usbDeviceConnection);
        if (usbDeviceConnection != null) {
            usbInterface = device.getInterface(0);
            usbDeviceConnection.claimInterface(usbInterface, forceClaim);

            byte[] rawDescriptors = usbDeviceConnection.getRawDescriptors();

            int lengthManufacturer = usbDeviceConnection.controlTransfer(
                    UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_STANDARD,
                    STD_USB_REQUEST_GET_DESCRIPTOR,
                    (LIBUSB_DT_STRING << 8) | rawDescriptors[indexManufacturer],
                    0,
                    buffer,
                    0xFF,
                    0);
            try {
                stringManufacturer = new String(buffer, 2, lengthManufacturer - 2, "UTF-16LE");
            } catch (UnsupportedEncodingException e) {
                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                textStatus.setText(e.toString());
            }

            int lengthProduct = usbDeviceConnection.controlTransfer(
                    UsbConstants.USB_DIR_IN | UsbConstants.USB_TYPE_STANDARD,
                    STD_USB_REQUEST_GET_DESCRIPTOR,
                    (LIBUSB_DT_STRING << 8) | rawDescriptors[indexProduct],
                    0,
                    buffer,
                    0xFF,
                    0);
            try {
                stringProduct = new String(buffer, 2, lengthProduct - 2, "UTF-16LE");
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            Toast.makeText(MainActivity.this,
                    "Manufacturer: " + stringManufacturer + "\n" +
                            "Product: " + stringProduct,
                    Toast.LENGTH_LONG).show();
            textStatus.setText("The Manufacturer: " + stringManufacturer + "\n" +
                    "The Product: " + stringProduct);
        } else {
            Toast.makeText(MainActivity.this,
                    "open failed",
                    Toast.LENGTH_LONG).show();
            textStatus.setText("open failed");
        }
        if (serialPort != null) {
            if (serialPort.open()) { //Set Serial Connection Parameters.
                serialPort.setBaudRate(115200);
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                textInfo.setText("Serial port opened ");
                //serialPort.read(mCallback);


            } else {

                Toast.makeText(MainActivity.this,
                        "device not communicating",
                        Toast.LENGTH_LONG).show();
                textStatus.setText("No Communication");
            }

        }
    }

    private void checkDeviceInfo() {

        deviceFound = null;

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();

            if(device.getVendorId()==targetVendorID){
                if(device.getProductId()==targetProductID){
                    deviceFound = device;

                }
            }
        }

        textInfo.setText("");
        //textInfoInterface.setText("");
        //textEndPoint.setText("");

        if(deviceFound==null){
            Toast.makeText(MainActivity.this,
                    "device not found",
                    Toast.LENGTH_LONG).show();
            textStatus.setText("No Device connected yet!");
        }else{
            String i = /*deviceFound.toString() + "\n" +*/
                    /*"DeviceID: " + deviceFound.getDeviceId() + "\n" +
                    "DeviceName: " + deviceFound.getDeviceName() + "\n" +
                    "DeviceClass: " + deviceFound.getDeviceClass() + " - "
                    + translateDeviceClass(deviceFound.getDeviceClass()) + "\n" +
                    "DeviceSubClass: " + deviceFound.getDeviceSubclass() + "\n" + */
                    "VendorID: " + deviceFound.getVendorId() + "\n" +
                    "ProductID: " + deviceFound.getProductId() /*+ "\n" +
                    "InterfaceCount: " + deviceFound.getInterfaceCount()*/;
            textInfo.setText(i);

           // checkUsbDevicve(deviceFound);
        }

    }

    private void checkDeviceInfoSkipDeviceSearching() {
        //called when ACTION_USB_DEVICE_ATTACHED,
        //device already found, skip device searching

        textInfo.setText("");
        //textInfoInterface.setText("");
        //textEndPoint.setText("");

        String i = /*deviceFound.toString() + "\n" + */
                /*"DeviceID: " + deviceFound.getDeviceId() + "\n" +
                "DeviceName: " + deviceFound.getDeviceName() + "\n" +
                "DeviceClass: " + deviceFound.getDeviceClass() + " - "
                + translateDeviceClass(deviceFound.getDeviceClass()) + "\n" +
                "DeviceSubClass: " + deviceFound.getDeviceSubclass() + "\n" + */
                "VendorID: " + deviceFound.getVendorId() + "\n" +
                "ProductID: " + deviceFound.getProductId()/* + "\n" +
                "InterfaceCount: " + deviceFound.getInterfaceCount() */ ;
        textInfo.setText(i);

      // checkUsbDevicve(deviceFound);

    }

    /* AdapterView.OnItemSelectedListener deviceOnItemSelectedListener =
            new AdapterView.OnItemSelectedListener(){

                @Override
                public void onItemSelected(AdapterView<?> parent,
                                           View view, int position, long id) {
                    UsbDevice device = deviceFound;

                    String i = device.toString() + "\n" +
                            "DeviceID: " + device.getDeviceId() + "\n" +
                            "DeviceName: " + device.getDeviceName() + "\n" +
                            "DeviceClass: " + device.getDeviceClass() + " - "
                            + translateDeviceClass(device.getDeviceClass()) + "\n" +
                            "DeviceSubClass: " + device.getDeviceSubclass() + "\n" +
                            "VendorID: " + device.getVendorId() + "\n" +
                            "ProductID: " + device.getProductId() + "\n" +
                            "InterfaceCount: " + device.getInterfaceCount();
                    textInfo.setText(i);

                   // checkUsbDevicve(device);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}

            }; */

    /*private void checkUsbDevicve(UsbDevice d) {
        //listInterface = new ArrayList<String>();
        //listUsbInterface = new ArrayList<UsbInterface>();

        for(int i=0; i<d.getInterfaceCount(); i++){
            UsbInterface usbif = d.getInterface(i);
           // listInterface.add(usbif.toString());
            //listUsbInterface.add(usbif);
        }

       /* adapterInterface = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, listInterface);
        adapterInterface.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spInterface.setAdapter(adapterInterface);
        spInterface.setOnItemSelectedListener(interfaceOnItemSelectedListener);
    } */

  /*  AdapterView.OnItemSelectedListener interfaceOnItemSelectedListener =
            new AdapterView.OnItemSelectedListener(){

                @Override
                public void onItemSelected(AdapterView<?> parent,
                                           View view, int position, long id) {

                    UsbInterface selectedUsbIf = listUsbInterface.get(position);

                    String sUsbIf = "\n" + selectedUsbIf.toString() + "\n"
                            + "Id: " + selectedUsbIf.getId() + "\n"
                            + "InterfaceClass: " + selectedUsbIf.getInterfaceClass() + "\n"
                            + "InterfaceProtocol: " + selectedUsbIf.getInterfaceProtocol() + "\n"
                            + "InterfaceSubclass: " + selectedUsbIf.getInterfaceSubclass() + "\n"
                            + "EndpointCount: " + selectedUsbIf.getEndpointCount();

                    textInfoInterface.setText(sUsbIf);
                    checkUsbInterface(selectedUsbIf);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}

            };

    private void checkUsbInterface(UsbInterface uif) {
        listEndPoint = new ArrayList<String>();
        listUsbEndpoint = new ArrayList<UsbEndpoint>();

        for(int i=0; i<uif.getEndpointCount(); i++){
            UsbEndpoint usbEndpoint = uif.getEndpoint(i);
            listEndPoint.add(usbEndpoint.toString());
            listUsbEndpoint.add(usbEndpoint);
        }

        adapterEndpoint = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, listEndPoint);
        adapterEndpoint.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEndPoint.setAdapter(adapterEndpoint);
        spEndPoint.setOnItemSelectedListener(endpointOnItemSelectedListener);
    }

    AdapterView.OnItemSelectedListener endpointOnItemSelectedListener =
            new AdapterView.OnItemSelectedListener(){

                @Override
                public void onItemSelected(AdapterView<?> parent,
                                           View view, int position, long id) {

                    UsbEndpoint selectedEndpoint = listUsbEndpoint.get(position);

                    String sEndpoint = "\n" + selectedEndpoint.toString() + "\n"
                            + translateEndpointType(selectedEndpoint.getType());

                    textEndPoint.setText(sEndpoint);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}

            }; */

    private String translateEndpointType(int type){
        switch(type){
            case UsbConstants.USB_ENDPOINT_XFER_CONTROL:
                return "USB_ENDPOINT_XFER_CONTROL (endpoint zero)";
            case UsbConstants.USB_ENDPOINT_XFER_ISOC:
                return "USB_ENDPOINT_XFER_ISOC (isochronous endpoint)";
            case UsbConstants.USB_ENDPOINT_XFER_BULK :
                return "USB_ENDPOINT_XFER_BULK (bulk endpoint)";
            case UsbConstants.USB_ENDPOINT_XFER_INT:
                return "USB_ENDPOINT_XFER_INT (interrupt endpoint)";
            default:
                return "unknown";
        }
    }

    private String translateDeviceClass(int deviceClass){
        switch(deviceClass){
            case UsbConstants.USB_CLASS_APP_SPEC:
                return "Application specific USB class";
            case UsbConstants.USB_CLASS_AUDIO:
                return "USB class for audio devices";
            case UsbConstants.USB_CLASS_CDC_DATA:
                return "USB class for CDC devices (communications device class)";
            case UsbConstants.USB_CLASS_COMM:
                return "USB class for communication devices";
            case UsbConstants.USB_CLASS_CONTENT_SEC:
                return "USB class for content security devices";
            case UsbConstants.USB_CLASS_CSCID:
                return "USB class for content smart card devices";
            case UsbConstants.USB_CLASS_HID:
                return "USB class for human interface devices (for example, mice and keyboards)";
            case UsbConstants.USB_CLASS_HUB:
                return "USB class for USB hubs";
            case UsbConstants.USB_CLASS_MASS_STORAGE:
                return "USB class for mass storage devices";
            case UsbConstants.USB_CLASS_MISC:
                return "USB class for wireless miscellaneous devices";
            case UsbConstants.USB_CLASS_PER_INTERFACE:
                return "USB class indicating that the class is determined on a per-interface basis";
            case UsbConstants.USB_CLASS_PHYSICA:
                return "USB class for physical devices";
            case UsbConstants.USB_CLASS_PRINTER:
                return "USB class for printers";
            case UsbConstants.USB_CLASS_STILL_IMAGE:
                return "USB class for still image devices (digital cameras)";
            case UsbConstants.USB_CLASS_VENDOR_SPEC:
                return "Vendor specific USB class";
            case UsbConstants.USB_CLASS_VIDEO:
                return "USB class for video devices";
            case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
                return "USB class for wireless controller devices";
            default: return "Unknown USB class!";

        }
    }

}