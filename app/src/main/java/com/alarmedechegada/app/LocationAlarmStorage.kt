package com.alarmedechegada.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LocationAlarmStorage {

    private const val PREF = "alarme_chegada"
    private const val CHAVE = "locais_json"

    fun listar(context: Context): List<LocationAlarme> {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(CHAVE, "[]") ?: "[]"
        val array = JSONArray(json)
        val lista = mutableListOf<LocationAlarme>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            lista.add(
                LocationAlarme(
                    id = obj.getLong("id"),
                    nome = obj.getString("nome"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    raioMetros = obj.getDouble("raioMetros").toFloat(),
                    ativo = obj.getBoolean("ativo"),
                    modo = obj.optString("modo", LocationAlarme.MODO_TODOS_OS_DIAS),
                    horaInicio = obj.optInt("horaInicio", 0),
                    minutoInicio = obj.optInt("minutoInicio", 0),
                    horaFim = obj.optInt("horaFim", 23),
                    minutoFim = obj.optInt("minutoFim", 59)
                )
            )
        }
        return lista
    }

    fun salvar(context: Context, alarme: LocationAlarme) {
        val lista = listar(context).toMutableList()
        val index = lista.indexOfFirst { it.id == alarme.id }
        if (index >= 0) lista[index] = alarme else lista.add(alarme)
        persistir(context, lista)
    }

    fun remover(context: Context, id: Long) {
        persistir(context, listar(context).filter { it.id != id })
    }

    fun buscar(context: Context, id: Long): LocationAlarme? {
        return listar(context).find { it.id == id }
    }

    private fun persistir(context: Context, lista: List<LocationAlarme>) {
        val array = JSONArray()
        lista.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("nome", it.nome)
            obj.put("latitude", it.latitude)
            obj.put("longitude", it.longitude)
            obj.put("raioMetros", it.raioMetros)
            obj.put("ativo", it.ativo)
            obj.put("modo", it.modo)
            obj.put("horaInicio", it.horaInicio)
            obj.put("minutoInicio", it.minutoInicio)
            obj.put("horaFim", it.horaFim)
            obj.put("minutoFim", it.minutoFim)
            array.put(obj)
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(CHAVE, array.toString()).apply()
    }
}
