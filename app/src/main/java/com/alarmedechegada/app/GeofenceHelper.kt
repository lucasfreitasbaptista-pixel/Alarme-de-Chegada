package com.alarmedechegada.app

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * Registra e cancela as "cercas virtuais" (geofences) no sistema, usando o
 * serviço de localização do Google Play Services. Diferente de ficar
 * consultando o GPS toda hora (o que gastaria muita bateria), o sistema
 * operacional monitora essas cercas de forma otimizada e só acorda o app
 * quando o usuário realmente entra na área.
 */
object GeofenceHelper {

    @SuppressLint("MissingPermission")
    fun registrar(context: Context, alarme: LocationAlarme) {
        if (!alarme.ativo) return

        val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

        val geofence = Geofence.Builder()
            .setRequestId(alarme.id.toString())
            .setCircularRegion(alarme.latitude, alarme.longitude, alarme.raioMetros)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, criarPendingIntent(context))
    }

    fun cancelar(context: Context, alarmeId: Long) {
        val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)
        geofencingClient.removeGeofences(listOf(alarmeId.toString()))
    }

    /** Reativa todas as cercas ativas — usado depois de reiniciar o celular. */
    fun registrarTodosAtivos(context: Context) {
        LocationAlarmStorage.listar(context).filter { it.ativo }.forEach { registrar(context, it) }
    }

    private fun criarPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}
