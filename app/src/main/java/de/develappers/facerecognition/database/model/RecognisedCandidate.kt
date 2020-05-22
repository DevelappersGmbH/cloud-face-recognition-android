package de.develappers.facerecognition.database.model

import java.io.Serializable

class RecognisedCandidate(var microsoft_conf: Double = 0.0,
                          var amazon_conf: Double = 0.0,
                          var kairos_conf: Double = 0.0,
                          var face_conf: Double = 0.0,
                          var luxand_conf: Double = 0.0): Serializable {
    lateinit var visitor: Visitor
}
