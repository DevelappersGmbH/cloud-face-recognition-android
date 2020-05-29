package de.develappers.facerecognition.serviceAI

import android.content.Context
import de.develappers.facerecognition.R
import java.lang.IllegalArgumentException

class ServiceFactory() {

    companion object {
        fun createAIServices(context: Context, values: Map<Int, Boolean>): MutableList<RecognitionService> {
            val serviceProviders = mutableListOf<RecognitionService>()
            values.keys.forEach{
                serviceProviders.add(
                    when (it) {
                        R.string.microsoft -> MicrosoftServiceAI(
                            context,
                            context.getString(it),
                            values.getOrDefault(it, false)
                        )
                        R.string.amazon -> AmazonServiceAI(
                            context,
                            context.getString(it),
                            values.getOrDefault(it, false)
                        )
                        R.string.face -> FaceServiceAI(
                            context,
                            context.getString(it),
                            values.getOrDefault(it, false)
                        )
                        R.string.kairos -> KairosServiceAI(
                            context,
                            context.getString(it),
                            values.getOrDefault(it, false)
                        )
                        R.string.luxand -> LuxandServiceAI(
                            context,
                            context.getString(it),
                            values.getOrDefault(it, false)
                        )
                        else -> throw IllegalArgumentException("No such service")
                    })
                }
            return serviceProviders
        }

    }


}