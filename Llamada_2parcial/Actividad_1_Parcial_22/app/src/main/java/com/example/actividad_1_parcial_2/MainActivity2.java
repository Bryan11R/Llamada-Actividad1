package com.example.actividad_1_parcial_2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
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
                    // Obtener las coordenadas actuales
                    double latitude = 0.0;  // Valor de latitud predeterminado
                    double longitude = 0.0; // Valor de longitud predeterminado

                    // Verificar si se ha obtenido la última ubicación conocida
                    if (ActivityCompat.checkSelfPermission(MainActivity2.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastKnownLocation != null) {
                            latitude = lastKnownLocation.getLatitude();
                            longitude = lastKnownLocation.getLongitude();
                        }
                    }

                    // Llamar al método sendMessageWithCoordinates con las coordenadas
                    sendMessageWithCoordinates(latitude, longitude);
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
        // Componer el mensaje con las coordenadas
        String message = "Mis coordenadas son: " + latitude + ", " + longitude;

        // Verificar si la aplicación tiene permiso para enviar mensajes de texto
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // Solicitar permiso para enviar mensajes de texto
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 3);
        } else {
            // Enviar el mensaje de texto automáticamente
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
        // Actualizar las coordenadas en la interfaz de usuario
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        coordenadasTextView.setText("Latitud: " + latitude + ", Longitud: " + longitude);

        // Enviar mensaje después de 7 segundos si la llamada no ha sido contestada
        if (!callAnswered) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Verificar nuevamente si la llamada ha sido contestada
                    if (!callAnswered) {
                        // Obtener las coordenadas actuales
                        double currentLatitude = location.getLatitude();
                        double currentLongitude = location.getLongitude();

                        // Verificar si las coordenadas actuales son diferentes de las anteriores
                        if (latitude != currentLatitude || longitude != currentLongitude) {
                            // Llamar al método sendMessageWithCoordinates con las coordenadas
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
                        // Resetear el indicador de respuesta de llamada
                        callAnswered = false;
                        // Verificar si el número de teléfono coincide con el número guardado
                        String numeroGuardado = numeroGuardadoTextView.getText().toString();
                        if (phoneNumber.equals(numeroGuardado)) {
                            // Iniciar el temporizador de 7 segundos si los números coinciden
                            handler.postDelayed(runnable, 7000);
                        } else {
                            // Mostrar notificación de número no coincidente
                            showNumberMismatchNotification();
                        }
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        Toast.makeText(MainActivity2.this, "Llamada saliente: " + phoneNumber, Toast.LENGTH_SHORT).show();
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        Toast.makeText(MainActivity2.this, "Llamada finalizada", Toast.LENGTH_SHORT).show();
                        // Detener el temporizador si la llamada ha sido contestada
                        if (!callAnswered) {
                            handler.removeCallbacks(runnable);
                        }
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void showNumberMismatchNotification() {
        // Mostrar una notificación indicando que el número de teléfono no coincide
        Toast.makeText(MainActivity2.this, "El número no coincide", Toast.LENGTH_SHORT).show();
    }

}
