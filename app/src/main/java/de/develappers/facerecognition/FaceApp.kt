package de.develappers.facerecognition

import android.app.Application
import android.os.Environment
import android.util.Log
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.rekognition.AmazonRekognition
import com.amazonaws.services.rekognition.AmazonRekognitionClient
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import de.develappers.facerecognition.retrofit.FaceApi
import de.develappers.facerecognition.retrofit.KairosApi
import de.develappers.facerecognition.retrofit.LuxandApi
import java.io.File


class FaceApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MicrosoftServiceClient =
            FaceServiceRestClient(getString(R.string.microsoft_endpoint), BuildConfig.MICROSOFT_KEY)

        AmazonServiceClient =
            AmazonRekognitionClient(BasicAWSCredentials(BuildConfig.AWS_ACCESS_KEY_ID, BuildConfig.AWS_SECRET_ACCESS_KEY))

        storageDirectory = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        galleryFolder = File(storageDirectory, applicationContext.getString(R.string.app_name))
        if (!galleryFolder.exists()) {
            val wasCreated: Boolean = galleryFolder.mkdirs()
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory")
            }
        }

        values = mapOf(Pair(R.string.microsoft, MICROSOFT),
            Pair(R.string.amazon, AMAZON),
            Pair(R.string.face, FACE),
            Pair(R.string.kairos, KAIROS),
            Pair(R.string.luxand, LUXAND))

    }

    companion object {
        val microsoftServiceClient: FaceServiceClient?
            get() = MicrosoftServiceClient
        private var MicrosoftServiceClient: FaceServiceClient? = null

        val amazonServiceClient: AmazonRekognition?
            get() = AmazonServiceClient
        private var AmazonServiceClient: AmazonRekognition? = null

        val faceApi by lazy {
            FaceApi.create()
        }
        val luxandApi by lazy {
            LuxandApi.create()
        }

        val kairosApi by lazy {
            KairosApi.create()
        }

        var storageDirectory: File? = null
        lateinit var values: Map<Int, Boolean>
        lateinit var galleryFolder: File



    }
}