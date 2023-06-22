package com.example.actividad_1_parcial_2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    EditText numero;
    Button guardar;
    TelephonyManager telephonyManager;
    PhoneStateListener phoneStateListener;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, BackgroundService.class);
        startService(serviceIntent);


        numero = findViewById(R.id.numero);
        guardar = findViewById(R.id.guardar);

        // Inicializa las preferencias compartidas
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        // Verificar si hay un dato guardado en las preferencias
        if (sharedPreferences.contains("numero_guardado")) {
            String numeroGuardado = sharedPreferences.getString("numero_guardado", "");
            if (!numeroGuardado.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);
                intent.putExtra("numero_guardado", numeroGuardado);
                startActivity(intent);
                finish(); // Finalizar la actividad actual para que no se pueda volver a ella con el botón "Atrás"
            }
        }

        // Registra un receptor de difusión para capturar eventos de llamadas telefónicas
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        registerReceiver(phoneCallReceiver, intentFilter);

        // Inicializa el administrador de telefonía y establece el listener de estado de llamada
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    // Se detecta una llamada entrante
                    numero.setText(incomingNumber);
                }
            }
        };
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        guardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String numeroTexto = numero.getText().toString();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("numero_guardado", numeroTexto);
                editor.apply();
                Toast.makeText(MainActivity.this, "Número guardado en preferencias", Toast.LENGTH_SHORT).show();

                // Iniciar el servicio en segundo plano
                Intent serviceIntent = new Intent(MainActivity.this, BackgroundService.class);
                serviceIntent.putExtra("numero_guardado", numeroTexto);
                startService(serviceIntent);
            }
        });
    }
    // Receptor de difusión para capturar eventos de llamadas telefónicas
    private BroadcastReceiver phoneCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    // Se detecta una llamada entrante
                    String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    numero.setText(incomingNumber);
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Detiene la escucha de eventos de llamadas telefónicas y desregistra el receptor de difusión
        if (telephonyManager != null && phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            unregisterReceiver(phoneCallReceiver);
        }
    }
    // Para iniciar el servicio
}
