package com.mmbarno.recordingview

interface RecorderEventListener {
    fun onRecorderStarted()

    fun onStartRecordingRequest()

    fun onRecorderCanceled()

    fun onRecordLessThanMinTime()

    fun onRecordFinished(recordTime: Long)

    fun onRecorderAnimationEnded()

    fun onSwissViewClicked()
}