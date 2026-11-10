package com.wzzhuz.ricecooker.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime

class ScheduleViewModel(private val repository: ScheduleRepository) : ViewModel() {

    private val _settings = MutableStateFlow(repository.load())
    val settings: StateFlow<ScheduleRepository.Settings> = _settings

    /** 每秒发一次当前时刻；UI 只取到分钟，整分钟内数值不跳动。 */
    val now: StateFlow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1000L)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LocalDateTime.now(),
    )

    fun addTarget(value: String) {
        if (!ScheduleCalculator.isValidTarget(value)) return
        update { current ->
            current.copy(targets = (current.targets + value).distinct().sorted())
        }
    }

    fun removeTarget(value: String) {
        update { current -> current.copy(targets = current.targets - value) }
    }

    fun setStep(step: Int) {
        if (!ScheduleCalculator.isValidStep(step)) return
        update { current -> current.copy(stepMinutes = step) }
    }

    private inline fun update(block: (ScheduleRepository.Settings) -> ScheduleRepository.Settings) {
        val next = block(_settings.value)
        _settings.value = next
        repository.save(next)
    }
}
