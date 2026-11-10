@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.wzzhuz.ricecooker.schedule.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wzzhuz.ricecooker.schedule.ScheduleCalculator
import com.wzzhuz.ricecooker.schedule.ScheduleCalculator.DayTag
import com.wzzhuz.ricecooker.schedule.ScheduleViewModel
import java.time.LocalDateTime

private val Tabular = TextStyle(fontFeatureSettings = "tnum")

/** 低对比虚线边框，对应网页原型里「+ 添加时间」的虚线样式。 */
private fun Modifier.dashedBorder(color: Color, radius: Dp = 14.dp): Modifier = drawBehind {
    val r = radius.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(r, r),
        style = Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
        ),
    )
}

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel) {
    val settings by viewModel.settings.collectAsState()
    val now by viewModel.now.collectAsState()
    val night = ScheduleCalculator.isNight(now)
    val p = palette(night)
    val rows = ScheduleCalculator.rows(settings.targets, now, settings.stepMinutes)

    var showAddDialog by remember { mutableStateOf(false) }
    var showStepPanel by remember { mutableStateOf(false) }

    RiceCookerTheme(night) {
        Surface(modifier = Modifier.fillMaxSize(), color = p.bg) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 460.dp)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 26.dp),
            ) {
                TopClock(now, p)
                Spacer(Modifier.height(34.dp))
                Header(p)
                Spacer(Modifier.height(12.dp))

                if (rows.isEmpty()) {
                    EmptyState(p)
                } else {
                    rows.forEach { row ->
                        key(row.target) {
                            ScheduleRow(row, p) { viewModel.removeTarget(row.target) }
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(p.line))
                }

                Spacer(Modifier.height(24.dp))
                AddButton(p) {
                    showStepPanel = false
                    showAddDialog = true
                }

                Spacer(Modifier.height(28.dp))
                Footer(p, settings.stepMinutes) {
                    showAddDialog = false
                    showStepPanel = !showStepPanel
                }

                if (showStepPanel) {
                    StepPanel(
                        step = settings.stepMinutes,
                        palette = p,
                        onSelect = { step ->
                            viewModel.setStep(step)
                            showStepPanel = false
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showAddDialog) {
        AddTimeDialog(
            palette = p,
            onDismiss = { showAddDialog = false },
            onConfirm = { value -> viewModel.addTarget(value) },
        )
    }
}

@Composable
private fun TopClock(now: LocalDateTime, p: AppPalette) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = ScheduleCalculator.formatClock(now),
            fontSize = 25.sp,
            fontWeight = FontWeight.W600,
            color = p.ink,
            style = Tabular,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = ScheduleCalculator.formatDate(now.toLocalDate()),
            fontSize = 12.sp,
            color = p.sub,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun Header(p: AppPalette) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("现在设为", fontSize = 11.5.sp, color = p.sub, letterSpacing = 0.5.sp)
        Text("煮好时间", fontSize = 11.5.sp, color = p.sub, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun EmptyState(p: AppPalette) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .border(1.dp, p.line, RectangleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("还没有时间，点下方添加", fontSize = 13.sp, color = p.sub)
    }
}

@Composable
private fun ScheduleRow(
    row: ScheduleCalculator.Row,
    p: AppPalette,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var dragX by remember { mutableStateOf(0f) }
    val reveal by animateDpAsState(
        targetValue = if (open) 78.dp else 0.dp,
        animationSpec = tween(180),
        label = "reveal",
    )
    val dstAlpha by animateFloatAsState(
        targetValue = if (open) 0f else 1f,
        animationSpec = tween(180),
        label = "dstAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp)
            .clip(RectangleShape)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f },
                    onHorizontalDrag = { _, delta -> dragX += delta },
                    onDragEnd = {
                        if (dragX < -30f) open = true
                        else if (dragX > 30f) open = false
                        dragX = 0f
                    },
                    onDragCancel = { dragX = 0f },
                )
            },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(p.line)
                .align(Alignment.TopCenter),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 左侧：要按进机器的动作值，行内唯一主角
            Text(
                text = ScheduleCalculator.formatDuration(row.minutes),
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = p.brand,
                style = Tabular,
            )

            // 右侧：识别标签 + 实际完成小字，不与左侧等大
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .alpha(dstAlpha),
                horizontalAlignment = Alignment.End,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.target,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = p.ink,
                        style = Tabular,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (row.dayTag == DayTag.TODAY) "今天" else "明天",
                        fontSize = 11.sp,
                        color = p.sub,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "实际 ${ScheduleCalculator.formatClock(row.actual)} 完成",
                    fontSize = 10.5.sp,
                    color = p.sub,
                    style = Tabular,
                )
            }
        }

        // 删除按钮滑入，行内容不位移
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 78.dp - reveal)
                .width(78.dp)
                .fillMaxHeight()
                .background(p.danger)
                .clickable { onDelete() },
            contentAlignment = Alignment.Center,
        ) {
            Text("删除", color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AddButton(p: AppPalette, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .dashedBorder(p.line)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("+ 添加时间", fontSize = 13.5.sp, color = p.sub)
    }
}

@Composable
private fun Footer(p: AppPalette, step: Int, onStepClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "预约 = 几小时后煮好，机器自己算开火时间",
            fontSize = 11.sp,
            color = p.sub,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("改动自动保存 · 左滑删除 · ", fontSize = 11.sp, color = p.sub)
            Text(
                text = "步进 $step 分钟 ›",
                fontSize = 11.sp,
                color = p.sub,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onStepClick),
            )
        }
    }
}

