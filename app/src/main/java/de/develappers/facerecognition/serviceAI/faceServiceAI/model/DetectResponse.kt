package de.develappers.facerecognition.serviceAI.faceServiceAI.model

class DetectResponse : BaseResponse() {
    var faces // 人脸列表
            : List<Face>? = null

    override fun toString(): String {
        return "{" +
                "\"faces\":" + faces +
                '}'
    }
}