package com.aiphoto.manager.api

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import retrofit2.http.GET

interface ComfyUIApi {
    @POST("prompt")
    suspend fun queuePrompt(@Body request: RequestBody): Response<ResponseBody>

    @GET("history/{promptId}")
    suspend fun getHistory(@Path("promptId") promptId: String): Response<Map<String, HistoryResponse>>

    @GET("history/{promptId}")
    suspend fun getHistoryRaw(@Path("promptId") promptId: String): Response<ResponseBody>

    @GET("history")
    suspend fun getHistory(@Query("max_items") maxItems: Int): Response<JsonObject>

    @GET("queue")
    suspend fun getQueue(): Response<JsonObject>

    @POST("interrupt")
    suspend fun interrupt(): Response<ResponseBody>

    @GET("view")
    @Streaming
    suspend fun getImage(
        @Query("filename") filename: String,
        @Query("subfolder") subfolder: String?,
        @Query("type") type: String
    ): Response<ResponseBody>

    @Multipart
    @POST("upload/image")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<Map<String, String>>

    // 模型列表查询
    @GET("object_info/UNETLoader")
    suspend fun getUNETLoaderInfo(): Response<JsonObject>

    @GET("object_info/LoraLoader")
    suspend fun getLoraLoaderInfo(): Response<JsonObject>
}

data class HistoryResponse(
    val outputs: Map<String, OutputInfo>? = null
)

data class OutputInfo(
    val images: List<ImageInfo>? = null
)

data class ImageInfo(
    val filename: String,
    val subfolder: String?,
    val type: String
)
