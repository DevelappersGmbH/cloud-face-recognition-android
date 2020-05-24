package de.develappers.facerecognition.serviceAI.faceServiceAI.model

open class FaceSetCreateResponse : BaseResponse() {
    var faceset_token: String? = null
    var outer_id: String? = null
    var face_added = 0
    var face_count = 0
    var failure_detail = arrayOf<FailureDetail>()

    override fun toString(): String {
        return "{" +
                "\"faceset_token\":\'" + faceset_token + "\'" +
                ", \"outer_id\":\'" + outer_id + "\'" +
                ", \"face_added\":" + face_added +
                ", \"face_count\":" + face_count +
                ", \"failure_detail\":" + failure_detail.toString() +
                '}'
    }

    class FailureDetail {
        var face_token: String? = null
        var reason: String? = null

        override fun toString(): String {
            return "{" +
                    "\"face_token\":\'" + face_token + "\'" +
                    ", \"reason\":\'" + reason + "\'" +
                    '}'
        }
    }
}