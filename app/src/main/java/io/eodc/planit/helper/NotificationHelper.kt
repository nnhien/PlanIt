package io.eodc.planit.helper

import android.app.*
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v7.preference.PreferenceManager
import io.eodc.planit.R
import io.eodc.planit.db.PlannerDatabase
import io.eodc.planit.receiver.NotificationPublishReceiver
import org.joda.time.DateTime
import java.util.*

/**
 * Helper to fire, schedule, and cancel notifications
 *
 * @author 2n
 */
class NotificationHelper
/**
 * Constructs a new NotificationHelper
 *
 * @param base The context to use
 */
(base: Context) : ContextWrapper(base) {

    private var mManager: NotificationManager? = null

    private val manager: NotificationManager
        get() {
            if (mManager == null) mManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return mManager
        }

    init {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val classes = NotificationChannel(CLASSES_CHANNEL_ID,
                    getString(R.string.noti_channel_classes),
                    NotificationManager.IMPORTANCE_DEFAULT)
            classes.setSound(null, null)

            val reminder = NotificationChannel(REMINDER_CHANNEL_ID,
                    getString(R.string.noti_channel_reminder),
                    NotificationManager.IMPORTANCE_DEFAULT)
            reminder.lightColor = Color.BLUE
            reminder.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            manager.createNotificationChannel(classes)
            manager.createNotificationChannel(reminder)
        }
    }

    /**
     * Fires a notification
     */
    fun fireNotification() {
        Thread {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
            val showNotification = sharedPreferences.getBoolean(getString(R.string.pref_show_notif_key), true)
            if (showNotification) {
                val whatDayValue = sharedPreferences.getString(getString(R.string.pref_what_assign_show_key), "")
                var dtToShow = DateTime()
                if (whatDayValue == getString(R.string.pref_what_assign_show_curr_day_value)) {
                    dtToShow = dtToShow.withTimeAtStartOfDay()
                } else if (whatDayValue == getString(R.string.pref_what_assign_show_next_day_value)) {
                    dtToShow = dtToShow.plusDays(1).withTimeAtStartOfDay()
                }

                val dueAssignments = PlannerDatabase.getInstance(this)!!.assignmentDao()
                        .getStaticAssignmentsDueBetweenDates(dtToShow, dtToShow.plusDays(1))
                val subjects = PlannerDatabase.getInstance(this)!!.classDao().allSubjects

                if (dueAssignments != null && subjects != null) {
                    val summaryBuilder = NotificationCompat.Builder(this, REMINDER_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_book_black_24dp)
                            .setGroup(GROUP_ID)
                            .setGroupSummary(true)

                    val summaryStyle = NotificationCompat.InboxStyle()

                    var summaryLineCount = 0
                    var overflowClasses = 0
                    var classesWithAssignmentsDue = 0

                    for (currentSubject in subjects) {
                        val notificationBuilder = NotificationCompat.Builder(this, CLASSES_CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_book_black_24dp)
                                .setContentTitle(currentSubject.name)
                                .setAutoCancel(true)
                                .setGroup(GROUP_ID)

                        val notificationStyle = NotificationCompat.BigTextStyle()
                        val sb = StringBuilder()
                        val className = currentSubject.name
                        val classId = currentSubject.id
                        var assignmentsDue = 0
                        for (assign in dueAssignments) {
                            if (assign.classId == currentSubject.id) {
                                sb.append(assign.title)
                                        .append("\n")
                                assignmentsDue++
                            }
                        }
                        if (assignmentsDue > 0) {
                            summaryStyle.addLine(className + " " + if (assignmentsDue == 1) sb.toString() else assignmentsDue.toString() + " assignments due")
                            summaryLineCount++
                            if (summaryLineCount > 6) overflowClasses++

                            notificationStyle.bigText(sb.toString().trim { it <= ' ' })
                            val notif = notificationBuilder.setContentText(if (assignmentsDue == 1) sb.toString() else assignmentsDue.toString() + " assignments due")
                                    .setStyle(notificationStyle).build()
                            classesWithAssignmentsDue++
                            if (mManager != null)
                                mManager!!.notify(classId, notif)
                            else {
                                val notifManagerCompat = NotificationManagerCompat.from(this)
                                notifManagerCompat.notify(classId, notif)
                            }
                        }
                    }
                    summaryStyle
                            .setBigContentTitle(classesWithAssignmentsDue.toString() + (if (classesWithAssignmentsDue == 1) " class " else " subjects ") + "with assignments due")

                    if (overflowClasses > 0) {
                        summaryStyle.setSummaryText("+" + overflowClasses + " other " + if (overflowClasses == 1) "class" else "subjects")
                    }

                    summaryBuilder.setStyle(summaryStyle)
                    val summaryNotif = summaryBuilder.build()
                    if (mManager != null)
                        mManager!!.notify(SUMMARY_NOTIF_ID, summaryNotif)
                    else {
                        val notifManagerCompat = NotificationManagerCompat.from(this)
                        notifManagerCompat.notify(SUMMARY_NOTIF_ID, summaryNotif)
                    }

                    scheduleNotification()
                }
            }
        }.start()
    }

    /**
     * Schedules a notification, based on the user's preference
     */
    fun scheduleNotification() {
        val intent = Intent(this, NotificationPublishReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val dtNow = DateTime()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val daysToNotify = stringSetToIntegerList(sharedPreferences.getStringSet(getString(R.string.pref_show_notif_days_key), null))

        if (daysToNotify != null) {
            for (i in daysToNotify.indices) {
                val dayOfWeek = daysToNotify[i]
                val dayToNotify = getNotificationTime(dayOfWeek)
                if (dayOfWeek >= dtNow.dayOfWeek && dtNow.isBefore(dayToNotify)) {
                    setAlarm(dayToNotify, pendingIntent)
                    return
                }
            }

            val dayToNotify = getNotificationTime(daysToNotify[0])
                    .plusWeeks(1)

            setAlarm(dayToNotify, pendingIntent)
        }
    }

    /**
     * Sets the alarm to be fired at the specified time
     *
     * @param time          The time to fire the notification at
     * @param pendingIntent The pending intent containing a [NotificationPublishReceiver]
     */
    private fun setAlarm(time: DateTime, pendingIntent: PendingIntent) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager?.set(AlarmManager.RTC_WAKEUP, time.millis, pendingIntent)
    }

    /**
     * Cancels any scheduled notifications
     */
    fun cancelNotification() {
        val intent = Intent(this, NotificationPublishReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager?.cancel(pendingIntent)
    }

    /**
     * Gets the notification time based on the day of week and user preference
     *
     * @param dayToNotify The weekday to notify on
     * @return A [DateTime] containing information on the date and time to fire the notification
     * at
     */
    private fun getNotificationTime(dayToNotify: Int): DateTime {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val dtNow = DateTime()
        val timeToNotify = sharedPreferences.getString(getString(R.string.pref_show_notif_time_key), "")

        var dtNotifyOn = dtNow.withDayOfWeek(dayToNotify)

        val timeParts = timeToNotify!!.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        dtNotifyOn = dtNotifyOn.withHourOfDay(Integer.valueOf(timeParts[0]))
        dtNotifyOn = dtNotifyOn.withMinuteOfHour(Integer.valueOf(timeParts[1]))
        dtNotifyOn = dtNotifyOn.withSecondOfMinute(0)

        return dtNotifyOn
    }

    companion object {
        private val SUMMARY_NOTIF_ID = 99999 // High number in case some person for some reason is taking 99998 classes.....

        private val CLASSES_CHANNEL_ID = "classes"
        private val REMINDER_CHANNEL_ID = "reminder"
        private val GROUP_ID = "assignments"

        /**
         * Utility method to convert a string set to a list of integers
         *
         * @param set The string set to convert
         * @return A sorted list of integers
         */
        private fun stringSetToIntegerList(set: Set<String>?): List<Int>? {
            if (set != null) {
                val list = ArrayList<Int>()
                for (s in set) {
                    list.add(Integer.valueOf(s))
                }
                Collections.sort(list)
                return list
            }
            return null
        }
    }
}