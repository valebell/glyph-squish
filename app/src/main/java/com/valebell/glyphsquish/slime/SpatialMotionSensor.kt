package com.valebell.glyphsquish.slime

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface

class SpatialMotionSensor(
    context: Context,
    private val viewedFromBack: Boolean,
    private val followsDisplayRotation: Boolean,
    private val onMotionChanged: (
        tiltX: Float,
        tiltY: Float,
        tiltZ: Float,
        accelerationX: Float,
        accelerationY: Float,
        accelerationZ: Float,
    ) -> Unit,
    private val onShake: (() -> Unit)? = null,
) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val accelerometerSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val linearAccelerationSensor =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val activeSensors =
        listOfNotNull(
            gravitySensor ?: accelerometerSensor,
            linearAccelerationSensor ?: accelerometerSensor,
        ).distinctBy { sensor -> sensor.type }

    private var gravityX = 0f
    private var gravityY = 0f
    private var gravityZ = 0f
    private var accelerationX = 0f
    private var accelerationY = 0f
    private var accelerationZ = 0f
    private var gravityInitialized = false
    private var started = false
    private val shakeDetector = ShakeDetector()

    fun start(): Boolean {
        if (started) return activeSensors.isNotEmpty()
        if (activeSensors.isEmpty()) return false

        var registeredAny = false
        activeSensors.forEach { sensor ->
            registeredAny = sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME,
            ) ||
                registeredAny
        }
        started = registeredAny
        if (!started) {
            sensorManager.unregisterListener(this)
        }
        return started
    }

    fun stop() {
        shakeDetector.reset()
        if (!started) return
        sensorManager.unregisterListener(this)
        started = false
        gravityX = 0f
        gravityY = 0f
        gravityZ = 0f
        accelerationX = 0f
        accelerationY = 0f
        accelerationZ = 0f
        gravityInitialized = false
        onMotionChanged(0f, 0f, 0f, 0f, 0f, 0f)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!started || event.values.size < 3 || (0..2).any { !event.values[it].isFinite() }) return
        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                updateGravity(
                    event.values,
                    GRAVITY_FILTER_ALPHA,
                )
            }

            Sensor.TYPE_LINEAR_ACCELERATION -> {
                updateAcceleration(event.values, event.timestamp)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                updateFromAccelerometer(event.values, event.timestamp)
            }
        }
        emitMotion()
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit

    private fun updateFromAccelerometer(
        values: FloatArray,
        timestampNanos: Long,
    ) {
        if (gravitySensor == null) {
            updateGravity(values, ACCELEROMETER_GRAVITY_FILTER_ALPHA)
        }
        if (linearAccelerationSensor == null && gravityInitialized) {
            updateAcceleration(
                x = values[0] - gravityX,
                y = values[1] - gravityY,
                z = values[2] - gravityZ,
                timestampNanos = timestampNanos,
            )
        }
    }

    private fun updateGravity(
        values: FloatArray,
        alpha: Float,
    ) {
        if (!gravityInitialized) {
            gravityX = values[0]
            gravityY = values[1]
            gravityZ = values[2]
            gravityInitialized = true
            return
        }
        gravityX += alpha * (values[0] - gravityX)
        gravityY += alpha * (values[1] - gravityY)
        gravityZ += alpha * (values[2] - gravityZ)
    }

    private fun updateAcceleration(
        values: FloatArray,
        timestampNanos: Long,
    ) {
        updateAcceleration(values[0], values[1], values[2], timestampNanos)
    }

    private fun updateAcceleration(
        x: Float,
        y: Float,
        z: Float,
        timestampNanos: Long,
    ) {
        if (shakeDetector.addSample(x, y, z, timestampNanos)) {
            onShake?.invoke()
        }
        accelerationX += MOTION_FILTER_ALPHA * (x - accelerationX)
        accelerationY += MOTION_FILTER_ALPHA * (y - accelerationY)
        accelerationZ += MOTION_FILTER_ALPHA * (z - accelerationZ)
    }

    private fun emitMotion() {
        val horizontalDirection = if (viewedFromBack) 1f else -1f
        val depthDirection = if (viewedFromBack) -1f else 1f
        val displayRotation =
            if (followsDisplayRotation) {
                displayManager
                    .getDisplay(Display.DEFAULT_DISPLAY)
                    ?.rotation ?: Surface.ROTATION_0
            } else {
                Surface.ROTATION_0
            }
        val gravity = remapMotionAxes(gravityX, gravityY, gravityZ, displayRotation)
        val acceleration =
            remapMotionAxes(
                accelerationX,
                accelerationY,
                accelerationZ,
                displayRotation,
            )
        onMotionChanged(
            normalized(gravity.x * horizontalDirection),
            normalized(gravity.y),
            normalized(gravity.z * depthDirection),
            normalized(acceleration.x * horizontalDirection),
            normalized(acceleration.y),
            normalized(acceleration.z * depthDirection),
        )
    }

    private fun normalized(value: Float): Float = (value / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)

    private companion object {
        const val ACCELEROMETER_GRAVITY_FILTER_ALPHA = 0.08f
        const val GRAVITY_FILTER_ALPHA = 0.34f
        const val MOTION_FILTER_ALPHA = 0.46f
    }
}

internal data class MotionAxes(
    val x: Float,
    val y: Float,
    val z: Float,
)

internal fun remapMotionAxes(
    x: Float,
    y: Float,
    z: Float,
    displayRotation: Int,
): MotionAxes =
    when (displayRotation) {
        Surface.ROTATION_90 -> MotionAxes(x = -y, y = x, z = z)
        Surface.ROTATION_180 -> MotionAxes(x = -x, y = -y, z = z)
        Surface.ROTATION_270 -> MotionAxes(x = y, y = -x, z = z)
        else -> MotionAxes(x = x, y = y, z = z)
    }
