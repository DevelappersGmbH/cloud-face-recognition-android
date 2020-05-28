package de.develappers.facerecognition.serviceAI.faceServiceAI.model

import java.io.Serializable

class SearchFaceResult : Serializable{
    var face_token: String? = null
    var confidence = 0f
    var user_id: String? = null

    override fun toString(): String {
        return "{" +
                "\"face_token\":\'" + face_token + "\'" +
                ", \"confidence\":" + confidence +
                ", \"user_id\":\'" + user_id + "\'" +
                '}'
    }
}