package com.example.sense;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * MainActivity handles the logic for tracking shake speed, managing highscores,
 * and applying a gravity effect to the score.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String CHANNEL_ID = "highscore_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int MAX_PROGRESS = 5000;
    private static final int GRAVITY_DECREASE = 100;
    private static final int GRAVITY_INTERVAL = 1000;

    private TextView tvHighscoreValue;
    private TextView tvLastAttemptValue;
    private ProgressBar pbHighscore;
    private CheckBox cbGravity;
    private Button btnRestart;

    private SensorManager sensorManager;
    private Sensor accelerationSensor;
    
    private int highscore = 0;
    private int lastAttempt = 0;
    
    private Handler gravityHandler;
    private Runnable gravityRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeVariables();
        createNotificationChannel();
        setupGravityTask();
    }

    /**
     * Initializes all UI components, sensors, and listeners.
     */
    private void initializeVariables() {
        tvHighscoreValue = findViewById(R.id.tvHighscoreValue);
        tvLastAttemptValue = findViewById(R.id.tvLastAttemptValue);
        pbHighscore = findViewById(R.id.pbHighscore);
        cbGravity = findViewById(R.id.cbGravity);
        btnRestart = findViewById(R.id.btnRestart);

        pbHighscore.setMax(MAX_PROGRESS);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }

        btnRestart.setOnClickListener(v -> {
            highscore = 0;
            updateUI();
        });

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    /**
     * Sets up the recurring task for gravity decrease.
     */
    private void setupGravityTask() {
        gravityHandler = new Handler(Looper.getMainLooper());
        gravityRunnable = new Runnable() {
            @Override
            public void run() {
                if (cbGravity.isChecked() && highscore > 0) {
                    highscore = Math.max(0, highscore - GRAVITY_DECREASE);
                    updateUI();
                }
                gravityHandler.postDelayed(this, GRAVITY_INTERVAL);
            }
        };
        gravityHandler.postDelayed(gravityRunnable, GRAVITY_INTERVAL);
    }

    /**
     * Updates the UI elements with current highscore and last attempt values.
     */
    private void updateUI() {
        tvHighscoreValue.setText(String.valueOf(highscore));
        tvLastAttemptValue.setText(String.valueOf(lastAttempt));
        pbHighscore.setProgress(highscore);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            // Focus on horizontal (X-axis) movement as per exercise description
            float xAccel = Math.abs(event.values[0]);
            
            // Scaling factor to map acceleration to the 0-5000 range
            int currentSpeed = (int) (xAccel * 50); 

            if (currentSpeed > 50) { 
                lastAttempt = currentSpeed;
                if (currentSpeed > highscore) {
                    highscore = Math.min(currentSpeed, MAX_PROGRESS);
                    sendHighscoreNotification();
                }
                updateUI();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    /**
     * Sends a notification when a new highscore is achieved.
     */
    private void sendHighscoreNotification() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("New Highscore!")
                    .setContentText("You reached a speed of " + highscore)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    /**
     * Creates the notification channel.
     */
    private void createNotificationChannel() {
        CharSequence name = "Highscore Channel";
        String description = "Notifications for new highscores";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerationSensor != null) {
            sensorManager.registerListener(this, accelerationSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gravityHandler != null) {
            gravityHandler.removeCallbacks(gravityRunnable);
        }
    }
}