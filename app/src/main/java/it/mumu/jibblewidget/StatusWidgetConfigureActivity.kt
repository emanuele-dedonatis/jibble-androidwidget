package it.mumu.jibblewidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import it.mumu.jibblewidget.databinding.StatusWidgetConfigureBinding

/**
 * The configuration screen for the [StatusWidget] AppWidget.
 */
class StatusWidgetConfigureActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var appWidgetUsername: EditText
    private lateinit var appWidgetPassword: EditText
    private lateinit var appWidgetInterval: EditText
    private var onClickListener = View.OnClickListener {
        val context = this@StatusWidgetConfigureActivity

        // When the button is clicked, store the string locally
        val usernameText = appWidgetUsername.text.toString()
        val passwordText = appWidgetPassword.text.toString()
        val intervalText = appWidgetPassword.text.toString()
        savePref(context, PrefKey.USERNAME, usernameText)
        savePref(context, PrefKey.PASSWORD, passwordText)
        savePref(context, PrefKey.INTERVAL, intervalText)

        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        updateAppWidget(context, appWidgetManager, appWidgetId)

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    private lateinit var binding: StatusWidgetConfigureBinding

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        binding = StatusWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appWidgetUsername = binding.etUsername as EditText
        appWidgetPassword= binding.etPassword as EditText
        appWidgetInterval= binding.etInterval as EditText
        binding.addButton.setOnClickListener(onClickListener)

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        appWidgetUsername.setText(loadPref(this@StatusWidgetConfigureActivity, PrefKey.USERNAME))
        appWidgetPassword.setText(loadPref(this@StatusWidgetConfigureActivity, PrefKey.PASSWORD))
        appWidgetInterval.setText(loadPref(this@StatusWidgetConfigureActivity, PrefKey.INTERVAL))
    }

}

private const val PREFS_NAME = "it.mumu.jibblewidget.StatusWidget"
private const val PREF_PREFIX_KEY = "appwidget_"

internal enum class PrefKey {
    USERNAME, PASSWORD, INTERVAL
}

// Write the prefix to the SharedPreferences object for this widget
internal fun savePref(context: Context, key: PrefKey, text: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.putString(PREF_PREFIX_KEY + key, text)
    prefs.apply()
}

// Read the prefix from the SharedPreferences object for this widget.
// If there is no preference saved, get the default from a resource
internal fun loadPref(context: Context, key: PrefKey): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0)
    val titleValue = prefs.getString(PREF_PREFIX_KEY + key, null)
    return titleValue ?: ""
}

internal fun deletePref(context: Context, key: PrefKey) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.remove(PREF_PREFIX_KEY + key)
    prefs.apply()
}