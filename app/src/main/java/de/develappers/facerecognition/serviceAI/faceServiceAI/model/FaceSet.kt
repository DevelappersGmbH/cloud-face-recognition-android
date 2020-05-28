package de.develappers.facerecognition.serviceAI.faceServiceAI.model

import java.io.Serializable

class FaceSet : Serializable {
    var faceset_token: String? = null
    var outer_id: String? = null
    var display_name: String? = null
    var tags: String? = null
    var user_data: String? = null

    override fun toString(): String {
        return "{" +
                "\"faceset_token\":\'" + faceset_token + "\'" +
                ", \"outer_id\":\'" + outer_id + "\'" +
                ", \"display_name\":\'" + display_name + "\'" +
                ", \"tags\":\'" + tags + "\'" +
                ", \"user_data\":\'" + user_data + "\'" +
                '}'
    }
}