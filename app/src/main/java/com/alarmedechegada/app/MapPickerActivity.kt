package com.alarmedechegada.app

import android.app.TimePickerDialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.views.overlay.MapEventsOverlay
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Tela de escolher o local do alarme num mapa. Usa OpenStreetMap (via
 * biblioteca osmdroid) em vez do Google Maps — funciona igual pro usuário,
 * mas é gratuito e não exige criar chave de API nem cadastrar cartão de
 * crédito em conta do Google Cloud.
 *
 * A busca de endereço usa o Nominatim, o serviço de geocodificação gratuito
 * do próprio projeto OpenStreetMap — também sem chave de API.
 */
class MapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var marcador: Marker? = null
    private var circuloRaio: Polygon? = null
    private var pontoEscolhido: GeoPoint? = null
    private var raioMetros = 300.0

    private var modoSelecionado = LocationAlarme.MODO_TODOS_OS_DIAS
    private var horaInicio = 0
    private var minutoInicio = 0
    private var horaFim = 23
    private var minutoFim = 59

    private var alarmeIdEditando: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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
            modoSelecionado = alarmeExistente.modo
            horaInicio = alarmeExistente.horaInicio
            minutoInicio = alarmeExistente.minutoInicio
            horaFim = alarmeExistente.horaFim
            minutoFim = alarmeExistente.minutoFim
            findViewById<EditText>(R.id.editNome).setText(alarmeExistente.nome)
            colocarMarcador(pontoInicial)
        } else {
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

        configurarBusca()
        configurarRaio()
        configurarModo()
        configurarHorarios()

        findViewById<Button>(R.id.btnSalvarLocal).setOnClickListener {
            salvarLocal()
        }
    }

    private fun configurarBusca() {
        val editBusca = findViewById<EditText>(R.id.editBusca)
        findViewById<Button>(R.id.btnBuscar).setOnClickListener {
            val consulta = editBusca.text.toString().trim()
            if (consulta.isNotEmpty()) buscarEndereco(consulta)
        }
    }

    private fun buscarEndereco(consulta: String) {
        Toast.makeText(this, "Buscando...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val urlTexto = "https://nominatim.openstreetmap.org/search?q=" +
                    URLEncoder.encode(consulta, "UTF-8") + "&format=json&limit=1"
                val conexao = URL(urlTexto).openConnection() as HttpURLConnection
                // Nominatim exige um User-Agent identificando o app (política de uso do serviço gratuito)
                conexao.setRequestProperty("User-Agent", packageName)
                conexao.connectTimeout = 8000
                conexao.readTimeout = 8000

                val resposta = conexao.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(resposta)

                runOnUiThread {
                    if (array.length() == 0) {
                        Toast.makeText(this, "Endereço não encontrado", Toast.LENGTH_SHORT).show()
                    } else {
                        val resultado = array.getJSONObject(0)
                        val lat = resultado.getString("lat").toDouble()
                        val lon = resultado.getString("lon").toDouble()
                        val ponto = GeoPoint(lat, lon)
                        mapView.controller.setZoom(16.0)
                        mapView.controller.animateTo(ponto)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Não foi possível buscar (verifique a internet)", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun configurarRaio() {
        val seekRaio = findViewById<SeekBar>(R.id.seekRaio)
        val txtRaio = findViewById<TextView>(R.id.txtRaio)
        seekRaio.progress = (raioMetros - 100).toInt()
        txtRaio.text = "Raio: ${raioMetros.toInt()} metros"
        seekRaio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                raioMetros = (progress + 100).toDouble()
                txtRaio.text = "Raio: ${raioMetros.toInt()} metros"
                pontoEscolhido?.let { desenharCirculo(it) }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun configurarModo() {
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupModo)
        when (modoSelecionado) {
            LocationAlarme.MODO_DIAS_DE_SEMANA -> radioGroup.check(R.id.radioSemana)
            LocationAlarme.MODO_SO_HOJE -> radioGroup.check(R.id.radioHoje)
            else -> radioGroup.check(R.id.radioTodos)
        }
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            modoSelecionado = when (checkedId) {
                R.id.radioSemana -> LocationAlarme.MODO_DIAS_DE_SEMANA
                R.id.radioHoje -> LocationAlarme.MODO_SO_HOJE
                else -> LocationAlarme.MODO_TODOS_OS_DIAS
            }
        }
    }

    private fun configurarHorarios() {
        val btnInicio = findViewById<Button>(R.id.btnHorarioInicio)
        val btnFim = findViewById<Button>(R.id.btnHorarioFim)

        btnInicio.text = "Início: %02d:%02d".format(horaInicio, minutoInicio)
        btnFim.text = "Fim: %02d:%02d".format(horaFim, minutoFim)

        btnInicio.setOnClickListener {
            TimePickerDialog(this, { _, hora, minuto ->
                horaInicio = hora
                minutoInicio = minuto
                btnInicio.text = "Início: %02d:%02d".format(hora, minuto)
            }, horaInicio, minutoInicio, true).show()
        }

        btnFim.setOnClickListener {
            TimePickerDialog(this, { _, hora, minuto ->
                horaFim = hora
                minutoFim = minuto
                btnFim.text = "Fim: %02d:%02d".format(hora, minuto)
            }, horaFim, minutoFim, true).show()
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
            ativo = true,
            modo = modoSelecionado,
            horaInicio = horaInicio,
            minutoInicio = minutoInicio,
            horaFim = horaFim,
            minutoFim = minutoFim
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
