package com.example.bluetoothnexttry;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;

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

    List<Entry> entries = new ArrayList<Entry>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
    }

    private void fillData(String path, String fileName){
        String line = null;
        String vals[3];
        try {
            FileInputStream fileInputStream = new FileInputStream(new File(path + fileName));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            while ( (line = bufferedReader.readLine()) != null ){
                int i = 0;
                for (String s : line.split("\t")){
                    vals[i] = s;
                    i++;
                }
                entries.add(new Entry(Float.parseFloat(vals[0]), Float.parseFloat(vals[1]), Float.parseFloat(vals[2])));
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
}
