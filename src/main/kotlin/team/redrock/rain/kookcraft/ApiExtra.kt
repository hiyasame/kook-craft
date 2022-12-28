package team.redrock.rain.kookcraft

import com.google.gson.JsonParser
import love.forte.simbot.component.kook.KookChannel
import love.forte.simbot.literal
import okhttp3.*
import java.io.IOException
import java.lang.RuntimeException
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * KookCraft
 * team.redrock.rain.kookcraft
 *
 * @author 寒雨
 * @since 2022/12/28 下午5:19
 */
private val okHttpClient by lazy { OkHttpClient.Builder().build() }

suspend fun KookChannel.rename(name: String, token: String) = suspendCoroutine {
    val body = FormBody.Builder()
        .add("channel_id", id.literal)
        .add("name", name)
        .build()
    val request = Request.Builder()
        .url("https://www.kookapp.cn/api/v3/channel/update")
        .addHeader("Authorization", "Bot $token")
        .post(body)
        .build()
    okHttpClient.newCall(request)
        .enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                it.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonObject = JsonParser().parse(response.body?.string()).asJsonObject
                val result = if (jsonObject.get("code").asInt == 0) Result.success(Unit)
                    else Result.failure(RuntimeException("请求失败: $jsonObject"))
                it.resumeWith(result)
            }

        })
}