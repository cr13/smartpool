package com.crodriguez.smartpool.activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.crodriguez.smartpool.R;
import com.crodriguez.smartpool.SmartPoolDevice;
import com.crodriguez.smartpool.adapters.AdapterDevices;
import com.facebook.login.LoginManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();


    private static final int RC_DEVICE = 1;
    private static final int BLUETOOTH = 2;
    private SwipeRefreshLayout refreshLayout;
    private BluetoothAdapter mBluetoothAdapter = null;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseDatabase database;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static String address = null;
    private AdapterDevices devices;
    private RecyclerView vista;
    private AlertDialog alertDialog;
    private Context context;
    public int numDevices;

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addDevice();
            }
        });

        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();


        database = FirebaseDatabase.getInstance();
        address = getIntent().getStringExtra(EXTRAS_DEVICE_ADDRESS);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if(user == null){
                goLoginScreen();
                }
            }
        };

        if(user!=null) {

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();

            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            final View cabecera = navigationView.getHeaderView(0);
            de.hdodenhof.circleimageview.CircleImageView perfil = (de.hdodenhof.circleimageview.CircleImageView) cabecera.findViewById(R.id.userImage);

            if (user.getPhotoUrl() != null)
                Picasso.get().load(user.getPhotoUrl()).resize(300, 300).centerCrop().into(perfil);
            else{
                perfil.getLayoutParams().height = 300;
                perfil.getLayoutParams().width = 300;
                perfil.setImageDrawable(getDrawable(R.drawable.ic_user));
                perfil.requestLayout();}
            ((TextView) cabecera.findViewById(R.id.txtNick)).setText(user.getDisplayName());
            ((TextView) cabecera.findViewById(R.id.txtEmail)).setText(user.getEmail());
            navigationView.setNavigationItemSelectedListener(this);

            //Cargamos los dispositivos asociados a la cuenta

            vista = (RecyclerView) findViewById(R.id.device_recycle);
            vista.setLayoutManager(new LinearLayoutManager(this));
            devices = new AdapterDevices(this);

            vista.setAdapter(devices);

            alertDialog = new SpotsDialog(context,R.style.Custom);

            //Cargamos botones
            buscarReceptores(user);

            // Obtener el refreshLayout
            refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);

            // Iniciar la tarea asíncrona al revelar el indicador
            refreshLayout.setOnRefreshListener(
                    new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            new HackingBackgroundTask().execute();
                        }
                    }
            );

        }
    }
    //Función para añadir escanear y añadir un dispositivo nuevo
    private void addDevice() {
        Intent i = new Intent(this, DeviceScanActivity.class);
        startActivityForResult(i,RC_DEVICE);
    }
    //Función según el resultado obtenido en la actividad de añadir un dispositivo nuevo
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1://Bluetooth
                    try {
                        address = data.getStringExtra(MainActivity.EXTRAS_DEVICE_ADDRESS); //receive the address of the bluetooth device
                        String name= data.getStringExtra(MainActivity.EXTRAS_DEVICE_NAME);


                        if (address != null) {
                            if(isOnlineNet())
                                guardarDevice(address,name);
                            else{
                                SmartPoolDevice smartPoolDevice = new SmartPoolDevice(-1,name, address);
                                Intent intent = new Intent(context, DevicePanelActivity.class);
                                intent.putExtra("dispositivo", smartPoolDevice);
                                startActivity(intent);
                            }


                        }
                    } catch (Exception e) {
                        Log.e(TAG, "address erronea");
                    }
                    break;
            }

        }
    }

    //Función para añadir el dispostivo nuevo a la base de datos
    protected void guardarDevice(final String address, final String name) {

        final DatabaseReference myRef = database.getReference("Usuarios").child(""+mAuth.getUid());
        final DatabaseReference myRefComp = database.getReference("Devices");
        myRefComp.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean existe= dataSnapshot.child(address).exists();
                if(!existe){
                    myRefComp.child(address).child("num_sensores").setValue(3);
                }
                myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        //Solo añade dispistivo si el usuario no lo tiene
                        boolean exist = false;
                        numDevices = (int) dataSnapshot.getChildrenCount();
                        try{
                            for(int i = 0; i<numDevices; i++)
                                exist=dataSnapshot.child(""+i).child("mac").getValue().equals(address);
                        }catch (Exception e){
                            exist=false;
                        }
                        if(!exist){
                            myRef.child("" + numDevices).child("mac").setValue(address);
                            myRef.child("" + numDevices).child("nombre").setValue(name);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Actualizamos la lista de dispositivos
        new HackingBackgroundTask().execute();

    }


    //Función para comprobar si tenemos conexión ha internet
    public Boolean isOnlineNet() {

        try {
            Process p = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.es");

            int val           = p.waitFor();
            boolean reachable = (val == 0);
            return reachable;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    //Función para ir al Login en caso de no estar logueado
    private void goLoginScreen() {
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    //Función para cerrar sesión
    private void logout(){
        FirebaseAuth.getInstance().signOut();
        LoginManager.getInstance().logOut();
        goLoginScreen();
    }

    //Sobre cargamos la función onBackPressed() para volver al activity anterior
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_manage) {

        }  else if (id == R.id.nav_send) {
            logout();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //Función para cargar los dispostivos de la BD del usuario logueado
    private void buscarReceptores(FirebaseUser user) {
        final DatabaseReference usuFire = database.getReference("Usuarios").child(user.getUid());
        try{alertDialog.show();}catch (Exception e){Log.e(TAG,"Error mostrando alert findDevices");}
        usuFire.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot receptor : dataSnapshot.getChildren()) {
                    int idArdu = Integer.parseInt(receptor.getKey());
                    String name=receptor.child("nombre").getValue().toString();
                    String mac=receptor.child("mac").getValue().toString();
                    SmartPoolDevice smartPoolDevice = new SmartPoolDevice(idArdu,name, mac);
                    devices.addDevice(smartPoolDevice);

                }
                try{alertDialog.dismiss();}catch (Exception e){Log.e(TAG,"Error cerrando alert findDevices");}
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
    // Función para para llamar a Toast de forma mas rápida
    public void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    //Función para Activar el bluetooth en caso de estar desactivado
    private void openBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, BLUETOOTH);

    }

    //Función para conectar con el dipositivo
    public void conectar(SmartPoolDevice device) {

            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter != null) {
                if (!mBluetoothAdapter.isEnabled()) {
                    openBluetooth();
                }else{

//                    Log.i("Debug", device.getName()+ "Abrimos servicio "+ device.getMac() +" id "+device.getId_divice());
                    Intent intent = new Intent(context, DevicePanelActivity.class);
                    intent.putExtra("dispositivo", device);

                    startActivity(intent);
                }
            }

    }

    //Función asincrona para refrescar la lista de dispositvos
    public class HackingBackgroundTask extends AsyncTask<Void, Void, List<SmartPoolDevice>> {

        static final int DURACION = 1000; // 1 segundos de carga

        @Override
        protected List doInBackground(Void... params) {


            try {
                Thread.sleep(DURACION);
                buscarReceptores(mAuth.getCurrentUser());
  //              progress.dismiss();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Retornar los nuevos elementos para el adaptador
           return devices.getDevices();

        }

        @Override
        protected void onPostExecute(List result) {
            super.onPostExecute(result);


            // Limpiar elementos antiguos
            devices.clear();

            // Añadir elementos nuevos
            devices.addAll(result);

            // Parar la animación del indicador
            refreshLayout.setRefreshing(false);


        }

    }
}
