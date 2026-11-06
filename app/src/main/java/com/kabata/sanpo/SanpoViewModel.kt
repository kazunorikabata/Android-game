package com.kabata.sanpo

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SanpoViewModel(app: Application) : AndroidViewModel(app), SensorEventListener {
    private val store = StepStore(app)
    private val sensorManager = app.getSystemService(SensorManager::class.java)
    private val stepSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var listening = false

    val hasStepSensor: Boolean = stepSensor != null

    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<GameState> = _state.asStateFlow()

    fun startListening() {
        _state.value = store.load()
        val sensor = stepSensor ?: return
        if (listening) return
        listening = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopListening() {
        if (!listening) return
        sensorManager.unregisterListener(this)
        listening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            _state.value = store.onSensorValue(event.values[0].toLong())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    /** デバッグ用：エミュレータなどで歩かずに試す */
    fun addDebugSteps(steps: Long) {
        _state.value = store.addSteps(steps)
    }

    fun acknowledgeArrival() {
        _state.value = store.markSpotSeen(_state.value.spotIndex)
    }

    override fun onCleared() {
        stopListening()
    }
}
