package com.crodriguez.smartpool.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Vibrator;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crodriguez.smartpool.activities.DevicePanelActivity;
import com.crodriguez.smartpool.activities.MainActivity;
import com.crodriguez.smartpool.R;
import com.crodriguez.smartpool.SmartPoolDevice;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase adaptador que extiende de recycleView
 * Se utiliza para rellenar la lista de dispositivos a mostrar
 */

public class AdapterDevices extends RecyclerView.Adapter<AdapterDevices.DevicesviewHolder> {

    // Referenca a la BD de firebase
    private FirebaseDatabase database;
    private List<SmartPoolDevice> devices;
    private Context context;

    public List<SmartPoolDevice> getDevices() {
        return devices;
    }

    public AdapterDevices(Context context) {
        this.context = context;
        this.devices = new ArrayList<>();
        database = FirebaseDatabase.getInstance();


    }

    @Override
    public DevicesviewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view_dispositivos, parent, false);
        AdapterDevices.DevicesviewHolder holder = new AdapterDevices.DevicesviewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(DevicesviewHolder devicesviewHolder,  final int pos) {
        final Vibrator vibe = (Vibrator) this.context.getSystemService(Context.VIBRATOR_SERVICE);
        FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
        int idArdu = devices.get(pos).getId_divice();
//        final boolean permisos = devices.get(pos).isPermisos();
        final DevicesviewHolder device=devicesviewHolder;
        database.getReference("Usuarios").child(""+user.getUid()).child(""+idArdu).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                device.btn.setTag(dataSnapshot.child("mac").getValue(String.class));
                final String name= dataSnapshot.child("nombre").getValue(String.class);
                device.btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        vibe.vibrate(100);
                        String codigo=v.getTag().toString();
                        Log.i("debug","Boton " + codigo + "con no "+ name+ " id " + devices.get(pos).getId_divice());

                        ((MainActivity) context).conectar(devices.get(pos));
                    }
                });
                device.btn.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        dialogoConfirmacion(devices.get(pos),v, pos);
                        return false;
                    }
                });

                device.nombre_arduino.setText(name);
                Log.i("debug1",""+dataSnapshot.child("nombre").getValue(String.class));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private void dialogoConfirmacion(final SmartPoolDevice device, final View v, final int pos){

        // Dialog de confirmacion para eliminar usuario
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Advertencia");
        builder.setMessage("¿Estás seguro de eliminar este dispositivo?");

        builder.setPositiveButton("SI", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the
                database.getReference("Usuarios").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        ArrayList<String> devicesAct=(ArrayList<String>) dataSnapshot.getValue();

                        devicesAct.remove(pos);
                        dataSnapshot.getRef().setValue(devicesAct);
                        //Actualizamos los dispositivos
                        ((MainActivity) context).new HackingBackgroundTask().execute();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }

                });

            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    //    Añade una lista completa de items

    public void addAll(List<SmartPoolDevice> lista){
        devices.addAll(lista);
        notifyDataSetChanged();
    }


    //    Permite limpiar todos los elementos del recycler

    public void clear(){
        devices.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return  devices.size();
    }

    //Añade un dispositivo a la lista
    public void addDevice(SmartPoolDevice id){
        devices.add(id);
        notifyItemInserted(devices.size()-1);
    }


    class DevicesviewHolder extends RecyclerView.ViewHolder {
        TextView nombre_arduino;
        ConstraintLayout btn;



        public DevicesviewHolder(View itemView) {
            super(itemView);
            nombre_arduino= (TextView) itemView.findViewById(R.id.nombre_arduino);
            btn = (ConstraintLayout) itemView.findViewById(R.id.btn_device);
        }
    }
}

