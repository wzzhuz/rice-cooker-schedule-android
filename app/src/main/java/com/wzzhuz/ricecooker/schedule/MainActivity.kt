package com.wzzhuz.ricecooker.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.wzzhuz.ricecooker.schedule.ui.ScheduleScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = ScheduleRepository(applicationContext)
        val viewModel = ViewModelProvider(this, Factory(repository))[ScheduleViewModel::class.java]
        setContent {
            ScheduleScreen(viewModel)
        }
    }

    private class Factory(private val repository: ScheduleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ScheduleViewModel(repository) as T
    }
}
