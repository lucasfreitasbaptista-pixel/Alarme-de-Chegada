package com.alarmedechegada.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var containerLocais: LinearLayout

    private val pedirLocalizacaoFina = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) {
            pedirLocalizacaoSegundoPlano()
        } else {
            continuarFluxoDePermissoes()
        }
    }

    private val pedirLocalizacaoSegundoPlanoLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        continuarFluxoDePermissoes()
    }

    private val pedirNotificacoes = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        continuarFluxoDePermissoes()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        containerLocais = findViewById(R.id.containerLocais)

        findViewById<Button>(R.id.btnNovoLocal).setOnClickListener {
            startActivity(Intent(this, MapPickerActivity::class.java))
        }

        pedirTudoNaPrimeiraAbertura()
    }

    override fun onResume() {
        super.onResume()
        atualizarLista()
    }

    private fun atualizarLista() {
        containerLocais.removeAllViews()
        val locais = LocationAlarmStorage.listar(this)
        val inflater = LayoutInflater.from(this)

        for (alarme in locais) {
            val item = inflater.inflate(R.layout.item_local_alarme, containerLocais, false)

            val txtNome = item.findViewById<TextView>(R.id.txtNome)
            val txtRaio = item.findViewById<TextView>(R.id.txtRaio)
            val switchAtivo = item.findViewById<Switch>(R.id.switchAtivo)
            val btnExcluir = item.findViewById<TextView>(R.id.btnExcluir)
            val areaClicavel = item.findViewById<LinearLayout>(R.id.areaClicavel)

            txtNome.text = alarme.nome
            txtRaio.text = "Raio de ${alarme.raioMetros.toInt()} metros"
            switchAtivo.isChecked = alarme.ativo

            switchAtivo.setOnCheckedChangeListener { _, isChecked ->
                val atualizado = alarme.copy(ativo = isChecked)
                LocationAlarmStorage.salvar(this, atualizado)
                if (isChecked) {
                    GeofenceHelper.registrar(this, atualizado)
                } else {
                    GeofenceHelper.cancelar(this, atualizado.id)
                }
            }

            btnExcluir.setOnClickListener {
                GeofenceHelper.cancelar(this, alarme.id)
                LocationAlarmStorage.remover(this, alarme.id)
                atualizarLista()
            }

            areaClicavel.setOnClickListener {
                val intent = Intent(this, MapPickerActivity::class.java)
                intent.putExtra("alarme_id", alarme.id)
                startActivity(intent)
            }

            containerLocais.addView(item)
        }
    }

    private fun pedirTudoNaPrimeiraAbertura() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pedirLocalizacaoFina.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            pedirLocalizacaoSegundoPlano()
        }
    }

    private fun pedirLocalizacaoSegundoPlano() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // No Android 10+, essa permissão precisa ser pedida numa etapa
            // separada da localização "normal" — o próprio sistema cuida de
            // mostrar a tela certa (às vezes um diálogo, às vezes direto nas
            // configurações do app, dependendo da versão do Android).
            pedirLocalizacaoSegundoPlanoLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            continuarFluxoDePermissoes()
        }
    }

    private fun continuarFluxoDePermissoes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pedirNotificacoes.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        pedirAlarmeExato()
        pedirPermissaoTelaCheia()
        solicitarIgnorarOtimizacaoBateria()
        AutoStartHelper.tentarAbrirUmaVez(this)
        AutoStartHelper.tentarAbrirEconomiaBateriaMiui(this)
    }

    private fun pedirAlarmeExato() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun pedirPermissaoTelaCheia() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager?.canUseFullScreenIntent() == false) {
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT))
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun solicitarIgnorarOtimizacaoBateria() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
            }
        }
    }
}
