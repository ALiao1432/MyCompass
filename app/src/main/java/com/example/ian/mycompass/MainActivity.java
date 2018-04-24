package com.example.ian.mycompass;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MainActivity extends AppCompatActivity {

    /*  TO DO
    *   color preference
    *   add degree number transform animation
    *   add find other people's direction
    * */

    private static final String TAG = MainActivity.class.getSimpleName();

//    private Context context;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location lastLocation;
    private ResultReceiver resultReceiver;
    private SensorManager sensorManager;
    private Sensor mSensor;
    private Sensor aSensor;
    private ConstraintLayout layout;
    private CompassView compassView;
    private TextView addressTextView;
    private CusWeatherCardView weatherCardView;
    private WeatherData weatherData;

    private float[] mSensorValue = new float[3];
    private float[] aSensorValue = new float[3];
    private final float[] reverseFloatValue = new float[3];
    private final float[] floatsValues = new float[3];
    private final double[] gpsCoordinates = new double[2]; // 0 : latitude, 1 : longitude
    private final int[] intValues = new int[3];
    private boolean hasPermission = false;
    private boolean isGetAddressSuccess = false;
    private boolean isBackground = false;
    private String addressOutput = "";
    private final String WEATHER_APPID = "1ebecdd87b08f61cb1e122431eceb822";

    private final SensorEventListener listener = new SensorEventListener() {
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

//            Log.d(TAG, "mSensorValue : " + mSensorValue[0] + ", " + mSensorValue[1] + ", " + mSensorValue[2]);
//            Log.d(TAG, "aSensorValue : " + aSensorValue[0] + ", " + aSensorValue[1] + ", " + aSensorValue[2]);
            calculateOrientation();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                Log.d(TAG, "mSensor accuracy : " + i);
            }
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                Log.d(TAG, "aSensor accuracy : " + i);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int MY_COARSE_LOCATION_REQUEST_CODE = 999;
        final int MY_FINE_LOCATION_REQUEST_CODE = 998;

//        context = this;

        layout = findViewById(R.id.mainLayout);
        initComponentView();
        addViewToLayout();

        fusedLocationProviderClient = getFusedLocationProviderClient(MainActivity.this);
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
//                    Log.d(TAG, "found address successful : " + addressOutput);
                    isGetAddressSuccess = true;
                } else  if (resultCode == GeoConstants.FAILURE_RESULT) {
                    addressOutput = "...";
                }
            }
        };

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

    private void initComponentView() {
        // compassView
        compassView = new CompassView(this);

        // addressTextView
        addressTextView = new TextView(this);
        addressTextView.setTextSize(20);
        addressTextView.setVisibility(View.INVISIBLE);
        addressTextView.setTextColor(Color.parseColor("#757575"));

        // weatherCardView
        weatherCardView = new CusWeatherCardView(this);
        weatherCardView.setRadius(16);
        weatherCardView.setBackgroundColor(Color.parseColor("#fafafa"));
        weatherCardView.setAlpha(.95f);
        weatherCardView.setVisibility(View.INVISIBLE);
    }

    private void addViewToLayout() {
        layout.addView(compassView);
        layout.addView(addressTextView);
        layout.addView(weatherCardView);
    }

    private void startLocationUpdates() {

        LocationCallback locationCallback;
        final long LOCATION_REQUEST_INTERVAL = 1000 * 60; // (1000 million * 60) sec
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
        Log.d(TAG, "isBackground : " + isBackground);
        if (!isBackground) {
            Intent intent = new Intent(this, FetchAddressIntentService.class);
            intent.putExtra(GeoConstants.RECEIVER, resultReceiver);
            intent.putExtra(GeoConstants.LOCATION_DATA_EXTRA, lastLocation);
            startService(intent);
        }
    }

    private void onLocationChanged(List<Location> locationList) {
        // new location has now been updated
        for (Location l : locationList) {
            Log.d(TAG, "update location : " + l.getLongitude() + ", " + l.getLatitude());
            gpsCoordinates[0] = l.getLatitude();
            gpsCoordinates[1] = l.getLongitude();
            findWeather(gpsCoordinates[0], gpsCoordinates[1]);

            lastLocation = l;
            startLatLongToAddressService();
        }
    }

    private void findWeather(double lat, double lon) {

        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            HttpUrl.Builder builder = HttpUrl.parse("http://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&APPID=" + WEATHER_APPID).newBuilder();
            Request request = new Request.Builder()
                    .url(builder.toString())
                    .build();

            try {
                Response response = client.newCall(request).execute();

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                weatherData = gson.fromJson(response.body().string(), WeatherData.class);
//                Log.d(TAG, "weather : " + response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
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
        final float LOW_PASS_FILTER_COEFFICIENT = 0.1f;

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

        isBackground = false;

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
//                                    Log.d(TAG, "gpsCoordinates : " + gpsCoordinates[0] + ", " + gpsCoordinates[1]);
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

        isBackground = true;

//        Log.d(TAG, "unregister sensor listener");
        sensorManager.unregisterListener(listener);
    }

    private class CompassView extends View {

        private final Paint framePaint = new Paint();
        private final Paint textPaint = new Paint();
        private final Paint xyPaint = new Paint();
        private final Path scalePath = new Path();
        private final Path xyPath = new Path();
        private final List<Float> compassPoint = new ArrayList<>();
        private final Rect textRect = new Rect();
        private Animator openAnimator;
        private Animator closeAnimator;
        private String degree;
        private int wSize;
        private int hSize;
        private boolean isTouchAddress = false;

        private final float FIX_FRAME_RADIUS = 250;
        private final float DYNAMIC_FRAME_RADIUS = 300;
        private final float DYNAMIC_FRAME_SMALL_CIRCLE_RADIUS = 6;
        private final float DYNAMIC_FRAME_GAP = (float) 360 / 40;
//        private final float BACKGROUND_ALPHA = .25f;
        private final long ANIMATOR_DURATION = 200;
        private final String[] COMPASS_TEXT = {
                "N",
                "E",
                "S",
                "W"
        };

        private CompassView(Context context) {
            super(context);

            initPaints();
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            wSize = MeasureSpec.getSize(widthMeasureSpec);
            hSize = MeasureSpec.getSize(heightMeasureSpec);

            setMeasuredDimension(wSize, hSize);
        }

        private void initPaints() {
            framePaint.setAntiAlias(true);
            framePaint.setStrokeWidth(12);
            framePaint.setStrokeCap(Paint.Cap.ROUND);
            framePaint.setStyle(Paint.Style.STROKE);

            textPaint.setAntiAlias(true);
            textPaint.setStrokeWidth(15);
            textPaint.setTextSize(200);
            textPaint.setStrokeCap(Paint.Cap.ROUND);
            textPaint.setStyle(Paint.Style.FILL);

            xyPaint.setAntiAlias(true);
            xyPaint.setStrokeCap(Paint.Cap.ROUND);
            xyPaint.setStyle(Paint.Style.STROKE);
            xyPaint.setStrokeWidth(2);
            xyPaint.setColor(Color.parseColor("#bdbdbd"));
        }

        private void openWeatherCardView() {
            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

            weatherCardView.setCusCardViewText(getWeatherString());
            weatherCardView.setLayoutParams(layoutParams);
            weatherCardView.setX((wSize - weatherCardView.getWidth()) / 2);
            weatherCardView.setY((hSize - weatherCardView.getHeight()) / 2);
            Log.d(TAG, "card width : " + (wSize - weatherCardView.getWidth()) / 2);
            Log.d(TAG, "card height : " + (hSize - weatherCardView.getHeight()) / 2);
            openCardAnimation();
        }

        private void openCardAnimation() {
            int cx = weatherCardView.getWidth() / 2;
            int cy = weatherCardView.getHeight() / 2;
            float finalRadius = (float) Math.hypot(weatherCardView.getWidth(), weatherCardView.getHeight());

            openAnimator = ViewAnimationUtils.createCircularReveal(weatherCardView, cx, cy, 0, finalRadius);
            openAnimator.setDuration(ANIMATOR_DURATION);
            weatherCardView.setVisibility(VISIBLE);
            openAnimator.start();
        }

        private void closeWeatherCardView() {
            if (openAnimator.isRunning()) {
                openAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        closeCardAnimation();
                    }
                });
            } else {
                closeCardAnimation();
            }
        }

        private void closeCardAnimation() {
            int cx = weatherCardView.getWidth() / 2;
            int cy = weatherCardView.getHeight() / 2;
            float finalRadius = (float) Math.hypot(weatherCardView.getWidth(), weatherCardView.getHeight());

            closeAnimator = ViewAnimationUtils.createCircularReveal(weatherCardView, cx, cy, finalRadius, 0);
            closeAnimator.setDuration(ANIMATOR_DURATION);
            closeAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    weatherCardView.setVisibility(View.INVISIBLE);
                }
            });
            closeAnimator.start();
        }

        private String getWeatherString() {
            if (weatherData != null) {
                Main main = weatherData.getMain();
                Wind wind = weatherData.getWind();

                return  "-------------- Weather Details --------------"
                        + "\n\nTemperature : " + (int)(main.getTemp() - 273.15)
                        + "\nPressure : " + main.getPressure()
                        + "\nHumidity : " + main.getHumidity()
                        + "\nWind speed : " + wind.getSpeed()
                        + "\nWind direction : " + wind.getDegString() + "(" + wind.getDeg() + ")"
                        + "\n\n-------------------------------------------------------";
            } else {
                return "WeatherData is not ready!";
            }
        }

