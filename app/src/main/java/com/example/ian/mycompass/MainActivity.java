package com.example.ian.mycompass;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import java.util.List;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MainActivity extends AppCompatActivity {

    /*  TO DO
    *   add degree number transform animation
    *   add find other people's direction
    *   study wifi indoor
    *   add sensor calibration function
    * */

    private static final String TAG = MainActivity.class.getSimpleName();

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private ResultReceiver resultReceiver;
    private SensorManager sensorManager;
    private Sensor mSensor;
    private Sensor aSensor;
    private CompassView compassView;

    private float[] mSensorValue;
    private float[] aSensorValue;
    private float[] floatsValues = new float[3];
    private float[] reverseFloatValue = new float[3];
    private double[] gpsCoordinates = new double[2]; // 0 : latitude, 1 : longitude
    private int[] intValues = new int[3];
    private boolean hasPermission = false;
    private boolean isGetAddressSuccess = false;
    private String addressOutput = "";

    private SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {

            // when sensor has data feedback,
            // get data by type
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mSensorValue = lowPassFilter(sensorEvent.values, mSensorValue);
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                aSensorValue = lowPassFilter(sensorEvent.values, aSensorValue);
            }

            // prevent mSensorValue or aSensorValue is null
            if (mSensorValue == null) {
                mSensorValue = new float[3];
            }
            if (aSensorValue == null) {
                aSensorValue = new float[3];
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

        final int MY_COARSE_LOCATION_REQUEST_CODE = 999;
        final int MY_FINE_LOCATION_REQUEST_CODE = 998;

        ConstraintLayout layout = findViewById(R.id.mainLayout);
        fusedLocationProviderClient = getFusedLocationProviderClient(MainActivity.this);
        compassView = new CompassView(this);
        resultReceiver = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultData == null) {
                    return;
                }

                addressOutput = resultData.getString(GeoConstants.RESULT_DATA_KEY);
                if (addressOutput == null) {
                    addressOutput = "";
                }

                if (resultCode == GeoConstants.SUCCESS_RESULT) {
                    Log.d(TAG, "found address successful : " + addressOutput);
//                    addressOutput = splitString(addressOutput);
                    isGetAddressSuccess = true;
                } else  if (resultCode == GeoConstants.FAILURE_RESULT) {
                    addressOutput = "...";
                }
            }
        };

        setContentView(compassView);

        // check if user grant the permission
        if (this.checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_COARSE_LOCATION_REQUEST_CODE);
        }
        if (this.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_FINE_LOCATION_REQUEST_CODE);
        }

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        aSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mSensor == null || aSensor == null) {
            Snackbar.make(layout, "Not support magnetic sensor, exit now?", Snackbar.LENGTH_INDEFINITE)
                    .setAction("OK", view -> finish())
                    .show();
        }
    }

