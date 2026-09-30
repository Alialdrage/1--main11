package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiRecitationService {
    private const val TAG = "GeminiRecitation"
    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getRecitationTeacherFeedback(
        ayahWithTashkeel: String,
        spokenText: String,
        accuracyScore: Int,
        missingOrWrongWords: List<String>
    ): String? = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.d(TAG, "Gemini API key is not configured; using offline evaluator.")
            return@withContext null
        }

        try {
            val prompt = """
                أنت معلم قرآن وتجويد رفيق ومشجع للمبتدئين والأطفال.
                قام الطالب المبتدئ بقراءة وتسميع هذه الآية الكريمة:
                الآية الأصلية: "$ayahWithTashkeel"
                ما نطق به الطالب (من التعرف الصوتي): "$spokenText"
                نسبة دقة الكلمات المقروءة: $accuracyScore%
                الكلمات التي قد تحتاج تصحيحاً أو لم تُنطق: ${missingOrWrongWords.joinToString("، ")}

                المطلوب:
                قدم تقييماً موجزاً ومشجعاً باللغة العربية الفصحى المبسطة:
                1. عبارة تشجيعية دافئة ولطيفة تناسب المبتدئ.
                2. نصيحة عملية واحدة أو اثنتين بخصوص مخارج الحروف أو أحكام التجويد (مثل القلقلة، الغنة، المد) في هذه الآية تحديداً.
                3. توجيه واضح لكيفية تصحيح الكلمات المتعثرة إن وجدت.
                اجعل الرد مختصراً في حدود 3 إلى 4 أسطر فقط وبأسلوب محبب ومحفز.
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 500)
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API returned error code ${response.code}")
                return@withContext null
            }

            val responseText = response.body?.string() ?: return@withContext null
            val rootJson = JSONObject(responseText)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@withContext parts.getJSONObject(0).optString("text", "")
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error invoking Gemini API for recitation feedback", e)
            null
        }
    }
}
