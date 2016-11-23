package com.iisc.distancecamera;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Intent;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.MotionEvent;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class Two extends Activity implements SensorEventListener, SurfaceHolder.Callback {

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;
    LayoutInflater controlInflater = null;
    double c;
    static int r = 0;
    static List<Integer> height3;
    double height10 = 1.4;
    double distance10 = 1.4;
    TextView angle;
    TextView text;
    static int i = 0;
    String temp;
    int temp1 = 0;
    double k, theta1 = 0;
    int touch = 0;
    private SensorManager mSensorManager = null;

    // angular speeds from gyro
    private float[] gyro = new float[3];

    // rotation matrix from gyro data
    private float[] gyroMatrix = new float[9];

    // orientation angles from gyro matrix
    private float[] gyroOrientation = new float[3];

    // magnetic field vector
    private float[] magnet = new float[3];

    // accelerometer vector
    private float[] accel = new float[3];

    // orientation angles from accel and magnet
    private float[] accMagOrientation = new float[3];

    // final orientation angles from sensor fusion
    private float[] fusedOrientation = new float[3];

    // accelerometer and magnetometer based rotation matrix
    private float[] rotationMatrix = new float[9];

    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp;
    private boolean initState = true;

    public static MyCountDownTimer countDownTimer;
    public static final int TIME_CONSTANT = 30;
    public static final float FILTER_COEFFICIENT = 0.98f;
    private Timer fuseTimer = new Timer();

    // The following members are only for displaying the sensor output.

    // private RadioGroup mRadioGroup;
    private TextView mAzimuthView;
    private TextView mPitchView;
    private TextView mRollView;
    // private int radioSelection;
    DecimalFormat d = new DecimalFormat("#.##");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        String ht = i.getStringExtra("height");
        String dis = i.getStringExtra("distance");
        temp = i.getStringExtra("temp");

        try {
            temp1 = Integer.parseInt(temp);
        } catch (NumberFormatException e) {
            temp1 = 0; // Code to execute if the entered value is not a number
        }
        if (temp1 == 0) {
            try {
                height10 = Double.parseDouble(ht);
            } catch (NumberFormatException e) {
                height10 = 1.4; // Code to execute if the entered value is not a
                                // number
            }
        } else {
            try {
                distance10 = Double.parseDouble(dis);
            } catch (NumberFormatException e) {
                distance10 = 1.4; // Code to execute if the entered value is not
                                  // a number
            }
        }

        if (temp1 == 0)
            System.out.println("correct");
        else
            System.out.println("wrong" + temp);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        System.out.println("sachin");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        getWindow().setFormat(PixelFormat.UNKNOWN);

        surfaceView = (SurfaceView) findViewById(R.id.camerapreview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        controlInflater = LayoutInflater.from(getBaseContext());
        View viewControl = controlInflater.inflate(R.layout.control, null);
        LayoutParams layoutParamsControl = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
        this.addContentView(viewControl, layoutParamsControl);

        text = (TextView) this.findViewById(R.id.timer);

        Button write1 = (Button) findViewById(R.id.button1);
        write1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Camera.Parameters parameters = camera.getParameters();
                // android.hardware.Camera.Size size =
                // parameters.getPictureSize();

                height3 = parameters.getZoomRatios();
                int zoom = parameters.getMaxZoom();
                if (r != (zoom))
                    r++;
                else
                    r = 0;

                // Log.d(TAG, "Zoom " + zoom);
                parameters.setZoom(r);
                camera.setParameters(parameters);

            }
        });

        Button write2 = (Button) findViewById(R.id.button2);
        write2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Camera.Parameters parameters = camera.getParameters();
                // android.hardware.Camera.Size size =
                // parameters.getPictureSize();

                height3 = parameters.getZoomRatios();
                int zoom = parameters.getMaxZoom();
                if (r > 1)
                    r--;
                else
                    r = 0;

                // Log.d(TAG, "Zoom " + zoom);
                parameters.setZoom(r);
                camera.setParameters(parameters);

            }
        });

        // Binding Click event to Button
        Button button3 = (Button) findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {

            public void onClick(View arg0) {
                // Closing SecondScreen Activity
                camera.stopPreview();
                finish();
            }
        });

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // initialise gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f;
        gyroMatrix[1] = 0.0f;
        gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f;
        gyroMatrix[4] = 1.0f;
        gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f;
        gyroMatrix[7] = 0.0f;
        gyroMatrix[8] = 1.0f;

        // get sensorManager and initialise sensor listeners
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        initListeners();

        // wait for one second until gyroscope and magnetometer/accelerometer
        // data is initialised then scedule the complementary filter task
        fuseTimer.scheduleAtFixedRate(new calculateFusedOrientationTask(), 1000, TIME_CONSTANT);

        // GUI stuff
        // mHandler = new Handler();
        // radioSelection = 0;
        d.setRoundingMode(RoundingMode.HALF_UP);
        d.setMaximumFractionDigits(3);
        d.setMinimumFractionDigits(3);
        // mRadioGroup = (RadioGroup)findViewById(R.id.radioGroup1);
        // mAzimuthView = (TextView)findViewById(R.id.textView4);
        // mPitchView = (TextView)findViewById(R.id.textView5);
        // mRollView = (TextView)findViewById(R.id.textView6);
        // mRadioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        // unregister sensor listeners to prevent the activity from draining the
        // device's battery.
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister sensor listeners to prevent the activity from draining the
        // device's battery.
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // restore the sensor listeners when user resumes the application.
        initListeners();
    }

    // This function registers sensor listeners for the accelerometer,
    // magnetometer and gyroscope.
    public void initListeners() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
        case Sensor.TYPE_ACCELEROMETER:
            // copy new accelerometer data into accel array and calculate
            // orientation
            System.arraycopy(event.values, 0, accel, 0, 3);
            calculateAccMagOrientation();
            break;

        case Sensor.TYPE_GYROSCOPE:
            // process gyro data
            gyroFunction(event);
            break;

        case Sensor.TYPE_MAGNETIC_FIELD:
            // copy new magnetometer data into magnet array
            System.arraycopy(event.values, 0, magnet, 0, 3);
            break;
        }
        k = (Math.round((-fusedOrientation[2]) * 100.0) / 100.0);
        if (temp1 == 0) {

            c = Math.tan(k) * height10;
            angle = (TextView) findViewById(R.id.anglee);
            angle.setText(String.valueOf(c));
        } else {
            // if()//Toast.makeText(this,Double.toString(c),Toast.LENGTH_SHORT).show();
        }
    }

    // calculates orientation angles from accelerometer and magnetometer output
    public void calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation);
        }
    }

    // This function is borrowed from the Android reference
    // at
    // http://developer.android.com/reference/android/hardware/SensorEvent.html#values
    // It calculates a rotation vector from the gyroscope angular speed values.
    private void getRotationVectorFromGyro(float[] gyroValues, float[] deltaRotationVector, float timeFactor) {
        float[] normValues = new float[3];

        // Calculate the angular speed of the sample
        float omegaMagnitude = (float) Math
                .sqrt(gyroValues[0] * gyroValues[0] + gyroValues[1] * gyroValues[1] + gyroValues[2] * gyroValues[2]);

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
        System.out.println("hai");
    }

    // This function performs the integration of the gyroscope data.
    // It writes the gyroscope based orientation into gyroOrientation.
    public void gyroFunction(SensorEvent event) {
        // don't start until first accelerometer/magnetometer orientation has
        // been acquired
        if (accMagOrientation == null)
            return;

        // initialisation of the gyroscope based rotation matrix
        if (initState) {
            float[] initMatrix = new float[9];
            initMatrix = getRotationMatrixFromOrientation(accMagOrientation);
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            gyroMatrix = matrixMultiplication(gyroMatrix, initMatrix);
            initState = false;
        }

        // copy the new gyro values into the gyro array
        // convert the raw gyro data into a rotation vector
        float[] deltaVector = new float[4];
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            System.arraycopy(event.values, 0, gyro, 0, 3);
            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // measurement done, save current time for next interval
        timestamp = event.timestamp;

        // convert rotation vector into rotation matrix
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // apply the new rotation interval on the gyroscope based rotation
        // matrix
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // get the gyroscope based orientation from the rotation matrix
        SensorManager.getOrientation(gyroMatrix, gyroOrientation);
    }

    private float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float) Math.sin(o[1]);
        float cosX = (float) Math.cos(o[1]);
        float sinY = (float) Math.sin(o[2]);
        float cosY = (float) Math.cos(o[2]);
        float sinZ = (float) Math.sin(o[0]);
        float cosZ = (float) Math.cos(o[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f;
        xM[1] = 0.0f;
        xM[2] = 0.0f;
        xM[3] = 0.0f;
        xM[4] = cosX;
        xM[5] = sinX;
        xM[6] = 0.0f;
        xM[7] = -sinX;
        xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY;
        yM[1] = 0.0f;
        yM[2] = sinY;
        yM[3] = 0.0f;
        yM[4] = 1.0f;
        yM[5] = 0.0f;
        yM[6] = -sinY;
        yM[7] = 0.0f;
        yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ;
        zM[1] = sinZ;
        zM[2] = 0.0f;
        zM[3] = -sinZ;
        zM[4] = cosZ;
        zM[5] = 0.0f;
        zM[6] = 0.0f;
        zM[7] = 0.0f;
        zM[8] = 1.0f;

        // rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    class calculateFusedOrientationTask extends TimerTask {
        public void run() {
            float oneMinusCoeff = 1.0f - FILTER_COEFFICIENT;

            /*
             * Fix for 179� <--> -179� transition problem: Check whether one
             * of the two orientation angles (gyro or accMag) is negative while
             * the other one is positive. If so, add 360� (2 * math.PI) to the
             * negative value, perform the sensor fusion, and remove the 360�
             * from the result if it is greater than 180�. This stabilizes the
             * output in positive-to-negative-transition cases.
             */

            // azimuth
            if (gyroOrientation[0] < -0.5 * Math.PI && accMagOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * (gyroOrientation[0] + 2.0 * Math.PI)
                        + oneMinusCoeff * accMagOrientation[0]);
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            } else if (accMagOrientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
                fusedOrientation[0] = (float) (FILTER_COEFFICIENT * gyroOrientation[0]
                        + oneMinusCoeff * (accMagOrientation[0] + 2.0 * Math.PI));
                fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI : 0;
            } else {
                fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0] + oneMinusCoeff * accMagOrientation[0];
            }

            // pitch
            if (gyroOrientation[1] < -0.5 * Math.PI && accMagOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * (gyroOrientation[1] + 2.0 * Math.PI)
                        + oneMinusCoeff * accMagOrientation[1]);
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            } else if (accMagOrientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
                fusedOrientation[1] = (float) (FILTER_COEFFICIENT * gyroOrientation[1]
                        + oneMinusCoeff * (accMagOrientation[1] + 2.0 * Math.PI));
                fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI : 0;
            } else {
                fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1] + oneMinusCoeff * accMagOrientation[1];
            }

            // roll
            if (gyroOrientation[2] < -0.5 * Math.PI && accMagOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * (gyroOrientation[2] + 2.0 * Math.PI)
                        + oneMinusCoeff * accMagOrientation[2]);
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            } else if (accMagOrientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
                fusedOrientation[2] = (float) (FILTER_COEFFICIENT * gyroOrientation[2]
                        + oneMinusCoeff * (accMagOrientation[2] + 2.0 * Math.PI));
                fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI : 0;
            } else {
                fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2] + oneMinusCoeff * accMagOrientation[2];
            }

            // overwrite gyro matrix and orientation with fused orientation
            // to comensate gyro drift
            gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);
            System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
        if (previewing) {
            camera.stopPreview();
            previewing = false;
        }

        if (camera != null) {
            try {

                RelativeLayout rlayout = (RelativeLayout) findViewById(R.id.button_bar);

                ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
                params.width = (int) (rlayout.getWidth() / 1);
                surfaceView.setLayoutParams(params);

                camera.setPreviewDisplay(surfaceHolder);

                camera.startPreview();
                previewing = true;
            }

            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        TextView text1 = (TextView) findViewById(R.id.textView1);

        if (temp1 == 0) {
            CountDownTimer countDownTimer;

            Button startB;
            TextView text;
            final long startTime = 5 * 1000;
            final long interval = 1 * 1000;

            // MotionEvent object holds X-Y values
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                i = 0;
                countDownTimer = new MyCountDownTimer(startTime, interval);

                if (i == 0) {
                    countDownTimer.start();
                }

                if (i == 1)
                    countDownTimer.cancel();
            }
        } else {
            if ((event.getAction() == MotionEvent.ACTION_DOWN)) {
                if (touch == 0) {

                    text1.setText("Point to the top and touch the screen");
                    theta1 = k;
                    touch = 1;
                }

                else {

                    touch = 0;
                    c = distance10 * ((1 / Math.tan(theta1)) - (1 / Math.tan(k)));

                    angle = (TextView) findViewById(R.id.anglee);

                    CountDownTimer countDownTimer;

                    Button startB;
                    TextView text;
                    final long startTime = 5 * 1000;
                    final long interval = 1 * 1000;

                    // MotionEvent object holds X-Y values
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        i = 0;
                        countDownTimer = new MyCountDownTimer(startTime, interval);

                        if (i == 0) {
                            countDownTimer.start();
                        }

                        if (i == 1)
                            countDownTimer.cancel();
                    }
                    angle.setText(String.valueOf(c));
                    text1.setText("Point to the bottom and touch the screen and freez for 5s");
                }

            }

        }

        return super.onTouchEvent(event);
    }

    public class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long startTime, long interval) {
            super(startTime, interval);
        }

        @Override
        public void onFinish() {
            text.setText("");
            i = 1;

        }

        @Override
        public void onTick(long millisUntilFinished) {
            text.setText("" + millisUntilFinished / 1000);
        }
    }

}
