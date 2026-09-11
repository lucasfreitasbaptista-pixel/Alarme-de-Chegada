package com.alarmedechegada.app

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Tela do alarme de chegada. Fica em tela cheia, por cima do bloqueio.
 *
 * Diferente do Super Despertador (que exige caminhar), aqui isso não faz
 * sentido — a pessoa pode estar sentada num ônibus ou trem. Em vez disso,
 * o botão de desligar precisa ser SEGURADO por alguns segundos, o que evita
 * desligar no reflexo, meio dormindo, sem realmente notar a chegada.
 */
class LocationAlarmActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var segurando = false
    private val TEMPO_SEGURAR_MS = 2000L

    private val runnableConcluir = Runnable {
        if (segurando) {
            concluirAlarme()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurarTelaSobreLockscreen()
        setContentView(R.layout.activity_location_alarm)

        findViewById<TextView>(R.id.txtNomeLocal).text =
            "Chegando em ${LocationAlarmService.nomeLocalAtual}"

        val botao = findViewById<Button>(R.id.btnSegurarDesligar)
        botao.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    segurando = true
                    botao.text = "Continue segurando..."
                    handler.postDelayed(runnableConcluir, TEMPO_SEGURAR_MS)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    segurando = false
                    botao.text = "Segure para desligar"
                    handler.removeCallbacks(runnableConcluir)
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        if (!LocationAlarmService.tocando) {
            finish()
        }
    }

    private fun concluirAlarme() {
        LocationAlarmService.pararAlarme(this)
        finish()
    }

    private fun configurarTelaSobreLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
    }

    override fun onBackPressed() {
        // intencionalmente vazio — só sai segurando o botão
    }
}
