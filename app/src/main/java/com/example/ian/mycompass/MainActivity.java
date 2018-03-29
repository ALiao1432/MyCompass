package com.example.ian.mycompass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    /*  TO DO
    *   add GPS data
    *   add find other people's direction
    * */

    private static final String TAG = MainActivity.class.getSimpleName();

    private SensorManager sensorManager;
    private Sensor mSensor;
    private Sensor aSensor;
    private CompassView compassView;

    private float[] mSensorValue = new float[3];
    private float[] aSensorValue = new float[3];
    private int[] intValues = new int[3];


    private SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mSensorValue = sensorEvent.values;
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                aSensorValue = sensorEvent.values;
            }
//            Log.d(TAG, "mSensorValue : " + mSensorValue[0] + ", " + mSensorValue[1] + ", " + mSensorValue[2]);
//            Log.d(TAG, "aSensorValue : " + aSensorValue[0] + ", " + aSensorValue[1] + ", " + aSensorValue[2]);
            calculateOrientation();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        compassView = new CompassView(this);
        ConstraintLayout layout = findViewById(R.id.mainLayout);

        setContentView(compassView);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mSensor == null || aSensor == null) {
            Snackbar.make(layout, "Not support magnetic sensor, exit now?", Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", view -> finish())
                    .show();
        } else {
            sensorManager.registerListener(listener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(listener, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private void calculateOrientation() {
        float[] values = new float[3];
        float[] R = new float[9];

        SensorManager.getRotationMatrix(R, null, aSensorValue, mSensorValue);
        SensorManager.getOrientation(R, values);

        for (int i = 0; i < values.length; i++) {
            intValues[i] = (int) Math.toDegrees(values[i]);
            if (intValues[i] < 0) {
                intValues[i] += 360;
            }
        }
        compassView.invalidate();
//        Log.d(TAG, "values[0] : " + intValues[0] + ",\n" + "values[1] : " + intValues[1] + ",\n" + "values[2] : " + intValues[2]);
    }

    @Override
    protected void onResume() {
        if (mSensor != null || aSensor != null) {
            sensorManager.registerListener(listener, mSensor, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(listener, aSensor, SensorManager.SENSOR_DELAY_UI);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(listener);
        super.onPause();
    }

    private class CompassView extends View {

        private Paint compassFramePaint = new Paint();
        private Paint compassTextPaint = new Paint();
        private Path path = new Path();
        private int wSize;
        private int hSize;
        private String degree;
        private Rect textRect = new Rect();

        private final float FIX_FRAME_RADIUS = 250;

        private CompassView(Context context) {
            super(context);

            initPaint();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            wSize = MeasureSpec.getSize(widthMeasureSpec);
            hSize = MeasureSpec.getSize(heightMeasureSpec);

            setMeasuredDimension(wSize, hSize);
        }

        private void initPaint() {
            compassFramePaint.setAntiAlias(true);
            compassFramePaint.setStrokeWidth(12);
            compassFramePaint.setStyle(Paint.Style.STROKE);

            compassTextPaint.setAntiAlias(true);
            compassTextPaint.setStrokeWidth(15);
            compassTextPaint.setTextSize(200);
            compassTextPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // set background color
            canvas.drawColor(Color.parseColor("#212121"));

            drawFixFrame(canvas);
            drawDegree(canvas);
        }

        private void drawFixFrame(Canvas canvas) {
            compassFramePaint.setColor(Color.parseColor("#e0e0e0"));

            setFixFrameScalePath();
            canvas.translate(wSize / 2, hSize / 2);
            canvas.drawCircle(0, 0, FIX_FRAME_RADIUS, compassFramePaint);
            for (int i = 0; i < 4; i++) {
                // draw N, E, S, W scale
                // each time canvas need to rotate 90 degree
                canvas.drawPath(path, compassFramePaint);
                canvas.rotate(90);
            }
        }

        private void setFixFrameScalePath() {
            path.moveTo(-wSize / 100, -FIX_FRAME_RADIUS);
            path.lineTo(0, -(FIX_FRAME_RADIUS + hSize / 100));
            path.lineTo(wSize / 100, -FIX_FRAME_RADIUS);
            path.close();
        }

        private void drawDegree(Canvas canvas) {
            degree = String.valueOf(intValues[0]);
            compassTextPaint.setColor(Color.parseColor("#757575"));
            compassTextPaint.getTextBounds(degree, 0, degree.length(), textRect);

            canvas.drawText(degree, -textRect.width() / 2, textRect.height() / 2, compassTextPaint);
        }
    }
}
