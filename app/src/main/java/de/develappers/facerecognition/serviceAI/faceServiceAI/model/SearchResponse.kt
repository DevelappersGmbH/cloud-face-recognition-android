package de.develappers.facerecognition.serviceAI.faceServiceAI.model

class SearchResponse : BaseResponse() {
    private var results: List<SearchFaceResult>? = null
    var thresholds: Map<String, Float>? = null
    var faces: List<Face>? =
        null

    fun getResults(): List<SearchFaceResult>? {
        return results
    }

    fun setResults(results: List<SearchFaceResult>?) {
        this.results = results
    }

    override fun toString(): String {
        return "{" +
                "\"results\":" + results +
                ", \"thresholds\":" + thresholds +
                ", \"faces\":" + faces +
                '}'
    }
}