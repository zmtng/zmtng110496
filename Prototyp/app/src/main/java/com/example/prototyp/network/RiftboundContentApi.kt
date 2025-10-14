package com.example.prototyp.network

import com.example.prototyp.network.dto.ContentDto
import retrofit2.http.GET
import retrofit2.http.Header

/**
 * Definiert die Endpunkte für die Riftbound Content API.
 */
interface RiftboundContentApi {

    /**
     * Ruft alle Inhalte (Karten, Sets etc.) aus der API ab.
     * @param apiKey Dein persönlicher Riot API Key, der im Header mitgesendet wird.
     * @return Ein ContentDto-Objekt mit allen Daten.
     */
    @GET("riftbound-content/v1/content")
    suspend fun getContent(@Header("X-Riot-Token") apiKey: String): ContentDto
}