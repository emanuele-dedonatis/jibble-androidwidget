package it.mumu.jibblewidget

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.DefaultHttpClient
import java.io.InputStreamReader

enum class JibbleStatus {
    IN, OUT, UNKNOWN
}

internal fun getJibbleStatus(username: String, password: String): JibbleStatus {
    try {
        // Login
        val (sessionToken,userId) = login(username, password)
        if(sessionToken == null || userId == null) {
            return JibbleStatus.UNKNOWN
        }

        // Company ID
        val companyId = company(sessionToken,userId) ?: return JibbleStatus.UNKNOWN

        // Last action
        val lastAction = lastAction(sessionToken, companyId) ?: return JibbleStatus.UNKNOWN
        return when (lastAction) {
            1-> JibbleStatus.IN
            2 -> JibbleStatus.OUT
            else -> JibbleStatus.UNKNOWN
        }
    }catch(e: Exception) {
        return JibbleStatus.UNKNOWN
    }

}

private fun login(username: String, password: String): Pair<String?,String?> {
    val client = DefaultHttpClient();
    val request = HttpPost("https://api.jibble.io/api/v1/functions/logInUser");
    request.setHeader("Content-type", "application/json");
    val requestEntity = StringEntity(
        """{ 
                  "_ApplicationId":"EdVXcwrUCkJu2T2mUfAgzemvSDDxYqDLECvx24Wk",
                  "username":"$username",
                  "password":"$password"
              }"""
    )
    request.setEntity(requestEntity);

    val response = client.execute(request) as HttpResponse;
    val inputStream = InputStreamReader(response.getEntity().getContent())
    val jsonObject = Parser.default().parse(inputStream) as JsonObject

    val sessionToken = jsonObject.obj("result")?.string("sessionToken")
    val userId = jsonObject.obj("result")?.string("objectId")
    return Pair(sessionToken, userId);
}

private fun company(sessionToken: String, userId: String): String? {
    val client = DefaultHttpClient();
    val request = HttpPost("https://api.jibble.io/api/v1/functions/fetchRelatedCompanies");
    request.setHeader("Content-type", "application/json");
    val requestEntity = StringEntity(
        """{
                  "_ApplicationId":"EdVXcwrUCkJu2T2mUfAgzemvSDDxYqDLECvx24Wk",
                  "_SessionToken":"$sessionToken",
                  "userId":"$userId"
              }"""
    )
    request.setEntity(requestEntity);

    val response = client.execute(request) as HttpResponse;
    val inputStream = InputStreamReader(response.getEntity().getContent())
    val jsonObject = Parser.default().parse(inputStream) as JsonObject

    return jsonObject.obj("result")?.array<JsonObject>("team_member")?.first()?.string("objectId");
}

private fun lastAction(sessionToken: String, companyId: String): Int? {
    val client = DefaultHttpClient();
    val request = HttpGetWithEntity("https://api.jibble.io/api/v1/classes/Person");
    //val request = HttpGetWithEntity("https://webhook.site/210d9c41-b0a1-48fa-b2e5-0b28d89831c8");
    request.setHeader("Content-type", "application/json");
    val requestEntity = StringEntity(
        """{
                  "_ApplicationId":"EdVXcwrUCkJu2T2mUfAgzemvSDDxYqDLECvx24Wk",
                  "_SessionToken":"$sessionToken",
                  "where":{
                    "company":{
                      "__type":"Pointer",
                      "className":"Company",
                      "objectId":"$companyId"
                    }
                  },
                  "include":"position",
                  "limit":1
              }"""
    )
    request.setEntity(requestEntity);

    val response = client.execute(request) as HttpResponse;
    val inputStream = InputStreamReader(response.getEntity().getContent())
    val jsonObject = Parser.default().parse(inputStream) as JsonObject
    return jsonObject.array<JsonObject>("results")?.first()?.int("lastAction");
}