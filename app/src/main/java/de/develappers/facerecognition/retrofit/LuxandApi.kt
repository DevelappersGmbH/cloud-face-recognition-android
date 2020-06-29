package de.develappers.facerecognition.retrofit

import de.develappers.facerecognition.BuildConfig
import de.develappers.facerecognition.LUXAND_URL
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*


interface LuxandApi {

    companion object {
        fun create(): LuxandApi {

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            val httpClient = OkHttpClient.Builder()
            httpClient.addInterceptor(logging) // <-- this is the important line!

            val retrofit = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(LUXAND_URL)
                .client(httpClient.build())
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
        @Part photoFile: MultipartBody.Part,
        @Part("threshold") requestThreshold : RequestBody,
        @Part("all") allCandidates : RequestBody
    ): List<LuxandFace>

    @Headers ("token: ${BuildConfig.LUXAND_KEY}")
    @GET("/subject")
    suspend fun listPersons(
    ): List<LuxandPerson>

    @Headers ("token: ${BuildConfig.LUXAND_KEY}")
    @DELETE("/subject/{id}")
    suspend fun deletePerson(
        @Path("id") luxandId: Int
    )

}