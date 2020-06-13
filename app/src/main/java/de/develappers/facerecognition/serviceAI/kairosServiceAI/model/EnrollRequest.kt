package de.develappers.facerecognition.serviceAI.kairosServiceAI.model

class EnrollRequest(val image : String,
                    val subject_id : String,
                    val gallery_name : String,
                    val multiple_faces : Boolean = true)