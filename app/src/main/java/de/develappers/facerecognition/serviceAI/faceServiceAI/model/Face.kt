package de.develappers.facerecognition.serviceAI.faceServiceAI.model

import com.amazonaws.services.rekognition.model.FaceAttributes


class Face {
    var face_token: String? = null
    private var face_rectangle: CommonRect? = null
    private var landmark: FaceLandmark? = null
    var attributes: FaceAttributes? = null

    fun getFace_rectangle(): CommonRect? {
        return face_rectangle
    }

    fun setFace_rectangle(face_rectangle: CommonRect?) {
        this.face_rectangle = face_rectangle
    }

    fun getLandmark(): FaceLandmark? {
        return landmark
    }

    fun setLandmark(landmark: FaceLandmark?) {
        this.landmark = landmark
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val face = o as Face
        if (if (face_token != null) face_token != face.face_token else face.face_token != null) return false
        if (if (face_rectangle != null) !face_rectangle?.equals(face.face_rectangle)!! else face.face_rectangle != null) return false
        if (if (landmark != null) !landmark?.equals(face.landmark)!! else face.landmark != null) return false
        return if (attributes != null) attributes == face.attributes else face.attributes == null
    }

    override fun hashCode(): Int {
        var result = if (face_token != null) face_token.hashCode() else 0
        result = 31 * result + if (face_rectangle != null) face_rectangle.hashCode() else 0
        result = 31 * result + if (landmark != null) landmark.hashCode() else 0
        result = 31 * result + if (attributes != null) attributes.hashCode() else 0
        return result
    }

    override fun toString(): String {
        return "{" +
                "\"face_token\":\'" + face_token + "\'" +
                ", \"face_rectangle\":" + face_rectangle +
                ", \"landmark\":" + landmark +
                ", \"attributes\":" + attributes +
                '}'
    }
}