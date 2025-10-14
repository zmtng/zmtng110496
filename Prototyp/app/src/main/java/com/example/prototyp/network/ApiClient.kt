package com.example.prototyp.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {

    // Basis-URL der Riot Games API für Europa
    private const val BASE_URL = "https://europe.api.riotgames.com/"

    // Moshi-Instanz zum Parsen von JSON-Antworten
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Retrofit-Instanz, die die API-Anfragen durchführt
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    /**
     * Erstellt den Service für die Riftbound Content API,
     * den wir für unsere API-Calls verwenden.
     */
    val riftboundContentApi: RiftboundContentApi by lazy {
        retrofit.create(RiftboundContentApi::class.java)
    }
}