//        private void showBackgroundAnimator(float from, float to) {
//            ValueAnimator animator = ValueAnimator.ofFloat(from, to);
//            animator.addUpdateListener(valueAnimator -> {
//                float alpha = (float) animator.getAnimatedValue();
//                setWindowBackgroundAlpha(alpha);
//            });
//            animator.setDuration(100);
//            animator.start();
//        }

//        private void setWindowBackgroundAlpha(float alpha) {
//            if (context == null) {
//                return;
//            }
//            if (context instanceof Activity) {
//                Window window = ((Activity) context).getWindow();
//                WindowManager.LayoutParams layoutParams = window.getAttributes();
//                layoutParams.alpha = alpha;
//                window.setAttributes(layoutParams);
//            }
//        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // set background color
            canvas.drawColor(Color.parseColor("#111111"));
//            canvas.drawColor(Color.parseColor("#ffffff"));

            drawFixFrame(canvas);
            drawDynamicFrame(canvas);
            drawDegree(canvas);
            drawXYChartHint(canvas);
            drawXYChart(canvas);
            drawLatitudeLongitude(canvas);
            addAddressTextView();
        }

        private void drawFixFrame(Canvas canvas) {
            framePaint.setColor(Color.parseColor("#e0e0e0"));

            setFixFrameScalePath();
            canvas.translate(wSize / 2, hSize / 2);
            canvas.drawCircle(0, 0, FIX_FRAME_RADIUS, framePaint);
            for (int i = 0; i < 4; i++) {
                // draw N, E, S, W scale
                // each time canvas need to rotate 90 degree
                canvas.drawPath(scalePath, framePaint);
                canvas.rotate(90);
            }
        }

        private void setFixFrameScalePath() {
            scalePath.reset();
            scalePath.moveTo(-wSize / 100, -FIX_FRAME_RADIUS);
            scalePath.lineTo(0, -(FIX_FRAME_RADIUS + hSize / 100));
            scalePath.lineTo(wSize / 100, -FIX_FRAME_RADIUS);
            scalePath.close();
        }

        private void drawDynamicFrame(Canvas canvas) {
            canvas.rotate(reverseFloatValue[0]);

            framePaint.setTextSize(70);
            framePaint.setColor(Color.parseColor("#616161"));
            framePaint.getTextBounds(COMPASS_TEXT[0], 0, 1, textRect);

            canvas.drawCircle(0, 0, DYNAMIC_FRAME_RADIUS, framePaint);

            for (float f = 0; f < 360f; f += DYNAMIC_FRAME_GAP) {
                if (Math.abs(floatsValues[0] - f) <= DYNAMIC_FRAME_GAP / 2
                        || (Math.abs(floatsValues[0] - f) < 360 && (Math.abs(floatsValues[0] -f) > 355.5f))) {
                    framePaint.setColor(Color.parseColor("#d32f2f"));
                } else {
                    framePaint.setColor(Color.parseColor("#616161"));
                }

                framePaint.setStrokeWidth(6);
                framePaint.setStyle(Paint.Style.FILL);

                if (f % 90 == 0) {
                    canvas.drawText(COMPASS_TEXT[(int) f / 90], -textRect.width() / 2, -(textRect.height() / 2 + DYNAMIC_FRAME_RADIUS * 1.1f), framePaint);
                } else {
                    if (f % 45 == 0) {
                        canvas.drawCircle(0, -(textRect.height() / 2 + DYNAMIC_FRAME_RADIUS * 1.15f), DYNAMIC_FRAME_SMALL_CIRCLE_RADIUS * 2f, framePaint);
                    } else {
                        canvas.drawCircle(0, -(textRect.height() / 2 + DYNAMIC_FRAME_RADIUS * 1.15f), DYNAMIC_FRAME_SMALL_CIRCLE_RADIUS, framePaint);
                    }
                }

                canvas.rotate(DYNAMIC_FRAME_GAP);
            }

            framePaint.setStyle(Paint.Style.STROKE);
            framePaint.setStrokeWidth(12);

            textRect.setEmpty();
            canvas.rotate(-reverseFloatValue[0]);
        }

        private void drawDegree(Canvas canvas) {
            degree = String.valueOf((int) Math.abs(reverseFloatValue[0]));

            textPaint.setColor(Color.parseColor("#757575"));
            textPaint.getTextBounds(degree, 0, degree.length(), textRect);

            canvas.drawText(degree, -textRect.width() / 2, textRect.height() / 2, textPaint);
            textRect.setEmpty();
        }

        private void drawXYChartHint(Canvas canvas) {
            xyPaint.setColor(Color.parseColor("#616161"));
            xyPaint.setStrokeWidth(1);
            xyPaint.setTextSize(30);

            for (int i = 0; i <= 360; i += 90) {
                canvas.drawText(String.valueOf(i), wSize * .285f, -i - hSize * .256f, xyPaint);
                canvas.drawLine(-wSize * .28f, -i - hSize * 0.26f, wSize * .28f, -i - hSize * .26f, xyPaint);
            }

            xyPaint.setStrokeWidth(2);
            xyPaint.setColor(Color.parseColor("#bdbdbd"));
        }

        private void drawXYChart(Canvas canvas) {
            if (compassPoint.size() == 0) {
                initPoints();
            }
            compassPoint.remove(0);
            compassPoint.add(-hSize * .26f + reverseFloatValue[0]);

            xyPath.moveTo(-wSize * .25f, compassPoint.get(0));
            for (int i = 1; i < compassPoint.size(); i++) {
                xyPath.lineTo(i - wSize * .25f, compassPoint.get(i));
            }
            canvas.drawPath(xyPath, xyPaint);
            xyPath.reset();
        }

        private void initPoints() {
            for (int i = 0; i < wSize / 2; i ++) {
                compassPoint.add(-hSize * .26f);
            }
        }

        private void drawLatitudeLongitude(Canvas canvas) {

            if (hasPermission) {
                textPaint.setTextSize(50);

                // latitude
                String coordinate = String.valueOf(gpsCoordinates[0]);
                coordinate =  addNESWBaseOnCoordinate(0, coordinate);
                textPaint.getTextBounds(coordinate, 0, coordinate.length(), textRect);
                canvas.drawText(coordinate, -textRect.width() / 2, hSize / 2.01f, textPaint);

                // longitude
                coordinate = String.valueOf(gpsCoordinates[1]);
                coordinate =  addNESWBaseOnCoordinate(1, coordinate);
                textPaint.getTextBounds(coordinate, 0, coordinate.length(), textRect);
                canvas.drawText(coordinate, -textRect.width() / 2, hSize / 2 - textRect.height() * 1.4f, textPaint);

                textPaint.setTextSize(200);
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

        private void addAddressTextView() {
            if (hasPermission && isGetAddressSuccess) {
                addressTextView.setLayoutParams(new ConstraintLayout.LayoutParams(wSize, ViewGroup.LayoutParams.WRAP_CONTENT));
                addressTextView.setVisibility(VISIBLE);
                addressTextView.setText(addressOutput);
                addressTextView.setY(hSize * .75f);
                addressTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
                addressTextView.setOnTouchListener((view, motionEvent) -> {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isTouchAddress = true;
//                            showBackgroundAnimator(1f, BACKGROUND_ALPHA);
                            openWeatherCardView();
                            break;
                        case MotionEvent.ACTION_UP:
                            if (isTouchAddress) {
//                                showBackgroundAnimator(BACKGROUND_ALPHA, 1f);
                                closeWeatherCardView();
                            }
                            isTouchAddress = false;
                            break;
                    }
                    performClick();
                    return true;
                });
            }
        }
    }

    private class CusWeatherCardView extends CardView {

        private TextView weatherTextView;

        public CusWeatherCardView(Context context) {
            super(context);

            initWeatherTextView(context);
            addView(weatherTextView);
        }

        private void initWeatherTextView(Context context) {
            CardView.LayoutParams layoutParams = new CardView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.setMargins(50, 20, 50,20);

            weatherTextView = new TextView(context);
            weatherTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            weatherTextView.setLayoutParams(layoutParams);
        }

        private void setCusCardViewText(String info) {
            weatherTextView.setText(info);
        }
    }
}
