package de.develappers.facerecognition.database.model

import java.io.Serializable

class RecognisedCandidate(): Serializable {
    lateinit var visitor: Visitor
    var microsoft_conf: Double? = null
    var amazon_conf: Double? = null
    var kairos_conf: Double? = null
    var face_conf: Double? = null
    var luxand_conf: Double? = null
}