//    private String splitString(String s) {
//        StringTokenizer stringTokenizer = new StringTokenizer(s, ",");
//        StringBuilder stringBuilder = new StringBuilder();
//        int splitCount = stringTokenizer.countTokens();
//
//        for (int i = 0; i < splitCount; i++) {
//            if (i % (splitCount / 2) == 0) {
//                stringBuilder.append(stringTokenizer.nextToken()).append(", ");
//            } else {
//                stringBuilder.append(",\n").append(stringTokenizer.nextToken());
//            }
//        }
//
//        return stringBuilder.toString();
//    }

    private void startLocationUpdates() {

        final long LOCATION_REQUEST_INTERVAL = 1000 * 60; // 60 sec
        final long LOCATION_REQUEST_FASTEST_INTERVAL = LOCATION_REQUEST_INTERVAL / 2;

        // create the location request to start receiving updates
        LocationRequest locationRequest =  LocationRequest.create()
                .setInterval(LOCATION_REQUEST_INTERVAL)
                .setFastestInterval(LOCATION_REQUEST_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder requestBuilder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = requestBuilder.build();

        // check whether location settings are satisfied
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // do work here
                onLocationChanged(locationResult.getLocations());
            }
        };

        if (this.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
            getFusedLocationProviderClient(this).requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
    }

    private void startLatLongToAddressService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(GeoConstants.RECEIVER, resultReceiver);
        intent.putExtra(GeoConstants.LOCATION_DATA_EXTRA, lastLocation);
        startService(intent);
    }

    private void onLocationChanged(List<Location> locationList) {
        // new location has now been updated
        for (Location l : locationList) {
            Log.d(TAG, "update location : " + l.getLongitude() + ", " + l.getLatitude());
            gpsCoordinates[0] = l.getLatitude();
            gpsCoordinates[1] = l.getLongitude();

            lastLocation = l;
            startLatLongToAddressService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (checkIfPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                && checkIfPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            hasPermission = true;
            startLocationUpdates();
        }
    }

    // sensor is too sensitive, need a filter to get smooth data
    private float[] lowPassFilter(float[] input, float[] output) {
        final float LOW_PASS_FILTER_COEFFICIENT = 0.15f;

        if (output == null) {
            return input;
        }

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + LOW_PASS_FILTER_COEFFICIENT * (input[i] - output[i]);
        }
        return output;
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
            floatsValues[i] = (float) Math.toDegrees(values[i]);
            intValues[i] = (int) floatsValues[i];

            if (floatsValues[i] < 0) {
                floatsValues[i] += 360f;
            }
            if (intValues[i] < 0) {
                intValues[i] += 360;
            }

            reverseFloatValue[i] = - floatsValues[i];
        }
        compassView.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mSensor != null || aSensor != null) {
//            Log.d(TAG, "register sensor listener");
            sensorManager.registerListener(listener, mSensor, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(listener, aSensor, SensorManager.SENSOR_DELAY_UI);
        }

        // need to check the permission in onResume() when hasPermission is false
        if (!hasPermission) {
            if (checkIfPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                    && checkIfPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                hasPermission = true;
                startLocationUpdates();
            }
        } else {
            startLocationUpdates();
        }
    }

    private boolean checkIfPermissionGranted(final String permission) {
        if (this.checkPermission(permission, Process.myPid(), Process.myUid()) == PackageManager.PERMISSION_GRANTED) {
            switch (permission) {
                case Manifest.permission.ACCESS_COARSE_LOCATION:
                    fusedLocationProviderClient.getLastLocation()
                            .addOnSuccessListener(this, location -> {
                                if (location != null) {
                                    gpsCoordinates[0] = location.getLatitude();
                                    gpsCoordinates[1] = location.getLongitude();
                                    Log.d(TAG, "gpsCoordinates : " + gpsCoordinates[0] + ", " + gpsCoordinates[1]);
                                }
                            });
                    break;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
//        Log.d(TAG, "unregister sensor listener");
        sensorManager.unregisterListener(listener);
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private class CompassView extends View {

        private Paint compassFramePaint = new Paint();
        private Paint compassTextPaint = new Paint();
        private Path path = new Path();
        private String degree;
        private Rect textRect = new Rect();
        private int wSize;
        private int hSize;

        private final float FIX_FRAME_RADIUS = 250;
        private final float DYNAMIC_FRAME_RADIUS = 300;
        private final float DYNAMIC_FRAME_SMALL_CIRCLE_RADIUS = 6;
        private final float DYNAMIC_FRAME_GAP = (float) 360 / 40;
        private final String[] COMPASS_TEXT = {
                "N",
                "E",
                "S",
                "W"
        };

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
            drawDynamicFrame(canvas);
            drawDegree(canvas);
            drawLatitudeLongitude(canvas);
            drawAddress(canvas);
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
            path.reset();
            path.moveTo(-wSize / 100, -FIX_FRAME_RADIUS);
            path.lineTo(0, -(FIX_FRAME_RADIUS + hSize / 100));
            path.lineTo(wSize / 100, -FIX_FRAME_RADIUS);
            path.close();
        }

        private void drawDynamicFrame(Canvas canvas) {
            canvas.rotate(reverseFloatValue[0]);

            compassFramePaint.setTextSize(70);
            compassFramePaint.setColor(Color.parseColor("#616161"));
            compassFramePaint.getTextBounds(COMPASS_TEXT[0], 0, 1, textRect);

            canvas.drawCircle(0, 0, DYNAMIC_FRAME_RADIUS, compassFramePaint);

            for (float f = 0; f < 360f; f += DYNAMIC_FRAME_GAP) {
                if (Math.abs(floatsValues[0] - f) <= DYNAMIC_FRAME_GAP / 2) {
                    compassFramePaint.setColor(Color.parseColor("#d32f2f"));
                } else {
                    compassFramePaint.setColor(Color.parseColor("#616161"));
                }

                compassFramePaint.setStrokeWidth(6);
                compassFramePaint.setStyle(Paint.Style.FILL);

                if (f % 90 == 0) {
                    canvas.drawText(COMPASS_TEXT[(int) f / 90], -textRect.width() / 2, -(textRect.height() / 2 + DYNAMIC_FRAME_RADIUS * 1.1f), compassFramePaint);
                } else {
                    if (f % 45 == 0) {
                        canvas.drawCircle(0, -(textRect.height() / 2 + DYNAMIC_FRAME_RADIUS * 1.15f), DYNAMIC_FRAME_SMALL_CIRCLE_RADIUS * 2f, compassFramePaint);
                    } else {
                        canvas.drawCircle(0, -(textRect.height() / 2 + DYNAMIC_FRAME_RADIUS * 1.15f), DYNAMIC_FRAME_SMALL_CIRCLE_RADIUS, compassFramePaint);
                    }
                }

                canvas.rotate(DYNAMIC_FRAME_GAP);
            }

            compassFramePaint.setStyle(Paint.Style.STROKE);
            compassFramePaint.setStrokeWidth(12);

            textRect.setEmpty();
            canvas.rotate(-reverseFloatValue[0]);
        }

        private void drawDegree(Canvas canvas) {
            degree = String.valueOf((int) Math.abs(reverseFloatValue[0]));
            compassTextPaint.setColor(Color.parseColor("#757575"));
            compassTextPaint.getTextBounds(degree, 0, degree.length(), textRect);

            canvas.drawText(degree, -textRect.width() / 2, textRect.height() / 2, compassTextPaint);
            textRect.setEmpty();
        }

        private void drawLatitudeLongitude(Canvas canvas) {

            if (hasPermission) {
                compassTextPaint.setTextSize(50);

                // latitude
                String coordinate = String.valueOf(gpsCoordinates[0]);
                coordinate =  addNESWBaseOnCoordinate(0, coordinate);
                compassTextPaint.getTextBounds(coordinate, 0, coordinate.length(), textRect);
                canvas.drawText(coordinate, -textRect.width() / 2, hSize / 2, compassTextPaint);

                // longitude
                coordinate = String.valueOf(gpsCoordinates[1]);
                coordinate =  addNESWBaseOnCoordinate(1, coordinate);
                compassTextPaint.getTextBounds(coordinate, 0, coordinate.length(), textRect);
                canvas.drawText(coordinate, -textRect.width() / 2, hSize / 2 - textRect.height(), compassTextPaint);

                compassTextPaint.setTextSize(200);
                textRect.setEmpty();
            }
        }

        private String addNESWBaseOnCoordinate(int latOrLong, String c) {
            if (latOrLong == 0) {
                // latitude
                if (c.startsWith("-")) {
                    c += " S";
                } else {
                    c += " N";
                }
            } else {
                // longitude
                if (c.startsWith("-")) {
                    c += " W";
                } else {
                    c += " E";
                }
            }

            return c;
        }

        private void drawAddress(Canvas canvas) {
            if (hasPermission && isGetAddressSuccess) {
                compassTextPaint.setTextSize(50);

                compassTextPaint.getTextBounds(addressOutput, 0, addressOutput.length(), textRect);
                canvas.drawText(addressOutput, -textRect.width() / 2, hSize / 2 - textRect.height() * 4, compassTextPaint);

                compassTextPaint.setTextSize(200);
                textRect.setEmpty();
            }
        }
    }
}
