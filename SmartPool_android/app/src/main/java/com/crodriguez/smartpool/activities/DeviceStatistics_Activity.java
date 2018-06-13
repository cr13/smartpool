package com.crodriguez.smartpool.activities;

import android.app.AlertDialog;
import android.graphics.Color;
//import android.support.v7.app.AlertDialog;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import dmax.dialog.SpotsDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.crodriguez.smartpool.R;
import com.crodriguez.smartpool.SmartPoolDevice;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/*
* Clase para crear las gráficas de los datos a mostrar en la Actividad
*/
public class DeviceStatistics_Activity extends AppCompatActivity {

    private static final String TAG = DeviceStatistics_Activity.class.getSimpleName();

    public static SmartPoolDevice EXTRAS_DEVICE = null;
    private FirebaseDatabase database;
    private long lastDato;

    private LineChart lineChart,lineChart1,lineChart2;

    private AlertDialog alertDialog;
    //Eje X
    private ArrayList <String> rangoX= new  ArrayList<>();
    //Eje Y
    final ArrayList<Float> temp = new ArrayList<>();
    final ArrayList<Float> ph = new ArrayList<>();
    final ArrayList<Float> turbi = new ArrayList<>();

    //Colors
    private ArrayList<Integer> colors= new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_statistics);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Estadísticas");

        //Initialize Firebase Database
        database = FirebaseDatabase.getInstance();
        EXTRAS_DEVICE = (SmartPoolDevice) getIntent().getExtras().getSerializable("dispositivo");

        lineChart = (LineChart) findViewById(R.id.lineChart);
        lineChart1 = (LineChart) findViewById(R.id.lineChart1);
        lineChart2 = (LineChart) findViewById(R.id.lineChart2);

        alertDialog = new SpotsDialog(DeviceStatistics_Activity.this,R.style.Custom);
        alertDialog.show();

    }

    /*
    *  Método para obtener los datos de la Base de datos
    *  Según el rango solicitado por el usuarios se mostrarán desde la fecha actual:
    *  Las últimas 24 horas.
    *  Una semana, mostrado la media por dia de los datos.
    *  Dos semana, mostrado la media por dia de los datos.
    *  Un mes, mostrado la media por dia de los datos.
    */
    private void cargarDatos(int periodo) {
        Long fechaactual = System.currentTimeMillis()/1000;
        if(periodo!=1)
            periodo-=1;
        final Long fechahaceXday= fechaactual -  3600*24*(periodo);

        rangoX.clear();
        temp.clear();
        ph.clear();
        turbi.clear();

        DatabaseReference ref =database.getReference("Devices").child(EXTRAS_DEVICE.getMac());

        ref.orderByValue().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                lastDato = Long.parseLong(dataSnapshot.getChildren().iterator().next().getKey());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        final int finalPeriodo = periodo;

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //Log.i("debug","Fecha minima "+fechahaceXday);

                Iterable<DataSnapshot> data = dataSnapshot.getChildren();
                String vvaux="";
                float avgtemp= (float) 0.0;
                float avgph= (float) 0.0;
                float avgturbi= (float) 0.0;
                int num=0;
                for (DataSnapshot datos : data) {
                    Long date= Long.valueOf(datos.getKey());
                    if(date>=fechahaceXday){
                        Date df = new java.util.Date(date*1000);//pasamos a milisegundos
                        String dia_mes = new SimpleDateFormat("dd MM").format(df);
                        String dmh = new SimpleDateFormat("dd MM HH").format(df);
                        if(num==0)
                            vvaux=dia_mes;

                        avgtemp+=datos.child("temp").child("valor").getValue(Float.class);
                        avgph+=datos.child("ph").child("valor").getValue(Float.class);
                        Log.i("debug"," tubi2: " + datos.child("turbi").child("valor").getValue(Float.class));

                        avgturbi+=datos.child("turbi").child("valor").getValue(Float.class);
                        num++;
                        if(!vvaux.contentEquals(dia_mes) || (date.equals(lastDato)&& finalPeriodo !=1) ){
                            rangoX.add(vvaux.replace(" ","/"));
                            temp.add(avgtemp/num);
                            ph.add(avgph/num);
                            turbi.add(avgturbi/num);
                            avgtemp= (float) 0.0;
                            avgph= (float) 0.0;
                            avgturbi= (float) 0.0;
                            num=0;
                            vvaux=dia_mes;

                        }else if(finalPeriodo ==1){
                            rangoX.add(dmh.replaceFirst(" ","/"));
                            temp.add(avgtemp);
                            ph.add(avgph);
                            Log.i("debug"," tubi: " + avgturbi);

                            turbi.add(avgturbi);
                            avgtemp= (float) 0.0;
                            avgph= (float) 0.0;
                            avgturbi= (float) 0.0;
                            num=0;
                        }

                    }
                }
                Log.i("debug", "fecha "+rangoX+" TEmpe: " + avgturbi);
                createCharts("Temperatura",lineChart,temp, 10);
                createCharts("Ph",lineChart1,ph,3);
                createCharts("Turbidez",lineChart2,turbi,20);
                alertDialog.dismiss();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_statistic_devices, menu);

        MenuItem item = menu.findItem(R.id.spinner);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);
        List<String> consultas = new ArrayList<>();
        consultas.add("24 Horas");
        consultas.add("1 Semana");
        consultas.add("2 Semana");
        consultas.add("1 mes");

        String[] spinners = new String[consultas.size()];
        consultas.toArray(spinners);
        ArrayAdapter spinnerArrayAdapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_dropdown_item,
                spinners);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(spinnerArrayAdapter);
        spinner.setSelection(1);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Log.i("debug","posicion: "+ position);
                alertDialog.show();
                switch (position){
                    case 0: cargarDatos(1);
                    break;
                    case 1: cargarDatos(7);
                        break;
                    case 2: cargarDatos(14);
                        break;
                    case 3: cargarDatos(30);
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return true;


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);

    }

    //Crear grafica
    public void createCharts(String description, LineChart lineChart,ArrayList<Float> datos, int Granularity){
        //LineChart
        lineChart = (LineChart) getSameChart(lineChart, description, Color.BLUE, Color.WHITE, 3000,true);
        lineChart.setData(getLineData(datos));
        lineChart.invalidate();
        axisX(lineChart.getXAxis());
        axisLeft(lineChart.getAxisLeft(),Granularity);
        axisRight(lineChart.getAxisRight());


    }

    //Carasterísticas comunes en las gráficas
    private Chart getSameChart(Chart chart, String description, int textColor, int background, int animateY, boolean leyenda){
        chart.getDescription().setText(description);
        chart.getDescription().setTextColor(textColor);
        chart.getDescription().setTextSize(15);
        chart.setBackgroundColor(background);
        chart.animateY(animateY);

        //Validar porque la grafica de radar y dispersion tiene dos datos especificos y la leyenda se crea de acuerdo a esos datos.
        if(leyenda)
            legend(chart);
        return chart;
    }

