package com.example.bluetoothnexttry;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GraphActivity extends AppCompatActivity {

    LineChart chart = (LineChart) findViewById(R.id.chart);
    final static String TAG = GraphActivity.class.getName();
    public String path;
    public String file;

    List<Entry> tempsEntries = new ArrayList<Entry>();
    List<Entry> HFConfigKEntries = new ArrayList<Entry>();
    List<Entry> HFConfigUpEntries = new ArrayList<Entry>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
    }
    public void refreshChart(View view){
        getData(pB.getFilepath);
        List<ILineDataSet> chartDataSet = new ArrayList<ILineDataSet>();
        chartDataSet.add(popDataSet(tempsEntries, "Temperature"));
        chartDataSet.add(popDataSet(HFConfigKEntries, "HF Config K"));
        chartDataSet.add(popDataSet(HFConfigUpEntries, "HF Config Up"));
        LineData chartData = new LineData(chartDataSet);
        chart.setData(chartData);
        chart.invalidate();
    }

    private void getData(File logFile){
        String line = null;
        String[] vals = new String[3];
        try {
            FileInputStream fileInputStream = new FileInputStream(logFile);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            while ( (line = bufferedReader.readLine()) != null ){
                int i = 0;
                for (String s : line.split("\t")){
                    vals[i] = s;
                    i++;
                }
                fillData(vals[0],Float.parseFloat(vals[1]),Float.parseFloat(vals[2]));
            }
            fileInputStream.close();
            bufferedReader.close();
        }
        catch (FileNotFoundException e)
        {
            Log.d(TAG,e.getMessage());
        }
        catch (IOException e)
        {
            Log.d(TAG, e.getMessage());
        }
    }
    private void fillData(String situation, float value, float time){
        if (situation.contentEquals("Temperature:")){
            tempsEntries.add(new Entry(time, value));
            return;
        }
        else if (situation.contentEquals("HF Config K")){
            HFConfigKEntries.add(new Entry(time, value));
            return;
        }
        else if (situation.contentEquals("HF Config Up")){
            HFConfigUpEntries.add(new Entry(time, value));
            return;
        }
        return;
    }
    private LineDataSet popDataSet(List currentList, String label){
        LineDataSet myDataSet = new LineDataSet(currentList, label);
        myDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        return myDataSet;
    }
}
