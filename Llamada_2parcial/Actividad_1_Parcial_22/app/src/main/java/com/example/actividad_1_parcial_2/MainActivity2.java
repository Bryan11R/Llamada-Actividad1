package com.example.actividad_1_parcial_2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity2 extends AppCompatActivity implements LocationListener {

    private TextView numeroGuardadoTextView;
    private TextView coordenadasTextView;
    private LocationManager locationManager;
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private Handler handler;
    private Runnable runnable;
    private boolean callAnswered = false;
    private String incomingPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Disable the keyguard (lock screen)
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        numeroGuardadoTextView = findViewById(R.id.numero_guardado);
        Intent intent = getIntent();
        if (intent != null) {
            incomingPhoneNumber = intent.getStringExtra("numero_guardado");
            numeroGuardadoTextView.setText(incomingPhoneNumber);
        }

        coordenadasTextView = findViewById(R.id.coordenadasTextView);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            startCallDetection();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 2);
        }

        startService();

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                if (!callAnswered) {
                    // Obtain the current coordinates
                    double latitude = 0.0;  // Default latitude value
                    double longitude = 0.0; // Default longitude value

                    if (ActivityCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation != null) {
                            latitude = lastKnownLocation.getLatitude();
                            longitude = lastKnownLocation.getLongitude();
                        }
                    }

                    // Call the sendMessageWithCoordinates method with the coordinates
                    sendMessageWithCoordinates(latitude, longitude);

                    // Call back to the same number
                    callNumber(incomingPhoneNumber);

                    // Silence the call
                    silenceCall();
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isGPSEnabled()) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        stopCallDetection();
        handler.removeCallbacks(runnable);
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        serviceIntent.putExtra("numero_guardado", incomingPhoneNumber);
        startService(serviceIntent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        stopService(serviceIntent);
    }

    private void startLocationUpdates() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
    }

    private void stopLocationUpdates() {
        locationManager.removeUpdates(this);
    }

    private boolean isGPSEnabled() {
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void stopCallDetection() {
        if (telephonyManager != null && phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private void sendMessageWithCoordinates(double latitude, double longitude) {
        String message = "Mis coordenadas son: " + latitude + ", " + longitude;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 3);
        } else {
            sendSMS(incomingPhoneNumber, message);
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al enviar el mensaje", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        coordenadasTextView.setText("Latitud: " + latitude + ", Longitud: " + longitude);

        if (!callAnswered) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!callAnswered) {
                        double currentLatitude = location.getLatitude();
                        double currentLongitude = location.getLongitude();

                        if (latitude != currentLatitude || longitude != currentLongitude) {
                            sendMessageWithCoordinates(currentLatitude, currentLongitude);
                        }
                    }
                }
            }, 7000);
        }
    }

    private void startCallDetection() {
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        Toast.makeText(MainActivity2.this, "Llamada entrante: " + phoneNumber, Toast.LENGTH_SHORT).show();
                        callAnswered = false;
                        String numeroGuardado = numeroGuardadoTextView.getText().toString();
                        if (phoneNumber.equals(numeroGuardado)) {
                            handler.postDelayed(runnable, 7000);
                        } else {
                            showNumberMismatchNotification();
                        }
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Toast.makeText(MainActivity2.this, "Llamada saliente: " + phoneNumber, Toast.LENGTH_SHORT).show();
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        Toast.makeText(MainActivity2.this, "Llamada terminada", Toast.LENGTH_SHORT).show();
                        callAnswered = true;
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void callNumber(String phoneNumber) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(callIntent);
    }

    private void silenceCall() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
    }

    private void showNumberMismatchNotification() {
        // Code to show a notification when the incoming number doesn't match the saved number
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                }
                break;
            case 2:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCallDetection();
                }
                break;
            case 3:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendMessageWithCoordinates(0.0, 0.0);
                }
                break;
        }
    }
}
