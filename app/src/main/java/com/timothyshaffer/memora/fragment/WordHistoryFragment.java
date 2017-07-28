package com.timothyshaffer.memora.fragment;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.timothyshaffer.memora.R;
import com.timothyshaffer.memora.db.MemoraContentProvider;
import com.timothyshaffer.memora.db.MemoraDbContract;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WordHistoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WordHistoryFragment extends Fragment {
    // Parameter arguments
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_CARD_ID = "id";

    private long mCardId;

    private static final int CHART_SPA_TO_ENG = 0;
    private static final int CHART_ENG_TO_SPA = 1;
    private BarChart[] mBarChart;

    private Context mContext;   // For getting colors from our resources file

    public WordHistoryFragment() {
        // Required empty public constructor
    }

    /**
     * Returns a new instance of this fragment for the given Card ID
     */
    public static WordHistoryFragment newInstance(long cardId) {
        WordHistoryFragment fragment = new WordHistoryFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CARD_ID, cardId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBarChart = new BarChart[2];
        mContext = getContext();
        if (getArguments() != null) {
            mCardId = getArguments().getLong(ARG_CARD_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_word_history, container, false);

        // Setup chart appearance
        mBarChart[CHART_SPA_TO_ENG] = (BarChart) rootView.findViewById(R.id.spa_history_chart);
        mBarChart[CHART_ENG_TO_SPA] = (BarChart) rootView.findViewById(R.id.eng_history_chart);

        for( int i = 0; i < 2; i++ ){
            setupBarChart(mBarChart[i]);
        }

        // Dataset #1: Spa -> Eng Difficulties
        Cursor spaDiffCursor = getActivity().getContentResolver().query(
                MemoraContentProvider.CONTENT_URI_DIFFICULTY,
                null,   // SELECT
                MemoraDbContract.Card_Difficulty.COLUMN_NAME_CARD_ID + "=? AND " +
                        MemoraDbContract.Card_Difficulty.COLUMN_NAME_CARD_REVERSED + "= 0", // WHERE
                new String[]{String.valueOf(mCardId)},   // WHERE ARGS
                MemoraDbContract.Card_Difficulty.COLUMN_NAME_SUBMITTED + " ASC");  // SORT BY

        // Setup the chart using the Cursor's data
        setData(mBarChart[CHART_SPA_TO_ENG], spaDiffCursor, "Spa -> Eng");

        // Dataset #2: Eng -> Spa Difficulties
        Cursor engDiffCursor = getActivity().getContentResolver().query(
                MemoraContentProvider.CONTENT_URI_DIFFICULTY,
                null,   // SELECT
                MemoraDbContract.Card_Difficulty.COLUMN_NAME_CARD_ID + "=? AND " +
                        MemoraDbContract.Card_Difficulty.COLUMN_NAME_CARD_REVERSED + "= 1", // WHERE
                new String[]{String.valueOf(mCardId)},   // WHERE ARGS
                MemoraDbContract.Card_Difficulty.COLUMN_NAME_SUBMITTED + " ASC");  // SORT BY

        // Set the data for the second chart
        setData( mBarChart[CHART_ENG_TO_SPA], engDiffCursor, "Eng -> Spa" );


        return rootView;
    }


    // Invalidates the charts
    public void clearChart() {
        mBarChart[CHART_SPA_TO_ENG].clear();
        mBarChart[CHART_ENG_TO_SPA].clear();
    }


    // Setup the display of a BarChart (axes, legend, touch, zoom, etc.)
    private void setupBarChart(BarChart barChart) {
        // Disable all user interactions with the chart
        barChart.setTouchEnabled(false);
        barChart.setDragEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setHighlightPerDragEnabled(false);
        barChart.setHighlightPerTapEnabled(false);
        barChart.setDragDecelerationEnabled(false);

        barChart.setDescription("");   // No description
        barChart.setNoDataText("This card has not been attempted.");   // No data message

        // Setup left and right Y-Axis
        YAxis leftAxis = barChart.getAxisLeft();
        YAxis rightAxis = barChart.getAxisRight();

        // Set min/max based on the known min and max difficulty from the DB
        leftAxis.setAxisMaxValue(4);
        leftAxis.setAxisMinValue(0);

        leftAxis.setDrawLabels(false);
        rightAxis.setDrawLabels(false);
        leftAxis.setDrawGridLines(false);
        rightAxis.setDrawGridLines(false);
        leftAxis.setDrawZeroLine(true);
        rightAxis.setDrawZeroLine(true);

        // Setup X-Axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);

        // Setup Legend
        Legend legend = barChart.getLegend();
        //legend.setFormSize(8f); // set the size of the legend forms/shapes
        legend.setForm(Legend.LegendForm.SQUARE); // set what type of form/shape should be used
        legend.setPosition(Legend.LegendPosition.RIGHT_OF_CHART);
        legend.setTextSize(12f);
        legend.setTextColor(Color.BLACK);
        //legend.setXEntrySpace(50f); // set the space between the legend entries on the x-axis

        ArrayList<String> labels = new ArrayList<>();
        labels.add(0, "Critical");
        labels.add(1, "Hard");
        labels.add(2, "Medium");
        labels.add(3, "Easy");

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(0, ContextCompat.getColor(mContext, R.color.criticalColor));
        colors.add(1, ContextCompat.getColor(mContext, R.color.hardColor));
        colors.add(2, ContextCompat.getColor(mContext, R.color.medColor));
        colors.add(3, ContextCompat.getColor(mContext, R.color.easyColor));

        legend.setCustom(colors, labels);

    }


    // Setup the data for the suplied BarChart using the Cursor. The label is used to fill-in the legend.
    private void setData( BarChart barChart, Cursor cursor, String label ){
        // Create 2 Datasets (spa->eng and eng->spa)
        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        // X-Axis values for both Y datasets
        ArrayList<String> xVals = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        int easyColor = ContextCompat.getColor(mContext, R.color.easyColor);
        int medColor = ContextCompat.getColor(mContext, R.color.medColor);
        int hardColor = ContextCompat.getColor(mContext, R.color.hardColor);
        int critColor = ContextCompat.getColor(mContext, R.color.criticalColor);

        int idxDifficulty = cursor.getColumnIndex(MemoraDbContract.Card_Difficulty.COLUMN_NAME_DIFFICULTY);
        int idxSubmitted = cursor.getColumnIndex(MemoraDbContract.Card_Difficulty.COLUMN_NAME_SUBMITTED);

        ArrayList<BarEntry> yValsSpa = new ArrayList<>();
        int index = 0;  // The index of x value that each y value corresponds to
        while (cursor.moveToNext()) {
            xVals.add(cursor.getString(idxSubmitted));
            int difficulty = cursor.getInt(idxDifficulty);
            // Add 1 to diff so that critical (0) rating shows up instead of being a line at 0
            yValsSpa.add(new BarEntry(difficulty+1, index));
            index++;

            // The bar's color is determined by the difficulty rating
            switch(difficulty){
                case 0:
                    colors.add(critColor);
                    break;
                case 1:
                    colors.add(hardColor);
                    break;
                case 2:
                    colors.add(medColor);
                    break;
                case 3:
                    colors.add(easyColor);
                    break;
                default:
                    // Error, should never get here
                    colors.add(Color.RED);
            }
        }

        cursor.close();

        if( !yValsSpa.isEmpty() ) {
            BarDataSet set1 = new BarDataSet(yValsSpa, label);
            set1.setAxisDependency(YAxis.AxisDependency.LEFT);
            set1.setColors(colors);
            set1.setDrawValues(false);

            dataSets.add(set1);
        }

        if( !dataSets.isEmpty() ) {
            // Format the data specifically for a LineChart
            BarData data = new BarData(xVals, dataSets);

            // Add the data to the LineChart
            barChart.setData(data);
        }
    }

}
