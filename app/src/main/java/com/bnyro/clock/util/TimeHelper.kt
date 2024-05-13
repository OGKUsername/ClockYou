package com.bnyro.clock.util

import android.annotation.SuppressLint
import android.content.Context
import com.bnyro.clock.R
import com.bnyro.clock.domain.model.TimeObject
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs
import kotlin.time.Duration

object TimeHelper {
    val currentTime: Date get() = Calendar.getInstance().time
    private const val MILLIS_PER_MINUTE: Int = 60_000
    private const val MINUTES_PER_HOUR: Int = 60

    fun getCurrentWeekDay(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    }

    fun formatDateTime(time: ZonedDateTime): Pair<String, String> {
        val showSeconds = Preferences.instance.getBoolean(Preferences.showSecondsKey, true)
        return formatDateTime(time, showSeconds)
    }

    fun formatDateTime(time: ZonedDateTime, showSeconds: Boolean): Pair<String, String> {
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)

        val timeFormatter = if (showSeconds) {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        } else {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        }
        return dateFormatter.format(time) to timeFormatter.format(time)
    }

    fun formatTime(time: ZonedDateTime): String {
        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        return timeFormatter.format(time)
    }

    @SuppressLint("DefaultLocale")
    fun formatGMTTimeDifference(timeDiff: Float): String {
        val prefix = if (timeDiff >= 0) "+" else "-"
        val hours = abs(timeDiff.toInt())
        val minutes = (timeDiff * 60f % 60).toInt()

        return if (minutes == 0) {
            "GMT $prefix$hours"
        } else {
            val formattedMinutes = String.format("%02d", minutes)
            "GMT $prefix$hours:$formattedMinutes"
        }

    }

    /**
     * Converts milliseconds to a formatted time string.
     *
     * @param millis The milliseconds since midnight.
     * @return The formatted time string.
     */
    fun millisToFormatted(millis: Long): String {
        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val localTime = LocalTime.of(
            millis.div(1000 * 60 * 60).toInt(),
            millis.div(1000 * 60).mod(60),
            millis.div(1000).mod(60)
        )
        return timeFormatter.format(localTime)
    }

    fun millisToTime(millis: Long): TimeObject {
        val hours = millis.div(1000 * 60 * 60).toInt()
        val minutes = millis.div(1000 * 60).mod(60)
        val seconds = millis.div(1000).mod(60)
        val milliseconds = millis.mod(1000)
        return TimeObject(hours, minutes, seconds, milliseconds)
    }

    fun getTimeByZone(timeZone: String? = null): ZonedDateTime {
        val zone = timeZone?.let { ZoneId.of(timeZone) } ?: ZoneId.systemDefault()
        val now = Instant.now()
        return now.atZone(zone)
    }

    fun formatHourDifference(
        context: Context,
        timeZone: com.bnyro.clock.domain.model.TimeZone
    ): String {
        val millisOffset = (timeZone.offset - TimeZone.getDefault().getOffset(currentTime.time))
        val minutesOffset = millisOffset / MILLIS_PER_MINUTE
        val hours = minutesOffset.div(MINUTES_PER_HOUR)
        val minutes = abs(minutesOffset.mod(MINUTES_PER_HOUR))
        return when {
            hours == 0 -> {
                context.getString(R.string.same_time)
            }

            minutes > 0 -> {
                if (hours > 0) {
                    context.resources.getQuantityString(
                        R.plurals.hour_minute_offset_positive,
                        hours,
                        hours,
                        minutes
                    )
                } else {
                    context.resources.getQuantityString(
                        R.plurals.hour_minute_offset_negative,
                        abs(hours),
                        abs(hours),
                        minutes
                    )
                }
            }

            else -> {
                if (hours > 0) {
                    context.resources.getQuantityString(
                        R.plurals.hour_offset_positive,
                        hours,
                        hours
                    )
                } else {
                    context.resources.getQuantityString(
                        R.plurals.hour_offset_negative,
                        abs(hours),
                        abs(hours)
                    )
                }
            }
        }
    }

    /**
     * Method that formats a Duration object into a verbose string to be displayed in the UI
     */
    fun durationToFormatted(context: Context, duration: Duration): String =
        duration.toComponents { days, hours, minutes, _, _ ->
            when {
                days == 0L && hours == 0 -> "$minutes ${
                    context.getString(R.string.minutes).lowercase()
                }"

                days == 0L -> "$hours ${
                    context.getString(R.string.hours).lowercase()
                } $minutes ${context.getString(R.string.minutes).lowercase()}"

                else -> "$days ${
                    context.getString(R.string.days).lowercase()
                } $hours ${
                    context.getString(R.string.hours).lowercase()
                } $minutes ${context.getString(R.string.minutes).lowercase()}"
            }
        }
}