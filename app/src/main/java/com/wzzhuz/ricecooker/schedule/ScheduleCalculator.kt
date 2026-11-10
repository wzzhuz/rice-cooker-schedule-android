package com.wzzhuz.ricecooker.schedule

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * 电饭煲预约助手 · 计算核心（纯 Kotlin，不依赖 Android，可直接 JVM 单元测试）。
 *
 * 客服已确认：
 *  - 预约 =「几小时后煮好」，机器自行倒推开始煮的时刻，所以 **T 不减烹饪时长**。
 *  - 步进默认 10 分钟，最大预约 24 小时，仅倒计时。
 *  - 向下取整：实际完成时刻永远不晚于预期完成时刻，宁可提前进保温。
 */
object ScheduleCalculator {

    const val DEFAULT_STEP = 10
    const val MIN_STEP = 10
    const val MAX_STEP = 240
    val PRESETS: List<Int> = listOf(10, 15, 30, 60)
    val DEFAULT_TARGETS: List<String> = listOf("07:30", "08:00", "09:30")

    private val TARGET_PATTERN = Regex("^\\d{2}:\\d{2}$")

    enum class DayTag { TODAY, TOMORROW }

    data class Row(
        val target: String,
        val minutes: Int,
        val expected: LocalDateTime,
        val actual: LocalDateTime,
        val dayTag: DayTag,
    )

    fun isValidTarget(value: String): Boolean {
        if (!TARGET_PATTERN.matches(value)) return false
        return try {
            parse(value); true
        } catch (_: Exception) {
            false
        }
    }

    fun isValidStep(step: Int): Boolean = step in MIN_STEP..MAX_STEP

    /** 24 小时内机器可接受的最大设置值；自定义步进不整除 1440 时也不会超上限。 */
    fun maxSet(step: Int): Int = (1440 / step) * step

    private fun parse(hm: String): LocalTime {
        val parts = hm.split(":")
        return LocalTime.of(parts[0].toInt(), parts[1].toInt())
    }

    /** 该时刻的下一次出现：今天剩余不足一个步进（含已到/已过）就顺延到明天。 */
    fun nextOccurrence(now: LocalDateTime, target: String, step: Int): LocalDateTime {
        val time = parse(target)
        var candidate = LocalDateTime.of(now.toLocalDate(), time)
        if (ChronoUnit.MINUTES.between(now, candidate) < step.toLong()) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    /**
     * N       = 当前时刻向下取整到分钟（整分钟内数值稳定，每分钟跳一次）
     * D       = 该行时刻下一次出现
     * maxSet  = floor(1440 / step) × step
     * T       = min( floor((D − N) / step) × step, maxSet )
     * 实际完成 = N + T
     */
    fun compute(target: String, now: LocalDateTime, step: Int): Row {
        require(isValidStep(step)) { "步进必须是 $MIN_STEP–$MAX_STEP 的整数分钟" }
        val n = now.truncatedTo(ChronoUnit.MINUTES)
        val d = nextOccurrence(n, target, step)
        val diff = ChronoUnit.MINUTES.between(n, d).toInt()
        val minutes = minOf((diff / step) * step, maxSet(step))
        val actual = n.plusMinutes(minutes.toLong())
        val tag = if (d.toLocalDate() == n.toLocalDate()) DayTag.TODAY else DayTag.TOMORROW
        return Row(target, minutes, d, actual, tag)
    }

    fun rows(targets: List<String>, now: LocalDateTime, step: Int): List<Row> =
        targets.distinct().sorted().map { compute(it, now, step) }

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "$h 小时 $m 分"
            h > 0 -> "$h 小时"
            else -> "$m 分钟"
        }
    }

    fun formatClock(time: LocalDateTime): String =
        "%02d:%02d".format(time.hour, time.minute)

    fun formatDate(date: LocalDate): String {
        // DayOfWeek: MONDAY=1 … SUNDAY=7；取模后 0=日、1=一 … 6=六
        val week = "日一二三四五六"[date.dayOfWeek.value % 7]
        return "${date.monthValue}月${date.dayOfMonth}日 周$week"
    }

    fun isNight(now: LocalDateTime): Boolean = now.hour >= 20 || now.hour < 7
}
