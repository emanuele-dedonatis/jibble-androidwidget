package it.mumu.jibblewidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
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
            setOnClickIntent(context, appWidgetManager, appWidgetId)
            setAlarm(context)
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
    // Show spinner
    val views = RemoteViews(context.packageName, R.layout.status_widget)
    views.setViewVisibility(R.id.imageView, View.GONE);
    views.setViewVisibility(R.id.progressBar, View.VISIBLE);
    val appWidgetManager = AppWidgetManager.getInstance(context)
    appWidgetManager.updateAppWidget(appWidgetId, views)

    val username = loadPref(context, PrefKey.USERNAME);
    val password = loadPref(context, PrefKey.PASSWORD);
    if(username != null && password != null) {
        GlobalScope.launch {
            // Request jibble status
            withContext(Dispatchers.IO) {
                val status = getJibbleStatus(username, password)

                // Update view
                withContext(Dispatchers.Main) {
                    updateView(context, status, appWidgetManager, appWidgetId)
                }
            }
        }
    }
}

internal fun updateView(
    context: Context,
    status: JibbleStatus,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    // Update image view
    var srcId = R.drawable.jibble_unknown
    when (status) {
        JibbleStatus.IN -> srcId = R.drawable.jibble_in
        JibbleStatus.OUT -> srcId = R.drawable.jibble_out
    }

    val views = RemoteViews(context.packageName, R.layout.status_widget)
    views.setImageViewResource(R.id.imageView, srcId)
    views.setViewVisibility(R.id.imageView, View.VISIBLE);
    views.setViewVisibility(R.id.progressBar, View.GONE);

    setOnClickIntent(context, appWidgetManager, appWidgetId)
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

internal fun setAlarm(
    context: Context
) {
    val intent = Intent(context, StatusWidget::class.java)
    intent.action = REQUEST_JIBBLE_STATUS
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

    val minutes = loadPref(context, PrefKey.INTERVAL) ?: DEFAULT_INTERVAL.toString();
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), minutes.toLong()*60000, pendingIntent)
}

internal fun setOnClickIntent(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    // Update status on click
    val intent = Intent(context, StatusWidget::class.java)
    intent.action = REQUEST_JIBBLE_STATUS
    val pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT)

    val views = RemoteViews(context.packageName, R.layout.status_widget)
    views.setOnClickPendingIntent(R.id.imageView, pendingIntent)
    appWidgetManager.updateAppWidget(appWidgetId, views)
}