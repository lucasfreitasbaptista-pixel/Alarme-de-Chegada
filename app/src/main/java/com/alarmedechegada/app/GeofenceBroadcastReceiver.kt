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

        val serviceIntent = Intent(context, LocationAlarmService::class.java).apply {
            putExtra("nome_local", alarme.nome)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Dispara uma vez e desativa — evita o alarme tocar de novo se o
        // usuário passar pela mesma área outra vez sem querer reativar.
        LocationAlarmStorage.salvar(context, alarme.copy(ativo = false))
        GeofenceHelper.cancelar(context, alarme.id)
    }
}
