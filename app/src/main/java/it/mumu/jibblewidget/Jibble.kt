package it.mumu.jibblewidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result

class Jibble {

    enum class Status {
        IN, OUT, UNKNOWN
    }

    companion object {

        fun getStatus(context: Context, appWidgetId: Int, username: String?, password: String?) {
            val httpAsync = "https://api.jibble.io/api/v1/functions/logInUser"
                .httpPost()
                .body("""
                  { 
                      "_ApplicationId":"EdVXcwrUCkJu2T2mUfAgzemvSDDxYqDLECvx24Wk",
                      "username":"$username",
                      "password":"$password"
                  }
                """)
                .responseString { request, response, result ->
                    when (result) {
                        is Result.Failure -> {
                            val ex = result.getException()
                            Log.d("ERR",ex.message?:"")
                            publish(context, appWidgetId, Status.UNKNOWN)
                        }
                        is Result.Success -> {
                            val data = result.get()
                            Log.d("SUCCESS",data)
                            publish(context, appWidgetId, Status.IN)
                        }
                    }
                }

            httpAsync.join()
        }

        fun publish(context: Context, appWidgetId: Int, status: Status) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            updateView(context, appWidgetManager, appWidgetId, status)
        }
    }
}