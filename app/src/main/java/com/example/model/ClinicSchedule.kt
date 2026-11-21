package com.example.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class ClinicSlot(
  val index: Int,
  val label: String,
  val timeOfDay: TimeOfDay,
)

object ClinicSchedule {
  val ALL_SLOTS: List<ClinicSlot> = listOf(
    ClinicSlot(0, "9:00 AM", TimeOfDay.MORNING),
    ClinicSlot(1, "9:30 AM", TimeOfDay.MORNING),
    ClinicSlot(2, "10:00 AM", TimeOfDay.MORNING),
    ClinicSlot(3, "10:30 AM", TimeOfDay.MORNING),
    ClinicSlot(4, "11:00 AM", TimeOfDay.MORNING),
    ClinicSlot(5, "11:30 AM", TimeOfDay.MORNING),
    ClinicSlot(6, "12:00 PM", TimeOfDay.AFTERNOON),
    ClinicSlot(7, "12:30 PM", TimeOfDay.AFTERNOON),
    ClinicSlot(8, "1:00 PM", TimeOfDay.AFTERNOON),
    ClinicSlot(9, "1:30 PM", TimeOfDay.AFTERNOON),
    ClinicSlot(10, "2:00 PM", TimeOfDay.AFTERNOON),
    ClinicSlot(11, "2:30 PM", TimeOfDay.AFTERNOON),
    ClinicSlot(12, "3:00 PM", TimeOfDay.AFTERNOON),
    ClinicSlot(13, "3:30 PM", TimeOfDay.AFTERNOON),
    ClinicSlot(14, "4:00 PM", TimeOfDay.EVENING),
    ClinicSlot(15, "4:30 PM", TimeOfDay.EVENING),
  )

  val DECLINE_REASONS: List<String> = listOf(
    "Doctor not available",
    "Time already booked",
    "Please call the clinic",
  )

  private val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")

  fun slotLabel(slotIndex: Int): String =
    ALL_SLOTS.getOrNull(slotIndex)?.label ?: "9:00 AM"

  fun slotsForTimeOfDay(timeOfDay: TimeOfDay): List<ClinicSlot> =
    ALL_SLOTS.filter { it.timeOfDay == timeOfDay }

  fun todayLocalDateKey(): String {
    val localCal = Calendar.getInstance()
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(localCal.time)
  }

  fun todayUtcMidnightMillis(): Long {
    val localCal = Calendar.getInstance()
    val utcCal = Calendar.getInstance(utcTimeZone).apply {
      clear()
      set(
        localCal.get(Calendar.YEAR),
        localCal.get(Calendar.MONTH),
        localCal.get(Calendar.DAY_OF_MONTH),
      )
    }
    return utcCal.timeInMillis
  }

  fun dateKeyFromUtcMillis(utcMillis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
      timeZone = utcTimeZone
    }
    val cal = Calendar.getInstance(utcTimeZone).apply { timeInMillis = utcMillis }
    return fmt.format(cal.time)
  }

  fun utcMillisFromDateKey(dateKey: String): Long {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
      timeZone = utcTimeZone
    }
    return fmt.parse(dateKey)?.time ?: todayUtcMidnightMillis()
  }

  fun isSundayDateKey(dateKey: String): Boolean {
    val cal = Calendar.getInstance(utcTimeZone).apply {
      timeInMillis = utcMillisFromDateKey(dateKey)
    }
    return cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
  }

  fun isPastDateKey(dateKey: String): Boolean =
    dateKey < todayLocalDateKey()

  fun isSelectableClinicUtcDate(utcMillis: Long): Boolean {
    val todayKey = todayLocalDateKey()
    val candidateKey = dateKeyFromUtcMillis(utcMillis)
    if (candidateKey < todayKey) return false
    val cal = Calendar.getInstance(utcTimeZone).apply { timeInMillis = utcMillis }
    return cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY
  }

  fun formatDateLabel(dateKey: String): String {
    val todayKey = todayLocalDateKey()
    val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val tomorrowKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(tomorrowCal.time)

    return when (dateKey) {
      todayKey -> "Today"
      tomorrowKey -> "Tomorrow"
      else -> {
        val fmt = SimpleDateFormat("EEE, MMM d", Locale.US).apply { timeZone = utcTimeZone }
        val cal = Calendar.getInstance(utcTimeZone).apply {
          timeInMillis = utcMillisFromDateKey(dateKey)
        }
        fmt.format(cal.time)
      }
    }
  }

  fun formatDateAndSlot(dateKey: String, slotIndex: Int): String =
    "${formatDateLabel(dateKey)} · ${slotLabel(slotIndex)}"

  /**
   * Returns upcoming open (Mon-Sat) dateKeys within [daysWindow] days starting from [startOffsetDays].
   */
  fun openDateKeysWithin(startOffsetDays: Int = 0, daysWindow: Int = 7): List<String> {
    val result = mutableListOf<String>()
    val localToday = Calendar.getInstance()
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    for (offset in startOffsetDays..daysWindow) {
      val c = (localToday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
      if (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
        result += fmt.format(c.time)
      }
    }
    return result
  }
}
