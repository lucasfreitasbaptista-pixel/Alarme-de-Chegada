package com.alarmedechegada.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val evento = GeofencingEvent.fromIntent(intent) ?: return
        if (evento.hasError()) return
        if (evento.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val idDisparado = evento.triggeringGeofences?.firstOrNull()?.requestId ?: return
        val alarme = LocationAlarmStorage.buscar(context, idDisparado.toLong()) ?: return
        if (!alarme.ativo) return

        // Respeita o dia da semana e o horário configurados. Se estiver fora
        // da janela permitida, ignora silenciosamente — o geofence continua
        // registrado, e pode disparar normalmente na próxima vez que a
        // condição de dia/horário for satisfeita.
        if (!alarme.permiteAgora()) return

        val serviceIntent = Intent(context, LocationAlarmService::class.java).apply {
            putExtra("nome_local", alarme.nome)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        if (alarme.modo == LocationAlarme.MODO_SO_HOJE) {
            // Toca uma vez só e desativa, igual ao modo "só hoje" do Super Despertador
            LocationAlarmStorage.salvar(context, alarme.copy(ativo = false))
            GeofenceHelper.cancelar(context, alarme.id)
        }
        // Nos modos "todos os dias" e "dias de semana", o geofence continua
        // ativo — pode disparar de novo em outro dia que bata com a regra.
    }
}
