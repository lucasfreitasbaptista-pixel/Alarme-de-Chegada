package com.alarmedechegada.app

import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay

/**
 * Tela de escolher o local do alarme num mapa. Usa OpenStreetMap (via
 * biblioteca osmdroid) em vez do Google Maps — funciona igual pro usuário,
 * mas é gratuito e não exige criar chave de API nem cadastrar cartão de
 * crédito em conta do Google Cloud.
 */
class MapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var marcador: Marker? = null
    private var circuloRaio: Polygon? = null
    private var pontoEscolhido: GeoPoint? = null
    private var raioMetros = 300.0

    // Se estiver editando um alarme já existente, guarda o id aqui
    private var alarmeIdEditando: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // osmdroid exige configurar um "user agent" antes de usar o mapa,
        // senão o servidor de mapas pode recusar as requisições
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val idExtra = intent.getLongExtra("alarme_id", -1L)
        val alarmeExistente = if (idExtra != -1L) LocationAlarmStorage.buscar(this, idExtra) else null

        val pontoInicial: GeoPoint
        if (alarmeExistente != null) {
            alarmeIdEditando = alarmeExistente.id
            pontoInicial = GeoPoint(alarmeExistente.latitude, alarmeExistente.longitude)
            raioMetros = alarmeExistente.raioMetros.toDouble()
            findViewById<EditText>(R.id.editNome).setText(alarmeExistente.nome)
            colocarMarcador(pontoInicial)
        } else {
            // Ponto inicial genérico (Brasil) até o usuário tocar no mapa
            pontoInicial = GeoPoint(-15.7801, -47.9292)
        }

        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(pontoInicial)

        val overlayEventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                colocarMarcador(p)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
        mapView.overlays.add(overlayEventos)

        val seekRaio = findViewById<SeekBar>(R.id.seekRaio)
        val txtRaio = findViewById<TextView>(R.id.txtRaio)
        seekRaio.progress = (raioMetros - 100).toInt()
        txtRaio.text = "Raio: ${raioMetros.toInt()} metros"
        seekRaio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                raioMetros = (progress + 100).toDouble() // de 100m a 2000m
                txtRaio.text = "Raio: ${raioMetros.toInt()} metros"
                pontoEscolhido?.let { desenharCirculo(it) }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<Button>(R.id.btnSalvarLocal).setOnClickListener {
            salvarLocal()
        }
    }

    private fun colocarMarcador(ponto: GeoPoint) {
        pontoEscolhido = ponto

        mapView.overlays.remove(marcador)
        marcador = Marker(mapView).apply {
            position = ponto
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        mapView.overlays.add(marcador)

        desenharCirculo(ponto)
        mapView.invalidate()
    }

    private fun desenharCirculo(centro: GeoPoint) {
        mapView.overlays.remove(circuloRaio)
        val pontosDoCirculo = Polygon.pointsAsCircle(centro, raioMetros)
        circuloRaio = Polygon(mapView).apply {
            points = pontosDoCirculo
            fillColor = 0x301976D2
            strokeColor = 0xFF1976D2.toInt()
            strokeWidth = 3f
        }
        mapView.overlays.add(circuloRaio)
        mapView.invalidate()
    }

    private fun salvarLocal() {
        val ponto = pontoEscolhido
        if (ponto == null) {
            Toast.makeText(this, "Toque no mapa para marcar o local primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        val nome = findViewById<EditText>(R.id.editNome).text.toString().trim()
        if (nome.isEmpty()) {
            Toast.makeText(this, "Dê um nome para esse local", Toast.LENGTH_SHORT).show()
            return
        }

        val alarme = LocationAlarme(
            id = alarmeIdEditando ?: System.currentTimeMillis(),
            nome = nome,
            latitude = ponto.latitude,
            longitude = ponto.longitude,
            raioMetros = raioMetros.toFloat(),
            ativo = true
        )

        LocationAlarmStorage.salvar(this, alarme)
        GeofenceHelper.registrar(this, alarme)
        finish()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}
