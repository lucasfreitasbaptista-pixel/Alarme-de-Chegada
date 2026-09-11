package com.alarmedechegada.app

import java.util.Calendar

data class LocationAlarme(
    val id: Long,
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val raioMetros: Float,
    val ativo: Boolean,
    val modo: String = MODO_TODOS_OS_DIAS,
    val horaInicio: Int = 0,
    val minutoInicio: Int = 0,
    val horaFim: Int = 23,
    val minutoFim: Int = 59
) {
    companion object {
        const val MODO_TODOS_OS_DIAS = "TODOS_OS_DIAS"
        const val MODO_DIAS_DE_SEMANA = "DIAS_DE_SEMANA"
        const val MODO_SO_HOJE = "SO_HOJE"
    }

    fun modoLabel(): String = when (modo) {
        MODO_TODOS_OS_DIAS -> "Todos os dias"
        MODO_DIAS_DE_SEMANA -> "Dias de semana"
        MODO_SO_HOJE -> "Só hoje"
        else -> ""
    }

    fun horarioFormatado(): String {
        val inicioEhDiaTodo = horaInicio == 0 && minutoInicio == 0
        val fimEhDiaTodo = horaFim == 23 && minutoFim == 59
        if (inicioEhDiaTodo && fimEhDiaTodo) return "Qualquer horário"
        return "%02d:%02d - %02d:%02d".format(horaInicio, minutoInicio, horaFim, minutoFim)
    }

    /**
     * Verifica se, no momento atual, esse alarme de local deveria estar
     * "armado" — ou seja, se o dia da semana e o horário batem com o que
     * foi configurado. O geofence em si fica sempre registrado no sistema;
     * essa checagem só decide se, ao entrar na área, o alarme deve tocar
     * de verdade ou ser ignorado silenciosamente.
     */
    fun permiteAgora(agora: Calendar = Calendar.getInstance()): Boolean {
        if (modo == MODO_DIAS_DE_SEMANA) {
            val diaDaSemana = agora.get(Calendar.DAY_OF_WEEK)
            if (diaDaSemana == Calendar.SATURDAY || diaDaSemana == Calendar.SUNDAY) {
                return false
            }
        }

        val minutosAgora = agora.get(Calendar.HOUR_OF_DAY) * 60 + agora.get(Calendar.MINUTE)
        val minutosInicio = horaInicio * 60 + minutoInicio
        val minutosFim = horaFim * 60 + minutoFim

        return if (minutosInicio <= minutosFim) {
            minutosAgora in minutosInicio..minutosFim
        } else {
            // Janela que cruza a meia-noite (ex: 22:00 às 02:00)
            minutosAgora >= minutosInicio || minutosAgora <= minutosFim
        }
    }
}
