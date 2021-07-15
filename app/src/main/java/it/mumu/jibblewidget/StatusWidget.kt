package it.mumu.jibblewidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.github.kittinunf.result.Result
import java.text.SimpleDateFormat
import java.util.*

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [StatusWidgetConfigureActivity]
 */

const val REQUEST_JIBBLE_STATUS = "REQUEST_JIBBLE_STATUS"
const val DEFAULT_INTERVAL = 30

class StatusWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if(appWidgetIds.isNotEmpty()) {
            // Save widget id (only one supported)
            val appWidgetId = appWidgetIds[0]
            savePref(context, PrefKey.WIDGETID, appWidgetId.toString())

            // Request jibble status
            requestJibbleStatus(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if(intent?.action == REQUEST_JIBBLE_STATUS) {
            val appWidgetId = loadPref(context, PrefKey.WIDGETID)
            if(appWidgetId != null) {
                requestJibbleStatus(context, appWidgetId.toInt())
            }
        }
        super.onReceive(context, intent)
    }
}

internal fun requestJibbleStatus(
    context: Context,
    appWidgetId: Int
) {
    // Update status
    Log.d("MUMU","Request jibble status")
    val username = loadPref(context, PrefKey.USERNAME);
    val password = loadPref(context, PrefKey.PASSWORD);
    Jibble.getStatus(context, appWidgetId, username, password)
}

internal fun updateView(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    status: Jibble.Status
) {
        Log.d("MUMU","UPDATE VIEW")

        // Update datetime
        val views = RemoteViews(context.packageName, R.layout.status_widget)
        when (status) {
            Jibble.Status.IN -> views.setTextViewText(R.id.tvStatus, "IN")
            Jibble.Status.OUT -> views.setTextViewText(R.id.tvStatus, "OUT")
            else -> views.setTextViewText(R.id.tvStatus, "UNKNOWN")
        }



        // Update datetime
        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        val currentDate = sdf.format(Date())
        views.setTextViewText(R.id.tvDatetime, currentDate)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Schedule next update
        val intent = Intent(context, StatusWidget::class.java)
        intent.action = REQUEST_JIBBLE_STATUS
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
        val trigger = Calendar.getInstance()
        val minutes = loadPref(context, PrefKey.INTERVAL) ?: DEFAULT_INTERVAL.toString();
        try{
            trigger.add(Calendar.MINUTE, minutes.toInt())
        }catch(e: NumberFormatException) {
            trigger.add(Calendar.MINUTE, DEFAULT_INTERVAL)
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pendingIntent)
}