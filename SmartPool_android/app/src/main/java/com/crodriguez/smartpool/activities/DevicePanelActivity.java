package com.crodriguez.smartpool.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crodriguez.smartpool.R;
import com.crodriguez.smartpool.SmartPoolDevice;
import com.crodriguez.smartpool.services.BluetoothLeService;
import com.crodriguez.smartpool.utils.GattAttributes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class DevicePanelActivity extends AppCompatActivity {
    private static final String TAG = DevicePanelActivity.class.getSimpleName();

    public static SmartPoolDevice EXTRAS_DEVICE = null;
    private int mDeviceId;
    private String mDeviceName;
    private String mDeviceAddress;
    private String mDeviceSSID;
    private String mDeviceSSIDpass;

    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicTXtemp;
    private BluetoothGattCharacteristic characteristicRXtemp;
    private BluetoothGattCharacteristic characteristicTXturbity;
    private BluetoothGattCharacteristic characteristicRXturbity;
    private BluetoothGattCharacteristic characteristicTXname;
    private BluetoothGattCharacteristic characteristicRXname;
    private BluetoothGattCharacteristic characteristicTXph;
    private BluetoothGattCharacteristic characteristicRXph;
    private BluetoothGattCharacteristic characteristicTXdate;
    private BluetoothGattCharacteristic characteristicRXdate;
    private Context context;

    private Boolean activado;

    private ProgressDialog progresReconect;
    private ProgressBar protemp;
    private ProgressBar proturbi;
    private ProgressBar proph;
    private TextView temp;
    private TextView turbi;
    private TextView ph;
    private TextView date;
    private ProgressBar progDate;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private FirebaseDatabase database;

    // Código para gestionar el ciclo de vida del servicio
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Se conecta automáticamente al dispositivo cuando la inicialización se realiza correctamente.
            mBluetoothLeService.connect(mDeviceAddress);
            Log.e(TAG, "conectado"+DevicePanelActivity.class.getSimpleName());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };



  /*
  *     Método para manejar los eventos disparados por el Servicio.
  *     ACTION_GATT_CONNECTED: conectado a un servidor GATT.
  *     ACTION_GATT_DISCONNECTED: desconectado de un servidor GATT.
  *     ACTION_GATT_SERVICES_DISCOVERED: descubrió los servicios del GATT.
  *     ACTION_DATA_AVAILABLE: datos recibidos del dispositivo.
  *         Esto puede ser el resultado de operaciones de lectura o de aviso.
  */

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                try {
                    progresReconect.dismiss();
                }catch (Exception e){}
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());;
//                Log.i("ServiciosBLE","Recorremos los servicios:");
                for (BluetoothGattService service : mBluetoothLeService.getSupportedGattServices()) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String uuid = characteristic.getUuid().toString();
                        //Log.i("ServiciosBLE","UUID ->"+ uuid);
                        try {
                            if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_TX)) {

                               // Log.i("Debug", "Estamos escribiendo la caracteristica de tx");
                                characteristicTX = characteristic;

                            } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_TX_TEMP)) {

                              //  Log.i("Debug", "Estamos escribiendo la caracteristica de Temperatura");
                                characteristicTXtemp = characteristic;

                            } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_TX_TURBI)) {

                                characteristicTXturbity = characteristic;

                            } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_TX_PH)) {

                             //   Log.i("Debug", "Estamos escribiendo la caracteristica de PH");
                                characteristicTXph = characteristic;

                            } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_TX_DATE)) {

                               // Log.i("Debug", "Estamos escribiendo la caracteristica de date");
                                characteristicTXdate = characteristic;

                            } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_TX_NAME)) {

                                //Log.i("Debug", "Estamos escribiendo la caracteristica de name");
                                characteristicTXname = characteristic;

                            } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_RX_TEMP)) {

                                characteristicRXtemp = characteristic;
                              //  Log.i("Debug", "Estamos leyendo la caracteristica de sen temperatura");
                                mBluetoothLeService.readCharacteristic(characteristic);

                            } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_RX_TURBI)) {

                                //Log.i("Debug1", "Estamos leyendo la caracteristica de sen turbidez");
                                characteristicRXturbity = characteristic;

                                mBluetoothLeService.readCharacteristic(characteristic);

                            }else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_RX_PH)) {

                               // Log.i("Debug", "Estamos leyendo la caracteristica de PH");
                                characteristicRXph = characteristic;
                                mBluetoothLeService.readCharacteristic(characteristic);

                            }else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_RX_DATE)) {

                               // Log.i("Debug", "Estamos leyendo la caracteristica de date");
                                characteristicRXdate = characteristic;
                                mBluetoothLeService.readCharacteristic(characteristic);

                            } else if (uuid.equals(GattAttributes.UUID_CHARACTERISTIC_RX_NAME)) {
                               // Log.i("Debug1", "Estamos leyendo la caracteristica de name");
                                characteristicRXname = characteristic;
                                mBluetoothLeService.readCharacteristic(characteristic);
                            }
                        } catch (Exception ignore) { }
                    }
                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String extraUuid = intent.getStringExtra(BluetoothLeService.EXTRA_UUID);
                int extraType = intent.getIntExtra(BluetoothLeService.EXTRA_TYPE, -1);
                byte[] extraData = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                    StringBuilder stringBuilder = new StringBuilder(extraData.length);
                    try {
                        for (byte byteChar : extraData)
                            stringBuilder.append(String.format("%c", byteChar));
                    }catch (Exception e){Log.i(TAG,"Error al leer caracter con tilde");}

                    if (extraUuid.equals(GattAttributes.UUID_CHARACTERISTIC_RX_TEMP)) {
                        temp.setText(stringBuilder.toString());
                        protemp.setVisibility(View.GONE);
                    }
                    if (extraUuid.equals(GattAttributes.UUID_CHARACTERISTIC_RX_TURBI)) {
                        turbi.setText(stringBuilder.toString());
                        proturbi.setVisibility(View.GONE);
                    }
                    if (extraUuid.equals(GattAttributes.UUID_CHARACTERISTIC_RX_PH)) {
                        ph.setText(stringBuilder.toString());
                        proph.setVisibility(View.GONE);
                    }
                    if (extraUuid.equals(GattAttributes.UUID_CHARACTERISTIC_RX_DATE)) {
                        date.setText(convertirDate(stringBuilder.toString(),"HH:mm:ss",2));
                        progDate.setVisibility(View.GONE);
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_panel);

        context=this;

        EXTRAS_DEVICE = (SmartPoolDevice) getIntent().getExtras().getSerializable("dispositivo");
        mDeviceId= EXTRAS_DEVICE.getId_divice();
        mDeviceName = EXTRAS_DEVICE.getName();
        mDeviceAddress = EXTRAS_DEVICE.getMac();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mDeviceName);


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        activado=false;
        temp = (TextView) findViewById(R.id.tvTemperature);
        protemp = (ProgressBar) findViewById(R.id.progTemp);

        temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                protemp.setVisibility(View.VISIBLE);
                activado =!activado;
                if(mConnected)
                    notificationChange(characteristicRXtemp, activado);
                else
                    cargarDatos(temp,turbi,ph,date,protemp);

            }
        });
        turbi = (TextView) findViewById(R.id.tvTurbidity);
        proturbi = (ProgressBar) findViewById(R.id.progTurbi);

        turbi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                proturbi.setVisibility(View.VISIBLE);
                activado =!activado;
                if(mConnected)
                    notificationChange(characteristicRXturbity, activado);
                else
                    cargarDatos(temp,turbi,ph,date,proturbi);


            }
        });
        ph = (TextView) findViewById(R.id.tvPh);
        proph = (ProgressBar) findViewById(R.id.progPh);
        ph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                proph.setVisibility(View.VISIBLE);
                activado =!activado;
                if(mConnected)
                    notificationChange(characteristicRXph, activado);
                else
                    cargarDatos(temp,turbi,ph,date,proph);

            }
        });
        date = (TextView) findViewById(R.id.tvDate);
        progDate =(ProgressBar) findViewById(R.id.progDate);
        database = FirebaseDatabase.getInstance();
        date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progDate.setVisibility(View.VISIBLE);
                activado =!activado;
                if(mConnected)
                    notificationChange(characteristicRXdate, activado);
                else
                    cargarDatos(temp,turbi,ph,date,progDate);
            }
        });

        cargarDatos(temp,turbi,ph,date,null);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_panel_devices, menu);

        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_change_name:
                dialogo();
                return true;
            case R.id.menu_conf:
                dialogoWifi();
                return true;
            case R.id.menu_estadisticas:
                Intent i = new Intent(this, DeviceStatistics_Activity.class);
                i.putExtra("dispositivo", EXTRAS_DEVICE);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            mBluetoothLeService.disconnect();
        }catch (Exception e){
            Log.i(TAG, "No existía conexión: "+ e);
        }
        unregisterReceiver(mGattUpdateReceiver);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // mConnectionState.setText(resourceId);
            }
        });
    }

    /*
    *  Método para iterar a través de los Servicios/Características recibidos por el GATT.
    * */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, GattAttributes.lookup(uuid, unknownServiceString));

            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    //  Método para crear un Toast de forma rápida
    protected void toast(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    //Método para convertir una fecha unixtime en formato legible
    private String convertirDate(String date, String formato, int gtm){
        long unixSeconds = Long.parseLong(date);
        if(gtm!=0)
            unixSeconds-=gtm*3600;
        // convertimos segundos a milisegundos
        Date fecha = new java.util.Date(unixSeconds*1000L);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat(formato);
        return sdf.format(fecha);

    }

    //Método para obtener la lectura del último test guardado en Firebase
    private void cargarDatos(final TextView temp, final TextView turbi, final TextView ph, final TextView date, final ProgressBar progess) {

        database.getReference("Devices").child(EXTRAS_DEVICE.getMac()).orderByValue().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                temp.setText(dataSnapshot.getChildren().iterator().next().child("temp").child("valor").getValue().toString());
                turbi.setText(dataSnapshot.getChildren().iterator().next().child("turbi").child("valor").getValue().toString());
                ph.setText(dataSnapshot.getChildren().iterator().next().child("ph").child("valor").getValue().toString());
                date.setText(convertirDate(dataSnapshot.getChildren().iterator().next().getKey(),"dd-MM-yyyy HH:mm",0));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

        if(progess!=null)
            try {
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        progess.setVisibility(View.GONE);
                    }
                }, 2000);
            }catch (Exception e){}
    }

    /*
     *  Método que crea un diálogo, con el formulario correspondiente para cambiar el nombre
     *  al Bluetooth del dispositivo.
     *  Requiere de conexión previa con el dispositivo BLE para que se haga efectivo el cambio.
     */
    protected void dialogo() {

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        View mView = getLayoutInflater().inflate(R.layout.dialogo, null);
        final EditText nombre = (EditText) mView.findViewById(R.id.mChangeMame);
        Button btnGuardar = (Button) mView.findViewById(R.id.btn_guardar);
        Button botonAtras = (Button) mView.findViewById(R.id.boton_cancelar);
        final TextInputLayout tilnombreble=(TextInputLayout) mView.findViewById(R.id.til_nombre);
        mBuilder.setView(mView);
        nombre.setText(mDeviceName);
        final AlertDialog dialog = mBuilder.create();
        dialog.show();
        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference myRef = database.getReference("Usuarios").child(""+user.getUid());
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeviceName=nombre.getText().toString();
                String nom = tilnombreble.getEditText().getText().toString();
                final ProgressDialog progress;
                if(!mDeviceName.isEmpty())
                    if (mConnected ) {
                        makeChange(characteristicTXname,"ble"+mDeviceName);
                        myRef.child(""+mDeviceId).child("nombre").setValue(mDeviceName);
                        dialog.dismiss();
                        progress = ProgressDialog.show(DevicePanelActivity.this, "Reiniciando ...", "" + "Esto puede tardar unos segundos, por favor espere.");  //show a progress dialog
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                Intent i = getBaseContext().getPackageManager()
                                        .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                finish();
                                startActivity(i);
                                progress.dismiss();

                            }

                        }, 15000);

                    }else{
                        toast("Conectese a un dispositivo para relizar esta acción");
                    }
                else{
                    tilnombreble.setError("El nombre debe contener entre 1 - 10 caracteres");
                }

            }
        });

        botonAtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    /*
     *  Método que crea un diálogo, con el formulario correspondiente para cambiar la conexión
     *  Wifi del dispositivo, es decir, darle un SSID y contraseña del router al que se va
     *  conectar el dispositivo.
     *  Requiere de conexión previa con el dispositivo BLE para que se haga efectivo el cambio.
     */

    private void dialogoWifi() {

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(this);
        View mView = getLayoutInflater().inflate(R.layout.dialogo_wifi, null);
        final EditText nombre = (EditText) mView.findViewById(R.id.m_ssid);
        final EditText pass = (EditText) mView.findViewById(R.id.m_ssi_pass);
        Button btnGuardar = (Button) mView.findViewById(R.id.btn_guardar);
        Button botonAtras = (Button) mView.findViewById(R.id.boton_cancelar);
        final TextInputLayout tilnombreble=(TextInputLayout) mView.findViewById(R.id.til_nombre);
        mBuilder.setView(mView);

        final AlertDialog dialog = mBuilder.create();
        dialog.show();
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeviceSSID=nombre.getText().toString();
                mDeviceSSIDpass=pass.getText().toString();

                final ProgressDialog progress;
                if(!mDeviceSSID.isEmpty())
                    if (mConnected ) {
                        Log.i(TAG,"AT+NAME" + nombre.getText().toString()+( " id "+mDeviceId));
                        makeChange(characteristicTX,"ssid"+mDeviceSSID);
                        makeChange(characteristicTX,"pass"+mDeviceSSIDpass);
                        dialog.dismiss();
                        progress = ProgressDialog.show(DevicePanelActivity.this, "Reiniciando ...", "" + "Esto puede tardar unos segundos, por favor espere.");  //show a progress dialog
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                Intent i = getBaseContext().getPackageManager()
                                        .getLaunchIntentForPackage( getBaseContext().getPackageName() );
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                finish();
                                startActivity(i);
                                progress.dismiss();

                            }

                        }, 15000);

                    }else{
                        toast("Conectese a un dispositivo para relizar esta acción");
                    }
                else{
                    tilnombreble.setError("El nombre debe contener entre 1 - 10 caracteres");
                }

            }
        });

        botonAtras.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

