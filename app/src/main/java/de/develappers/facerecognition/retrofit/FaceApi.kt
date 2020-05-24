package de.develappers.facerecognition.retrofit

import de.develappers.facerecognition.FACE_URL
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.FaceSetCreateResponse
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.FaceSetDeleteResponse
import io.reactivex.Observable
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

interface FaceApi {

    companion object {
        fun create(): FaceApi {

            val retrofit = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(FACE_URL)
                .build()

            return retrofit.create(FaceApi::class.java)
        }
    }
    @FormUrlEncoded
    @POST("faceset/create")
    suspend fun createFaceSet(@Field("api_key") apiKey: String,
                              @Field("api_secret") apiSecret: String,
                              @Field("outer_id") outerId: String):
            Observable<FaceSetCreateResponse>
    @FormUrlEncoded
    @POST("faceset/delete")
    suspend fun deleteFaceSet(@Field("api_key") apiKey: String,
                              @Field("api_secret") apiSecret: String,
                              @Field("outer_id") outerId: String):
            FaceSetDeleteResponse
}