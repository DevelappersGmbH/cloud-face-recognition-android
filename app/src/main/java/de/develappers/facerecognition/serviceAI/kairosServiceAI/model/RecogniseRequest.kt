package de.develappers.facerecognition.serviceAI.kairosServiceAI.model

class RecogniseRequest(val image : String,
                       val gallery_name : String,
                       val threshold : Double,
                       val max_number_results : Int)