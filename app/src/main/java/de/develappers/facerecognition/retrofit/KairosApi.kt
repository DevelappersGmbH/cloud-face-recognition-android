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
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface KairosApi {

    companion object {
        fun create(): KairosApi {

            val retrofit = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(KAIROS_URL)
                .build()

            return retrofit.create(KairosApi::class.java)
        }
    }
//TODO: https://square.github.io/okhttp/interceptors/
    @Headers( "Content-type:application/json",
        "app_key: ${BuildConfig.KAIROS_KEY}",
        "app_id: ${BuildConfig.KAIROS_APP_ID}"
        )
    @POST("gallery/remove")
    suspend fun removeGallery(
        @Body request: GalleryRemoveRequest
    ): GalleryRemoveResponse

    @Headers( "Content-type:application/json",
        "app_key: ${BuildConfig.KAIROS_KEY}",
        "app_id: ${BuildConfig.KAIROS_APP_ID}"
    )
    @POST("enroll")
    suspend fun enroll(
        @Body request: EnrollRequest
    ): EnrollResponse

    @Headers( "Content-type:application/json",
        "app_key: ${BuildConfig.KAIROS_KEY}",
        "app_id: ${BuildConfig.KAIROS_APP_ID}"
    )
    @POST("recognize")
    suspend fun recognise(
        @Body request: RecogniseRequest
    ): RecogniseResponse
}