package gg.solara.discord.core.retrofit

import gg.solara.discord.core.retrofit.tebex.TebexService
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Configuration
class RetrofitConfiguration
{
    @Value("\${solara.tebex.apikey}") lateinit var tebexApiKey: String

    @Bean
    fun json() = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Bean
    fun tebexService() = tebexRetrofit().create(TebexService::class.java)

    @Bean
    fun okHttpClient() = OkHttpClient.Builder().build()

    @Bean
    fun tebexRetrofit(): Retrofit
    {
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor { chain ->
            val request: Request = chain.request().newBuilder()
                .addHeader("X-Tebex-Secret", tebexApiKey)
                .build()
            chain.proceed(request)
        }

        return Retrofit.Builder()
            .client(httpClient.build())
            .baseUrl("https://plugin.tebex.io/")
            .addConverterFactory(json().asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