//    Método para mostrar la legenda de la gráfica
    private void legend(Chart chart) {
        Legend legend = chart.getLegend();
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);

        ArrayList<LegendEntry> entries = new ArrayList<>();
        for (int i = 0; i < rangoX.size(); i++) {
            LegendEntry entry = new LegendEntry();
            int color=getNewColor();
            colors.add(color);
            entry.formColor = color;
            entry.label = rangoX.get(i);
            entries.add(entry);
        }
        legend.setCustom(entries);
    }

//    Método para generar un color aleatorio
    private int getNewColor() {
        Random rnd = new Random();
        int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));

        return color;
    }

    //Eje horizontal o eje X
    private void axisX(XAxis axis){
        axis.setGranularityEnabled(true);
        axis.setPosition(XAxis.XAxisPosition.BOTTOM);
        axis.setValueFormatter(new IndexAxisValueFormatter(rangoX));
    }

    //Eje Vertical o eje Y lado izquierdo
    private void axisLeft(YAxis axis, int Granularity){
        axis.setSpaceTop(30);
        axis.setAxisMinimum(0);
        axis.setAxisMaximum(Granularity*5);
        axis.setGranularity(Granularity);
    }

    //Eje Vertical o eje Y lado Derecho
    private void axisRight(YAxis axis){
        axis.setEnabled(false);
        axis.setSpaceTop(30);
        axis.setAxisMinimum(0);
        axis.setGranularity(1);
    }

    //Carasteristicas comunes en dataset
    private DataSet getDataSame(DataSet dataSet){
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(10);
        return dataSet;
    }

    // Lista de puntos
    private ArrayList<Entry> getLineEntries(ArrayList<Float> datos) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < datos.size(); i++){
            entries.add(new Entry(i, datos.get(i)));}
        return entries;
    }

    //Método para dibujar y proporcionar el estilo de lineas, según los puntos recibidos
    private LineData getLineData(ArrayList<Float> datos) {
        LineDataSet lineDataSet = (LineDataSet) getDataSame(new LineDataSet(getLineEntries(datos), ""));
        lineDataSet.setLineWidth(2.5f);
        //Color de los circulos de la grafica
        lineDataSet.setCircleColors(colors);
        //Tamaño de los circulos de la grafica
        lineDataSet.setCircleRadius(5f);
        //Sombra grafica
        lineDataSet.setDrawFilled(true);
        //Estilo de la linea picos(linear) o curveada(cubic) cuadrada(Stepped)
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        return new LineData(lineDataSet);
    }

}
