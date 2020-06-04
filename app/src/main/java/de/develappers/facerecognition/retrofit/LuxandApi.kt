package de.develappers.facerecognition.retrofit

import de.develappers.facerecognition.BuildConfig
import de.develappers.facerecognition.FACE_URL
import de.develappers.facerecognition.LUXAND_URL
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.DetectResponse
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.FaceSetCreateResponse
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.FaceSetDeleteResponse
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.SearchResponse
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.AddFaceToPersonResponse
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.CreatePersonResponse
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.LuxandFace
import io.reactivex.Observable
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface LuxandApi {

    companion object {
        fun create(): LuxandApi {

            val retrofit = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(LUXAND_URL)
                .build()

            return retrofit.create(LuxandApi::class.java)
        }
    }

    @Headers ("token: ${BuildConfig.LUXAND_KEY}")
    @FormUrlEncoded
    @POST("/subject")
    suspend fun createPerson(
        @Field("name") personName: String?
    ): CreatePersonResponse

    @Headers ("token: ${BuildConfig.LUXAND_KEY}")
    @Multipart
    @POST("/subject/{id}")
    suspend fun addFaceToPerson(
        @Path("id") luxandId: Int?,
        @Part photoFile: MultipartBody.Part
    ): AddFaceToPersonResponse


    @Headers ("token: ${BuildConfig.LUXAND_KEY}")
    @Multipart
    @POST("photo/search")
    suspend fun search(
        @Part photoFile: MultipartBody.Part
    ): List<LuxandFace>

    /*@Headers ("token: ${BuildConfig.LUXAND_KEY}")
    @FormUrlEncoded
    @Multipart
    @POST("/subject/{id}")
    suspend fun addFaceToPerson(
        @Path("id") luxandId: Int?,
        @Field("photo") photoFile: MultipartBody.Part
    ): AddFaceToPersonResponse*/



}