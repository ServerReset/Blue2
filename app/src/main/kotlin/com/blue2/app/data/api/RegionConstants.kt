package com.blue2.app.data.api

import com.blue2.app.domain.models.BluelinkRegion
import java.util.UUID

object RegionConstants {

    data class RegionConfig(
        val baseUrl: String,
        val authBaseUrl: String = baseUrl,
        val clientId: String,
        val clientSecret: String,
        val appId: String,
        val serviceId: String = clientId,
        val redirectUri: String,
        val userAgent: String = "okhttp/3.14.9",
        val region: BluelinkRegion,
        val brand: String = "H",
        val basicToken: String = "",
        val cfbBlob: String = "",
        val gcmSenderId: String = "",
    )

    // US Hyundai — CCNC API
    val US = RegionConfig(
        baseUrl = "https://api.telematics.hyundaiusa.com/",
        clientId = "m66129Bb-em93-SPAHYN-bZ91-am4540zp19920",
        clientSecret = "v558o935-6nne-423i-baa8",
        appId = "14d5efbe-c194-4a5c-af66-e0ba8c8f4c80",
        serviceId = "m66129Bb-em93-SPAHYN-bZ91-am4540zp19920",
        redirectUri = "https://owners.hyundaiusa.com/us/en/index.html",
        region = BluelinkRegion.US,
    )

    // Canada Hyundai — CCNC API
    val CA = RegionConfig(
        baseUrl = "https://mybluelink.ca/",
        clientId = "HATAHSPACA0232141ED9722C67715A0B",
        clientSecret = "CLISCR01AHSPA",
        appId = "HATAHSPACA0232141ED9722C67715A0B",
        serviceId = "BLUELINKY",
        redirectUri = "https://mybluelink.ca/login",
        region = BluelinkRegion.CA,
    )

    // EU Hyundai — Gen5W / SPA API
    val EU = RegionConfig(
        baseUrl = "https://prd.eu-ccapi.hyundai.com:8080/",
        authBaseUrl = "https://idpconnect-eu.hyundai.com/",
        clientId = "6d477c38-3ca4-4cf3-9557-2a1929a94654",
        clientSecret = "KUy49XxPzLpLuoK0xhBC77W6VXhmtQR9iQhmIFjjoY4IpxsV",
        appId = "1eba27d2-9a5b-4eba-8ec7-97eb6c62fb51",
        serviceId = "6d477c38-3ca4-4cf3-9557-2a1929a94654",
        redirectUri = "https://prd.eu-ccapi.hyundai.com:8080/api/v1/user/oauth2/redirect",
        // Basic auth = base64(clientId:clientSecret)
        basicToken = "NmQ0NzdjMzgtM2NhNC00Y2YzLTk1NTctMmExOTI5YTk0NjU0OktVeTQ5WHhQekxwTHVvSzB4aEJDNzdXNlZYaG10UVI5aVFobUlGampvWTRJcHhzVg==",
        cfbBlob = "RFtoRq/vDXJmRndoZaZQyfOot7OrIqGVFj96iY2WL3yyH5Z/pUvlUhqmCxD2t+D65SQ=",
        gcmSenderId = "414998006775",
        region = BluelinkRegion.EU,
    )

    // Australia Hyundai — Gen5W
    val AU = RegionConfig(
        baseUrl = "https://au-apigw.ccs.hyundai.com.au:8080/",
        authBaseUrl = "https://idpconnect-au.hyundai.com.au/",
        clientId = "855c72df-dfd7-4230-ab03-67cbf902bb1c",
        clientSecret = "e6fbwHM32YNbhQl0pviaPp3rf4t3S6k91eceFMJLdbdThCO",
        appId = "f9ccfdac-a48d-4c57-bd32-9116963c24ed",
        basicToken = "ODU1YzcyZGYtZGZkNy00MjMwLWFiMDMtNjdjYmY5MDJiYjFjOmU2ZmJ3SE0zMllOYmhRbDBwdmlhUHAzcmY0dDNTNms5MWVjZUEzTUpMZGJkVGhDTw==",
        cfbBlob = "nGDHng3k4Cg9gWV+C+A6Yk/ecDopUNTkGmDpr2qVKAQXx9bvY2/YLoHPfObliK32F2g=",
        redirectUri = "https://au-apigw.ccs.hyundai.com.au:8080/api/v1/user/oauth2/redirect",
        region = BluelinkRegion.AU,
    )

    // Middle East Hyundai
    val ME = RegionConfig(
        baseUrl = "https://prd.eu-ccapi.hyundai.com:8080/",
        authBaseUrl = "https://idpconnect-eu.hyundai.com/",
        clientId = "6d477c38-3ca4-4cf3-9557-2a1929a94654",
        clientSecret = "KUy49XxPzLpLuoK0xhBC77W6VXhmtQR9iQhmIFjjoY4IpxsV",
        appId = "1eba27d2-9a5b-4eba-8ec7-97eb6c62fb51",
        basicToken = "NmQ0NzdjMzgtM2NhNC00Y2YzLTk1NTctMmExOTI5YTk0NjU0OktVeTQ5WHhQekxwTHVvSzB4aEJDNzdXNlZYaG10UVI5aVFobUlGampvWTRJcHhzVg==",
        cfbBlob = "RFtoRq/vDXJmRndoZaZQyfOot7OrIqGVFj96iY2WL3yyH5Z/pUvlUhqmCxD2t+D65SQ=",
        redirectUri = "https://prd.eu-ccapi.hyundai.com:8080/api/v1/user/oauth2/redirect",
        region = BluelinkRegion.ME,
    )

    fun forRegion(region: BluelinkRegion): RegionConfig = when (region) {
        BluelinkRegion.US -> US
        BluelinkRegion.CA -> CA
        BluelinkRegion.EU -> EU
        BluelinkRegion.AU -> AU
        BluelinkRegion.ME -> ME
    }

    fun generateDeviceId(): String = UUID.randomUUID().toString()

    // EU/AU Stamp generation using the CFB blob. The bluelinky-stamps repo
    // publishes pre-computed stamps; we fall back to HMAC-SHA256 of appId:epoch.
    fun generateStamp(appId: String, cfbBlob: String, epoch: Long = System.currentTimeMillis() / 1000): String {
        return try {
            val key = android.util.Base64.decode(cfbBlob, android.util.Base64.DEFAULT)
            val toHash = "$appId:$epoch"
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            val keySpec = javax.crypto.spec.SecretKeySpec(key.take(32).toByteArray(), "HmacSHA256")
            mac.init(keySpec)
            val hash = mac.doFinal(toHash.toByteArray())
            android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            // Fallback: epoch-based string
            epoch.toString()
        }
    }

    fun basicAuth(clientId: String, clientSecret: String): String =
        okhttp3.Credentials.basic(clientId, clientSecret)

    // EU temperature code mapping (e.g. 22°C → "10H")
    fun celsiusToTempCode(celsius: Double): String {
        val clamped = celsius.coerceIn(16.0, 28.0)
        val index = ((clamped - 16.0) / 0.5).toInt()
        return "%02XH".format(6 + index)
    }

    // US/CA remote start needs Fahrenheit index
    fun celsiusToFahrenheit(celsius: Double): Int = (celsius * 9.0 / 5.0 + 32).toInt()
}