//  Método para activar o desactivar las notificación Bluetooht
    private void notificationChange(BluetoothGattCharacteristic characteristic, Boolean accion){
        try {
            if (mConnected) {
                mBluetoothLeService.setCharacteristicNotification(characteristic, accion);

            }else{
                reconectar();
            }
        }catch (Exception e){
            toast("Reconecta el bluetooth");
            reconectar();
        }

    }

//  Método para enviar datos por Bluetooth
    private void makeChange(BluetoothGattCharacteristic characteristic, String accion) {
        final String str =accion;

        Log.d(TAG, "Sending result BLE=" + str);
        final byte[] tx = str.getBytes();
        try {

            if (mConnected) {

                mBluetoothLeService.writeCharacteristic(characteristic,tx);

                mBluetoothLeService.setCharacteristicNotification(characteristic, true);

            }else{
                reconectar();

            }
        }catch (Exception e){

            toast("Reconecta el bluetooth");
            reconectar();
        }
    }

//  Método para volver a establecer conexión BLE con el dispositivo
    private void  reconectar(){
        mBluetoothLeService.disconnect();
        progresReconect = ProgressDialog.show(DevicePanelActivity.this, "Reconectando...", "Por favor espere.");  //show a progress dialog
//        long timeout = (System.currentTimeMillis() - lastSendLed);
//        if (timeout > 2000 || timeout < 0) {
//            lastSendLed = System.currentTimeMillis();
//
//        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                mBluetoothLeService.connect(mDeviceAddress);

            }

        }, 2000);

    }

}
