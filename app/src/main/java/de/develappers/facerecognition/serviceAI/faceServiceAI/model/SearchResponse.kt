package de.develappers.facerecognition.serviceAI.faceServiceAI.model

class SearchResponse : BaseResponse() {
    var results: List<SearchFaceResult>? = null
    var thresholds: Map<String, Float>? = null
    var faces: List<Face>? = null

    override fun toString(): String {
        return "{" +
                "\"results\":" + results +
                ", \"thresholds\":" + thresholds +
                ", \"faces\":" + faces +
                '}'
    }
}