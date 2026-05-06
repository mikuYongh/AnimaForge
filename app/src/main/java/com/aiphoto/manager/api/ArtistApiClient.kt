package com.aiphoto.manager.api

import android.util.Log
import com.aiphoto.manager.data.model.Artist
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ArtistApiClient {

    companion object {
        const val BASE_URL = "https://cdn.mooshieblob.com/20260325_anima_all_artists"
        const val SEARCH_URL = "$BASE_URL/indices/search.json"

        fun getImageUrl(imageId: String): String {
            return "$BASE_URL/images/$imageId.webp"
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    @Volatile
    private var cachedArtists: List<Artist>? = null

    suspend fun fetchArtists(forceRefresh: Boolean = false): Result<List<Artist>> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedArtists != null) {
            return@withContext Result.success(cachedArtists!!)
        }

        try {
            val request = Request.Builder().url(SEARCH_URL).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("空响应"))
                val artists: List<Artist> = gson.fromJson(body, object : TypeToken<List<Artist>>() {}.type)
                cachedArtists = artists
                Log.d("ArtistApiClient", "加载了 ${artists.size} 个画师")
                Result.success(artists)
            } else {
                Result.failure(Exception("请求失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ArtistApiClient", "获取画师列表失败", e)
            Result.failure(e)
        }
    }
}
