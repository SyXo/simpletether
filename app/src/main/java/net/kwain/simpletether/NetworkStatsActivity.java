package net.kwain.simpletether;

import android.content.Intent;
import android.graphics.Color;
import android.net.TrafficStats;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;

public class NetworkStatsActivity extends AppCompatActivity {
    private static final String TAG = "NetworkStatsActivity";

    public static boolean tethering = false;
    private Handler handler;
    private StatsUpdater updater;

    public final static int REFRESH_INTERVAL = 5;
    public final static int NB_POINTS = 20;
    private LinkedList<Point> stats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.graph_title);
        setContentView(R.layout.activity_network_stats);

        handler = new Handler();
        stats = new LinkedList<>();
    }

    public static void setTetheringEnabled(boolean enabled) {
        tethering = enabled;
    }

    public boolean isTetheringEnabled() {
        return tethering;
    }

    private void openConnectionSharingSettings() {
        Intent launch = new Intent();
        launch.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_FROM_BACKGROUND);
        launch.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        startActivity(launch);
    }

    private static class Point {
        private long tx;
        private long rx;
        private Date date;

        public Point(long rx, long tx) {
            this.date = new Date();
            this.rx = rx;
            this.tx = tx;
        }

        public PointValue getRx(int idx) {
            return new PointValue(idx * REFRESH_INTERVAL, (float)(rx / 1024 / 1024));
        }

        public PointValue getTx(int idx) {
            return new PointValue(idx * REFRESH_INTERVAL, (float)(tx / 1024 / 1024));
        }

        public PointValue getSum(int idx) {
            return new PointValue(idx * REFRESH_INTERVAL, (float)((rx + tx) / 1024 / 1024));
        }

        public PointValue getSpeed(int idx, Point previousPoint) {
            if (previousPoint == null) {
                return new PointValue(idx * REFRESH_INTERVAL, 0f);
            }
            float tDiff = ((float)((date.getTime() - previousPoint.date.getTime()))) / 1000;
            float bDiff = ((float)((rx + tx) - previousPoint.rx - previousPoint.tx));
            bDiff /= tDiff;
            return new PointValue(idx * REFRESH_INTERVAL, bDiff / 1024);
        }

        public String getSpeedStr(Point previousPoint) {
            if (previousPoint == null) {
                Log.w(TAG, "getSpeedStr(): no previous point");
                return "RX: 0B/s ; TX: 0B/s";
            }
            float tDiff = ((float)((date.getTime() - previousPoint.date.getTime()))) / 1000;
            float rxSpeed = ((float)((rx) - previousPoint.rx)) / tDiff;
            float txSpeed = ((float)((tx) - previousPoint.tx)) / tDiff;
            return ("RX: " + (((int)(rxSpeed)) / 1024) + "KiB/s" +
                    " ; TX: " + (((int)(txSpeed)) / 1024) + "KiB/s");
        }

        public String getTotalStr() {
            long t = (rx + tx) / 1024 / 1024 ;
            return ("Total: " + Long.toString(t) + "MiB");
        }

        public PointValue getSpeedRx(int idx, Point previousPoint) {
            if (previousPoint == null) {
                return new PointValue(idx * REFRESH_INTERVAL, 0f);
            }
            float tDiff = ((float)((date.getTime() - previousPoint.date.getTime()))) / 1000;
            float bDiff = ((float)((rx) - previousPoint.rx));
            bDiff /= tDiff;
            return new PointValue(idx * REFRESH_INTERVAL, bDiff / 1024);
        }

        public PointValue getSpeedTx(int idx, Point previousPoint) {
            if (previousPoint == null) {
                return new PointValue(idx * REFRESH_INTERVAL, 0f);
            }
            float tDiff = ((float)((date.getTime() - previousPoint.date.getTime()))) / 1000;
            float bDiff = ((float)((tx) - previousPoint.tx));
            bDiff /= tDiff;
            return new PointValue(idx * REFRESH_INTERVAL, bDiff / 1024);
        }

        public String toString() {
            return this.date.toString() + ": RX: " + this.rx + " ; TX: " + this.tx;
        }

        public boolean equals(Object o) {
            return this.date.equals(((Point)o).date);
        }
    }

    private class StatsUpdater implements Runnable {
        private boolean running = true;

        public StatsUpdater() {
        }

        public void stop() {
            running = false;
        }

        @Override
        public void run() {
            if (!running)
                return;
            Point pt = new Point(
                    TrafficStats.getMobileRxBytes(),
                    TrafficStats.getMobileTxBytes()
            );
            addTrafficStats(pt);
            handler.postDelayed(this, REFRESH_INTERVAL * 1000);
        }
    }

    public void addTrafficStats(Point pt) {
        Log.i(TAG, "Traffic stats: " + pt);

        if (stats.size() > 0) {
            Point last = stats.getLast();
            if (last.equals(pt)) {
                Log.w(TAG, "Already got a point for this date");
                return;
            }
        }

        stats.add(pt);

        while (stats.size() > NB_POINTS) {
            stats.removeFirst();
        }

        Log.i(TAG, "Got " + stats.size() + " points");

        Log.i(TAG, "Updating graphs");
        updateSpeedGraph();
        updateTotalGraph();
    }

    private void updateSpeedGraph() {
        if (stats.size() <= 0)
            return;

        LineChartView chart = (LineChartView)findViewById(R.id.chart_live_traffic);

        List<Line> lines = new ArrayList<>();

        List<PointValue> rx = new ArrayList<>();
        List<PointValue> tx = new ArrayList<>();
        List<PointValue> sum = new ArrayList<>();

        Point previous = null;
        Point pprevious = null;
        Point last = null;
        int idx = 0;

        for (Point pt : stats) {
            last = pt;
            sum.add(pt.getSpeed(idx, previous));
            rx.add(pt.getSpeedRx(idx, previous));
            tx.add(pt.getSpeedTx(idx, previous));

            pprevious = previous;
            previous = pt;
            idx += 1;
        }

        TextView txt = (TextView) findViewById(R.id.textView_live_traffic);
        txt.setText(last.getSpeedStr(pprevious));

        Line line;

        line = new Line(rx);
        line.setColor(ChartUtils.COLOR_GREEN);
        line.setHasLabels(false);
        line.setHasPoints(false);
        lines.add(line);

        line = new Line(tx);
        line.setColor(ChartUtils.COLOR_RED);
        line.setHasLabels(false);
        line.setHasPoints(false);
        lines.add(line);

        line = new Line(sum);
        line.setColor(ChartUtils.COLOR_BLUE);
        line.setHasLabels(true);
        line.setHasPoints(true);
        lines.add(line);

        LineChartData data = new LineChartData(lines);
        data.setAxisXBottom(new Axis().
                setHasLines(true).
                setTextColor(Color.BLACK)
        );

        data.setAxisYLeft(new Axis().
                setHasLines(true).
                setMaxLabelChars(3).
                setTextColor(Color.BLACK)
        );
        chart.setLineChartData(data);

    }

    private void updateTotalGraph() {
        if (stats.size() <= 0)
            return;

        LineChartView chart = (LineChartView)findViewById(R.id.chart_data_consumption);

        List<Line> lines = new ArrayList<>();

        List<PointValue> sum = new ArrayList<>();

        int idx = 0;
        for (Point pt : stats) {
            sum.add(pt.getSum(idx));
            idx += 1;
        }

        Line line;

        line = new Line(sum);
        line.setColor(ChartUtils.COLOR_BLUE);
        line.setHasLabels(true);
        line.setHasPoints(true);
        lines.add(line);

        LineChartData data = new LineChartData(lines);
        data.setAxisXBottom(new Axis().
                setHasLines(true).
                setTextColor(Color.BLACK)
        );

        data.setAxisYLeft(new Axis().
                setHasLines(true).
                setMaxLabelChars(3).
                setTextColor(Color.BLACK)
        );
        chart.setLineChartData(data);

        TextView txt = (TextView) findViewById(R.id.textView_data_consumption);
        txt.setText(stats.getLast().getTotalStr());
    }

    @Override
    protected void onResume() {
        super.onResume();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        WindowManager.LayoutParams params = window.getAttributes();
        params.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        // params.screenBrightness = 0;
        window.setAttributes(params);

        updater = new StatsUpdater();
        updater.run();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && !isTetheringEnabled()) {
            openConnectionSharingSettings();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        updater.stop();
        updater = null;
    }
}
