package de.develappers.facerecognition.retrofit

import de.develappers.facerecognition.FACE_URL
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.DetectResponse
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.FaceSetCreateResponse
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.FaceSetDeleteResponse
import de.develappers.facerecognition.serviceAI.faceServiceAI.model.SearchResponse
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
    suspend fun createFaceSet(
        @Field("api_key") apiKey: String?,
        @Field("api_secret") apiSecret: String?,
        @Field("outer_id") outerId: String?
    ): FaceSetCreateResponse

    @FormUrlEncoded
    @POST("faceset/removeface")
    suspend fun removeFaceTokensFromSet(
        @Field("api_key") apiKey: String?,
        @Field("api_secret") apiSecret: String?,
        @Field("outer_id") outerId: String?,
        @Field("face_tokens") faceToken: String
    ): FaceSetDeleteResponse

    @FormUrlEncoded
    @POST("faceset/delete")
    suspend fun deleteFaceSet(
        @Field("api_key") apiKey: String?,
        @Field("api_secret") apiSecret: String?,
        @Field("outer_id") outerId: String?
    ): FaceSetDeleteResponse

    @FormUrlEncoded
    @POST("detect")
    suspend fun detect(
        @Field("api_key") apiKey: String?,
        @Field("api_secret") apiSecret: String?,
        @Field("image_base64") imgBase64: String?
    ): DetectResponse

    @FormUrlEncoded
    @POST("face/setuserid")
    suspend fun setUserId(
        @Field("api_key") apiKey: String?,
        @Field("api_secret") apiSecret: String?,
        @Field("face_token") faceToken: String?,
        @Field("user_id") localPathId: String?
    )

    @FormUrlEncoded
    @POST("faceset/addface")
    suspend fun addFaceToFaceSet(
        @Field("api_key") apiKey: String?,
        @Field("api_secret") apiSecret: String?,
        @Field("outer_id") outerId: String?,
        @Field("face_tokens") faceToken: String?
    ) : FaceSetCreateResponse

    @FormUrlEncoded
    @POST("search")
    suspend fun search(
        @Field("api_key") apiKey: String?,
        @Field("api_secret") apiSecret: String?,
        @Field("image_base64") imgBase64: String?,
        @Field("outer_id") outerId: String?,
        @Field("return_result_count") count: Int?
    ): SearchResponse
}