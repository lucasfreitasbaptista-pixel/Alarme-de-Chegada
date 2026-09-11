package com.alarmedechegada.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

object AutoStartHelper {

    private val componentesConhecidos = listOf(
        ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
        ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
        ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"),
        ComponentName("com.letv.android.letvsafe", "com.letv.android.letvsafe.AutobootManageActivity"),
    )

    private const val PREF_KEY = "autostart_solicitado"

    fun tentarAbrirUmaVez(context: Context) {
        val prefs = context.getSharedPreferences("alarme_chegada", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREF_KEY, false)) return
        prefs.edit().putBoolean(PREF_KEY, true).apply()

        val pm = context.packageManager
        for (componente in componentesConhecidos) {
            try {
                val intent = Intent().apply {
                    component = componente
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (intent.resolveActivity(pm) != null) {
                    Toast.makeText(
                        context,
                        "Procure \"Alarme de Chegada\" e ative o início automático",
                        Toast.LENGTH_LONG
                    ).show()
                    context.startActivity(intent)
                    return
                }
            } catch (e: Exception) {
            }
        }
    }

    /**
     * Tenta abrir a tela de economia de bateria específica do MIUI.
     * Retorna true se conseguiu abrir essa tela (aparelho é Xiaomi/MIUI),
     * ou false se o aparelho não tem essa tela (não é Xiaomi) — nesse caso,
     * quem chamou essa função deve pedir a permissão padrão do Android no
     * lugar, pra não deixar o usuário sem ser perguntado em nenhum aparelho.
     */
    fun tentarAbrirEconomiaBateriaMiui(context: Context): Boolean {
        val prefs = context.getSharedPreferences("alarme_chegada", Context.MODE_PRIVATE)
        val chave = "economia_bateria_miui_solicitada"
        if (prefs.getBoolean(chave, false)) return false
        prefs.edit().putBoolean(chave, true).apply()

        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.miui.powerkeeper",
                    "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
                )
                putExtra("package_name", context.packageName)
                putExtra("package_label", "Alarme de Chegada")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                Toast.makeText(
                    context,
                    "Selecione \"Sem restrições\" para o Alarme de Chegada",
                    Toast.LENGTH_LONG
                ).show()
                context.startActivity(intent)
                return true
            }
        } catch (e: Exception) {
        }
        return false
    }
}
