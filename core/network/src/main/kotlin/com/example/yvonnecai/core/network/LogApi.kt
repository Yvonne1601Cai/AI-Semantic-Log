package com.example.yvonnecai.core.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/** Retrofit definition of the log endpoint. */
interface LogApi {

    /**
     * Downloads the raw payload (~1.4 MB, 5,000 entries).
     *
     * `@Streaming` stops Retrofit from buffering the whole body in memory, and returning
     * [ResponseBody] lets the caller decode it straight from the byte stream.
     */
    @Streaming
    @GET
    suspend fun getLogs(@Url url: String): ResponseBody
}
