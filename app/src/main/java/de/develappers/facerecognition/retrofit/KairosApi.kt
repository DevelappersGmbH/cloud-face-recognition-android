package de.develappers.facerecognition.retrofit

import de.develappers.facerecognition.BuildConfig
import de.develappers.facerecognition.FACE_URL
import de.develappers.facerecognition.FaceApp
import de.develappers.facerecognition.KAIROS_URL
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.DetectResponse
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.FaceSetCreateResponse
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.FaceSetDeleteResponse
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.SearchResponse
import de.develappers.facerecognition.serviceAI.kairosServiceAI.model.*
import io.reactivex.Observable
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface KairosApi {

    companion object {
        fun create(): KairosApi {

           //https://stackoverflow.com/questions/32605711/adding-header-to-all-request-with-retrofit-2
            val builder = OkHttpClient().newBuilder()
            builder.addInterceptor { chain: Interceptor.Chain ->
                val request =
                    chain
                        .request()
                        .newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .addHeader("app_key", BuildConfig.KAIROS_KEY).build()
                chain.proceed(request)
            }

            val retrofit = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(KAIROS_URL)
                .client(builder.build())
                .build()

            return retrofit.create(KairosApi::class.java)
        }
    }

    @Headers("app_id: ${BuildConfig.KAIROS_APP_ID}")
    @POST("gallery/remove")
    suspend fun removeGallery(
        @Body request: GalleryRemoveRequest
    ): GalleryRemoveResponse

    @Headers("app_id: ${BuildConfig.KAIROS_APP_ID}")
    @POST("enroll")
    suspend fun enroll(
        @Body request: EnrollRequest
    ): EnrollResponse

    @Headers("app_id: ${BuildConfig.KAIROS_APP_ID}")
    @POST("recognize")
    suspend fun recognise(
        @Body request: RecogniseRequest
    ): RecogniseResponse
}