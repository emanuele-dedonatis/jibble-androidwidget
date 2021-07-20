package it.mumu.jibblewidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.*
import java.util.*

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

            // Request jibble status now
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
    val username = loadPref(context, PrefKey.USERNAME);
    val password = loadPref(context, PrefKey.PASSWORD);

    if(username != null && password != null) {
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val status = getJibbleStatus(username, password)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                withContext(Dispatchers.Main) {
                    updateView(context, appWidgetManager, appWidgetId, status)
                }
            }
        }
    }
}

internal fun updateView(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    status: JibbleStatus
) {
    val views = RemoteViews(context.packageName, R.layout.status_widget)

    // Update image view
    var srcId = R.drawable.jibble_unknown
    when (status) {
        JibbleStatus.IN -> srcId = R.drawable.jibble_in
        JibbleStatus.OUT -> srcId = R.drawable.jibble_out
    }
    views.setImageViewResource(R.id.imageView, srcId)

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
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.timeInMillis, pendingIntent)
}