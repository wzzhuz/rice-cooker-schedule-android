package com.wzzhuz.ricecooker.schedule

import android.content.Context

/**
 * 全部数据只存本机 SharedPreferences：目标时刻列表 + 步进值。
 * 改动即时落盘，没有保存按钮、没有确认弹窗。
 */
class ScheduleRepository(context: Context) {

    data class Settings(
        val targets: List<String>,
        val stepMinutes: Int,
    )

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): Settings {
        val step = prefs.getInt(KEY_STEP, ScheduleCalculator.DEFAULT_STEP)
            .takeIf { ScheduleCalculator.isValidStep(it) }
            ?: ScheduleCalculator.DEFAULT_STEP

        val raw = prefs.getString(KEY_TARGETS, null)
        val targets = if (raw == null) {
            ScheduleCalculator.DEFAULT_TARGETS
        } else {
            raw.split(",")
                .map { it.trim() }
                .filter { ScheduleCalculator.isValidTarget(it) }
                .distinct()
                .sorted()
        }
        return Settings(targets, step)
    }

    fun save(settings: Settings) {
        val targets = settings.targets
            .filter { ScheduleCalculator.isValidTarget(it) }
            .distinct()
            .sorted()
        val step = settings.stepMinutes.takeIf { ScheduleCalculator.isValidStep(it) }
            ?: ScheduleCalculator.DEFAULT_STEP
        prefs.edit()
            .putString(KEY_TARGETS, targets.joinToString(","))
            .putInt(KEY_STEP, step)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "rice_cooker_android_v1"
        const val KEY_TARGETS = "targets"
        const val KEY_STEP = "stepMinutes"
    }
}
