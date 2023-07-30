package com.mutualmobile.composesensors

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * The HeartBeat sensor returns an event everytime a heart beat peak is detected. Peak here ideally
 * corresponds to the positive peak in the QRS complex of an ECG signal.
 *
 * For more info, please refer the [Android Documentation Reference](https://developer.android.com/reference/android/hardware/SensorEvent#sensor.type_heart_beat:)
 *
 * @param isConfidentPeak A confidence value of 0.0 (false) indicates complete uncertainty - that a peak
 * is as likely to be at the indicated timestamp as anywhere else. A confidence value of 1.0 (true)
 * indicates complete certainly - that a peak is completely unlikely to be anywhere else on the QRS
 * complex. Defaults to false.
 * @param isAvailable Whether the current device has a heart beat sensor. Defaults to false.
 * @param accuracy Accuracy factor of the heart beat sensor. Defaults to 0.
 */
@RequiresApi(Build.VERSION_CODES.N)
@Immutable
class HeartBeatSensorState internal constructor(
    val isConfidentPeak: Boolean = false,
    val isAvailable: Boolean = false,
    val accuracy: Int = 0,
    private val startListeningEvents: (() -> Unit)? = null,
    private val stopListeningEvents: (() -> Unit)? = null,
) : SensorStateListener {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeartBeatSensorState) return false

        if (isConfidentPeak != other.isConfidentPeak) return false
        if (isAvailable != other.isAvailable) return false
        if (accuracy != other.accuracy) return false
        if (startListeningEvents != other.startListeningEvents) return false
        if (stopListeningEvents != other.stopListeningEvents) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isConfidentPeak.hashCode()
        result = 31 * result + isAvailable.hashCode()
        result = 31 * result + accuracy.hashCode()
        result = 31 * result + startListeningEvents.hashCode()
        result = 31 * result + stopListeningEvents.hashCode()
        return result
    }

    override fun toString(): String {
        return "HeartBeatSensorState(isConfidentPeak=$isConfidentPeak isAvailable=$isAvailable, " +
                "accuracy=$accuracy)"
    }

    override fun startListening() {
        startListeningEvents?.invoke()
    }

    override fun stopListening() {
        stopListeningEvents?.invoke()
    }
}

/**
 * Creates and [remember]s an instance of [HeartBeatSensorState].
 * @param autoStart Start listening to sensor events as soon as sensor state is initialised.
 * Defaults to true.
 * @param sensorDelay The rate at which the raw sensor data should be received.
 * Defaults to [SensorDelay.Normal].
 * @param onError Callback invoked on every error state.
 */
@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun rememberHeartBeatSensorState(
    autoStart: Boolean = true,
    sensorDelay: SensorDelay = SensorDelay.Normal,
    onError: (throwable: Throwable) -> Unit = {},
): HeartBeatSensorState {
    val sensorState = rememberSensorState(
        sensorType = SensorType.HeartBeat,
        sensorDelay = sensorDelay,
        autoStart = autoStart,
        onError = onError
    )
    val confidenceSensorState = remember {
        mutableStateOf(
            HeartBeatSensorState(
                startListeningEvents = sensorState::startListening,
                stopListeningEvents = sensorState::stopListening
            )
        )
    }

    LaunchedEffect(
        key1 = sensorState,
        block = {
            val sensorStateValues = sensorState.data
            if (sensorStateValues.isNotEmpty()) {
                confidenceSensorState.value = HeartBeatSensorState(
                    isConfidentPeak = sensorStateValues[0] == 1f,
                    isAvailable = sensorState.isAvailable,
                    accuracy = sensorState.accuracy,
                    startListeningEvents = sensorState::startListening,
                    stopListeningEvents = sensorState::stopListening
                )
            }
        }
    )

    return confidenceSensorState.value
}
