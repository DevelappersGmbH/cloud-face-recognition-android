package de.develappers.facerecognition.retrofit

import de.develappers.facerecognition.BuildConfig
import de.develappers.facerecognition.LUXAND_URL
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.AddFaceToPersonResponse
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.CreatePersonResponse
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.LuxandFace
import de.develappers.facerecognition.serviceAI.luxandServiceAI.model.LuxandPerson
import okhttp3.Interceptor
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
            val builder = OkHttpClient().newBuilder()

            if (BuildConfig.DEBUG) {
                val interceptor = HttpLoggingInterceptor()
                interceptor.level = HttpLoggingInterceptor.Level.BODY
                builder.addInterceptor(interceptor)
            }

            builder.addInterceptor { chain: Interceptor.Chain ->
                val request =
                    chain.request().newBuilder().addHeader("token", BuildConfig.LUXAND_KEY).build()
                chain.proceed(request)
            }

            val retrofit = Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(LUXAND_URL)
                .client(builder.build())
                .build()

            return retrofit.create(LuxandApi::class.java)
        }
    }

    @FormUrlEncoded
    @POST("/subject")
    suspend fun createPerson(
        @Field("name") personName: String?
    ): CreatePersonResponse

    @Multipart
    @POST("/subject/{id}")
    suspend fun addFaceToPerson(
        @Path("id") luxandId: Int?,
        @Part photoFile: MultipartBody.Part
    ): AddFaceToPersonResponse

    @Multipart
    @POST("photo/search")
    suspend fun search(
        @Part photoFile: MultipartBody.Part,
        @Part("threshold") requestThreshold : RequestBody,
        @Part("all") allCandidates : RequestBody
    ): List<LuxandFace>

    @GET("/subject")
    suspend fun listPersons(
    ): List<LuxandPerson>

    @DELETE("/subject/{id}")
    suspend fun deletePerson(
        @Path("id") luxandId: Int
    )

}