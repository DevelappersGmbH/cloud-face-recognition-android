package de.develappers.facerecognition.retrofit

import de.develappers.facerecognition.FACE_URL
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class Connect {

    companion object {

        private fun getRetrofit(Url:String):Retrofit {
            return Retrofit.Builder()
                .addCallAdapterFactory(
                    RxJava2CallAdapterFactory.create())
                .addConverterFactory(
                    GsonConverterFactory.create())
                .baseUrl(Url)
                .build()
        }

        fun getApiData(): Retrofit {
            val retrofitApi = getRetrofit(FACE_URL)
            return retrofitApi
        }

        fun faceApi() : FaceApi{
            val retrofitCall = getApiData()
            return retrofitCall.create(FaceApi::class.java)
        }

    }
}