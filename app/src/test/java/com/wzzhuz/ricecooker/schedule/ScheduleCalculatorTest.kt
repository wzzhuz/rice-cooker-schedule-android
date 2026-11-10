package com.wzzhuz.ricecooker.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ScheduleCalculatorTest {

    private fun at(vararg p: Int): LocalDateTime = when (p.size) {
        3 -> LocalDateTime.of(p[0], p[1], p[2], 0, 0)
        4 -> LocalDateTime.of(p[0], p[1], p[2], p[3], 0)
        5 -> LocalDateTime.of(p[0], p[1], p[2], p[3], p[4])
        else -> LocalDateTime.of(p[0], p[1], p[2], p[3], p[4], p[5])
    }

    private val targets = ScheduleCalculator.DEFAULT_TARGETS

    // ---- 场景 A：睡前正常设置 ----

    @Test
    fun `周一 21-37 三行预约值`() {
        val rows = ScheduleCalculator.rows(targets, at(2026, 11, 9, 21, 37), 10)
        assertEquals(listOf(590, 620, 710), rows.map { it.minutes })
        assertEquals(
            listOf("9 小时 50 分", "10 小时 20 分", "11 小时 50 分"),
            rows.map { ScheduleCalculator.formatDuration(it.minutes) },
        )
        assertEquals(
            List(3) { ScheduleCalculator.DayTag.TOMORROW },
            rows.map { it.dayTag },
        )
    }

    @Test
    fun `实际完成时刻按 N 加 T 计算`() {
        val rows = ScheduleCalculator.rows(targets, at(2026, 11, 9, 21, 37), 10)
        assertEquals(
            listOf("07:27", "07:57", "09:27"),
            rows.map { ScheduleCalculator.formatClock(it.actual) },
        )
    }

    @Test
    fun `预约值不减烹饪时长`() {
        // 客服确认「几小时后煮好」，机器自行倒推；若误减 30 分会得到 590
        assertEquals(
            620,
            ScheduleCalculator.compute("08:00", at(2026, 11, 9, 21, 37), 10).minutes,
        )
    }

    @Test
    fun `秒级输入先截断到分钟整分钟内不跳动`() {
        val a = ScheduleCalculator.compute("08:00", at(2026, 11, 9, 21, 37, 0), 10)
        val b = ScheduleCalculator.compute("08:00", at(2026, 11, 9, 21, 37, 45), 10)
        assertEquals(a.minutes, b.minutes)
        assertEquals(a.actual, b.actual)
    }

    // ---- 场景 B：部分行已过，自动顺延 ----

    @Test
    fun `周二 08-05 前两行顺延到明天 09-30 仍标今天`() {
        val rows = ScheduleCalculator.rows(targets, at(2026, 11, 10, 8, 5), 10)
        assertEquals(listOf(1400, 1430, 80), rows.map { it.minutes })
        assertEquals(
            listOf(
                ScheduleCalculator.DayTag.TOMORROW,
                ScheduleCalculator.DayTag.TOMORROW,
                ScheduleCalculator.DayTag.TODAY,
            ),
            rows.map { it.dayTag },
        )
        assertEquals(
            listOf("07:25", "07:55", "09:25"),
            rows.map { ScheduleCalculator.formatClock(it.actual) },
        )
    }

    @Test
    fun `目标时刻已到顺延到明天且正好 24 小时`() {
        val rows = ScheduleCalculator.rows(targets, at(2026, 11, 11, 7, 30), 10)
        assertEquals(1440, rows[0].minutes)
        assertEquals("24 小时", ScheduleCalculator.formatDuration(rows[0].minutes))
        assertEquals("07:30", ScheduleCalculator.formatClock(rows[0].actual))
        assertEquals(30, rows[1].minutes)
        assertEquals(120, rows[2].minutes)
    }

    // ---- 场景 C：自定义步进与 24 小时兜底 ----

    @Test
    fun `自定义 25 分在 21-37 的结果`() {
        val rows = ScheduleCalculator.rows(targets, at(2026, 11, 9, 21, 37), 25)
        assertEquals(listOf(575, 600, 700), rows.map { it.minutes })
        assertEquals("10 小时", ScheduleCalculator.formatDuration(rows[1].minutes))
        assertEquals("07:37", ScheduleCalculator.formatClock(rows[1].actual))
    }

    @Test
    fun `自定义 25 分顺延后不超过 24 小时`() {
        val row = ScheduleCalculator.compute("07:30", at(2026, 11, 11, 7, 29), 25)
        assertEquals(1425, ScheduleCalculator.maxSet(25))
        assertEquals(1425, row.minutes)
        assertEquals("23 小时 45 分", ScheduleCalculator.formatDuration(row.minutes))
        assertEquals("07:14", ScheduleCalculator.formatClock(row.actual))
        assertTrue(row.minutes <= 1440)
    }

    @Test
    fun `maxSet 对整除与不整除的步进都正确`() {
        assertEquals(1440, ScheduleCalculator.maxSet(10))
        assertEquals(1440, ScheduleCalculator.maxSet(15))
        assertEquals(1440, ScheduleCalculator.maxSet(30))
        assertEquals(1425, ScheduleCalculator.maxSet(25))
    }

    // ---- 不变量：向下取整、绝不迟到、24 小时上限 ----

    @Test
    fun `任意时刻与任意合法步进下实际完成都不晚于预期完成`() {
        val steps = listOf(10, 15, 20, 25, 30, 60, 240)
        val hours = listOf(0, 6, 7, 8, 12, 19, 20, 21, 23)
        for (step in steps) {
            for (h in hours) {
                for (m in listOf(0, 7, 29, 37, 59)) {
                    val now = at(2026, 11, 10, h, m)
                    for (t in targets + listOf("06:45", "23:55")) {
                        val row = ScheduleCalculator.compute(t, now, step)
                        assertFalse(
                            "迟到: $t @ $now step=$step",
                            row.actual.isAfter(row.expected),
                        )
                        assertTrue("超 24 小时: ${row.minutes}", row.minutes <= 1440)
                        assertTrue("步进不对齐: ${row.minutes}", row.minutes % step == 0)
                        assertTrue("不可按: ${row.minutes}", row.minutes >= step)
                    }
                }
            }
        }
    }

    @Test
    fun `列表按时刻升序且去重`() {
        val rows = ScheduleCalculator.rows(
            listOf("09:30", "07:30", "08:00", "08:00"),
            at(2026, 11, 9, 21, 37),
            10,
        )
        assertEquals(listOf("07:30", "08:00", "09:30"), rows.map { it.target })
    }

    @Test
    fun `添加 06-45 后排在首行`() {
        val rows = ScheduleCalculator.rows(
            targets + "06:45",
            at(2026, 11, 9, 21, 37),
            10,
        )
        assertEquals("06:45", rows.first().target)
        // 21:37 → 次日 06:45 为 548 分钟，按 10 分步进向下取整为 540（9 小时）
        assertEquals("9 小时", ScheduleCalculator.formatDuration(rows.first().minutes))
    }

    // ---- 步进校验与文案 ----

    @Test
    fun `步进合法范围 10 到 240`() {
        assertFalse(ScheduleCalculator.isValidStep(9))
        assertTrue(ScheduleCalculator.isValidStep(10))
        assertTrue(ScheduleCalculator.isValidStep(20))
        assertTrue(ScheduleCalculator.isValidStep(240))
        assertFalse(ScheduleCalculator.isValidStep(241))
    }

    @Test
    fun `步进文字随取值切换`() {
        assertEquals(
            "按 10 分钟步进向下取整，最多早 9 分钟煮好并自动保温，绝不迟到。",
            "按 10 分钟步进向下取整，最多早 ${10 - 1} 分钟煮好并自动保温，绝不迟到。",
        )
        assertEquals(1440 % 10, 0)
        assertEquals(1440 % 25, 15)
    }

    @Test
    fun `时长文案边界`() {
        assertEquals("24 小时", ScheduleCalculator.formatDuration(1440))
        assertEquals("30 分钟", ScheduleCalculator.formatDuration(30))
        assertEquals("1 小时 25 分", ScheduleCalculator.formatDuration(85))
        assertEquals("10 小时", ScheduleCalculator.formatDuration(600))
    }

    @Test
    fun `日期与周几文案`() {
        assertEquals("11月9日 周一", ScheduleCalculator.formatDate(at(2026, 11, 9, 21, 37).toLocalDate()))
        assertEquals("11月15日 周日", ScheduleCalculator.formatDate(at(2026, 11, 15, 8, 5).toLocalDate()))
    }

    @Test
    fun `夜间深浅色在 20 点到 7 点切换`() {
        assertTrue(ScheduleCalculator.isNight(at(2026, 11, 9, 21, 37)))
        assertTrue(ScheduleCalculator.isNight(at(2026, 11, 9, 6, 59)))
        assertFalse(ScheduleCalculator.isNight(at(2026, 11, 9, 7, 0)))
        assertFalse(ScheduleCalculator.isNight(at(2026, 11, 10, 8, 5)))
    }

    @Test
    fun `目标时刻格式校验`() {
        assertTrue(ScheduleCalculator.isValidTarget("07:30"))
        assertTrue(ScheduleCalculator.isValidTarget("23:59"))
        assertFalse(ScheduleCalculator.isValidTarget("7:30"))
        assertFalse(ScheduleCalculator.isValidTarget("25:00"))
        assertFalse(ScheduleCalculator.isValidTarget("07:60"))
    }
}