@Composable
private fun StepPanel(step: Int, palette: AppPalette, onSelect: (Int) -> Unit) {
    val custom = !ScheduleCalculator.PRESETS.contains(step)
    var input by remember(step) { mutableStateOf(if (custom) step.toString() else "") }
    var error by remember { mutableStateOf("") }
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(shape)
            .border(1.dp, palette.line, shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (custom) "预约步进 · 当前 $step 分（自定义）" else "预约步进 · 一次设定，长期生效",
            fontSize = 11.sp,
            color = palette.sub,
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScheduleCalculator.PRESETS.forEach { preset ->
                val selected = preset == step
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, if (selected) palette.brand else palette.line, RoundedCornerShape(10.dp))
                        .clickable { onSelect(preset) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$preset 分",
                        fontSize = 13.sp,
                        color = if (selected) palette.brand else palette.ink,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("自定义", fontSize = 12.sp, color = palette.sub)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = input,
                onValueChange = { value ->
                    input = value.filter { it.isDigit() }
                    error = ""
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = if (custom) palette.brand else palette.ink,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.width(84.dp),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (custom) palette.brand else palette.line,
                                RoundedCornerShape(10.dp),
                            )
                            .padding(vertical = 9.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) { inner() }
                },
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, palette.line, RoundedCornerShape(10.dp))
                    .clickable {
                        val value = input.toIntOrNull()
                        if (value == null || !ScheduleCalculator.isValidStep(value)) {
                            error = "请填 10–240 之间的整数分钟"
                        } else {
                            onSelect(value)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("使用", fontSize = 12.5.sp, color = palette.ink)
            }
        }

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(error, fontSize = 11.sp, color = palette.danger)
        }

        Spacer(Modifier.height(10.dp))
        val divides = 1440 % step == 0
        Text(
            text = if (divides) {
                "按 $step 分钟步进向下取整，最多早 ${step - 1} 分钟煮好并自动保温，绝不迟到。"
            } else {
                "按 $step 分钟步进向下取整，只会提前煮好并自动保温，绝不迟到；提前多久看每行小字。"
            },
            fontSize = 11.sp,
            color = palette.sub,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AddTimeDialog(
    palette: AppPalette,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加煮好时间", fontSize = 16.sp, color = palette.ink) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm("%02d:%02d".format(state.hour, state.minute))
                onDismiss()
            }) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        containerColor = palette.bg,
        titleContentColor = palette.ink,
        textContentColor = palette.ink,
    )
}
