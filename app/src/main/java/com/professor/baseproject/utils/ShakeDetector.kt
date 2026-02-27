package com.professor.baseproject.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastUpdateTime = 0L
    private var lastShakeTime = 0L
    private val shakeCooldown = 400L // 1 second
    private val shakeThreshold = 600
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    fun start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val curTime = System.currentTimeMillis()

        // Only allow update every 100 ms
        if ((curTime - lastUpdateTime) > 100) {
            val diffTime = curTime - lastUpdateTime
            lastUpdateTime = curTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val delta = x + y + z - lastX - lastY - lastZ
            val speed = abs(delta / diffTime * 10000)

            if (speed > shakeThreshold && (curTime - lastShakeTime) > shakeCooldown) {
                lastShakeTime = curTime
                onShake()
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
