package com.example.tberroa.portal.screens.stats.recent;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.LayoutManager;
import com.androidplot.ui.Size;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import com.example.tberroa.portal.R;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class RecentViewAdapter extends RecyclerView.Adapter<RecentViewAdapter.plotViewHolder> {

    private final Context context;
    private final List<String> plotTitles;
    private final List<Map<String, SimpleXYSeries>> plotData;
    private final int numberOfPlots;

    public RecentViewAdapter(Context context, List<String> plotTitles, List<Map<String, SimpleXYSeries>> plotData) {
        this.context = context;
        this.plotTitles = plotTitles;
        this.plotData = plotData;
        numberOfPlots = plotData.size();
    }

    public class plotViewHolder extends RecyclerView.ViewHolder {
        final TextView plotTitle;
        final XYPlot plot;

        plotViewHolder(View itemView) {
            super(itemView);
            plotTitle = (TextView) itemView.findViewById(R.id.plot_title);
            plotTitle.setVisibility(View.INVISIBLE);
            plot = (XYPlot) itemView.findViewById(R.id.plot);
            plot.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return numberOfPlots;
    }

    @Override
    public plotViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        View inspection = LayoutInflater.from(context).inflate(R.layout.element_plot, viewGroup, false);
        return new plotViewHolder(inspection);
    }

    @Override
    public void onBindViewHolder(plotViewHolder plotViewHolder, int i) {
        // set views
        plotViewHolder.plotTitle.setText(plotTitles.get(i));
        createPlot(context, plotViewHolder.plot, plotData.get(i));

        // make views visible
        plotViewHolder.plotTitle.setVisibility(View.VISIBLE);
        plotViewHolder.plot.setVisibility(View.VISIBLE);
    }

    static public void createPlot(Context context, XYPlot plot, Map<String, SimpleXYSeries> plotData) {
        // initialize the min and max values of the data
        double min = 500000, max = 0;

        // iterate over each entry in the plot data map
        int i = 0;
        for (Map.Entry<String, SimpleXYSeries> entry : plotData.entrySet()) {
            // update the min and max values as iteration occurs
            for (int j = 0; j < entry.getValue().size(); j++) {
                if (max < entry.getValue().getY(j).doubleValue()) {
                    max = entry.getValue().getY(j).doubleValue();
                }
                if (min > entry.getValue().getY(j).doubleValue()) {
                    min = entry.getValue().getY(j).doubleValue();
                }
            }

            // add each series to the plot
            LineAndPointFormatter seriesFormat = new LineAndPointFormatter();
            seriesFormat.setPointLabelFormatter(new PointLabelFormatter());
            switch (i % 8) {
                case 0:
                    seriesFormat.configure(context.getApplicationContext(), R.xml.line_blue);
                    break;
                case 1:
                    seriesFormat.configure(context.getApplicationContext(), R.xml.line_green);
                    break;
                case 2:
                    seriesFormat.configure(context.getApplicationContext(), R.xml.line_orange);
                    break;
                case 3:
                    seriesFormat.configure(context.getApplicationContext(), R.xml.line_pink);
                    break;
                case 4:
                    seriesFormat.configure(context.getApplicationContext(), R.xml.line_purple);
                    break;
                case 5:
                    seriesFormat.configure(context.getApplicationContext(), R.xml.line_red);
                    break;
                case 6:
                    seriesFormat.configure(context.getApplicationContext(), R.xml.line_sky);
                    break;
                case 7:
                    seriesFormat.configure(context.getApplicationContext(), R.xml.line_yellow);
                    break;
            }
            plot.addSeries(entry.getValue(), seriesFormat);
            i++;
        }

        // calculate the range step value
        double step = Math.floor((max - min) / 5);
        if (step < 1) {
            step = 1;
        }

        // plot styling
        plot.setBorderStyle(XYPlot.BorderStyle.NONE, null, null);
        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, step);
        plot.setRangeValueFormat(new DecimalFormat("#"));
        plot.setTicksPerRangeLabel(1);
        XYGraphWidget g = plot.getGraphWidget();
        g.position(-0.5f, XLayoutStyle.RELATIVE_TO_RIGHT,
                -0.5f, YLayoutStyle.RELATIVE_TO_BOTTOM, AnchorPosition.CENTER);
        g.setSize(Size.FILL);
        g.setBackgroundPaint(null);
        g.setGridBackgroundPaint(null);
        g.setDomainOriginLinePaint(null);
        LayoutManager l = plot.getLayoutManager();
        l.remove(plot.getTitleWidget());
        l.remove(plot.getDomainLabelWidget());
        l.remove(plot.getLegendWidget());
    }
}
