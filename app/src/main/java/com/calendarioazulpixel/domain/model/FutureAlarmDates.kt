package com.calendarioazulpixel.domain.model

data class FutureAlarmDates(
    val isApplicable: Boolean,
    val generalAlarms: List<String>,
    val diaAlarms: List<String>,
    val nocheAlarms: List<String>
)