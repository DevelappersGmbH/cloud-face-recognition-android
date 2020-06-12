package de.develappers.facerecognition.serviceAI.luxandServiceAI.model

class LuxandFace (val id: Int, val name: String, val probability: Double, val rectangle: Rectangle){
    class Rectangle (val top: Int, val left: Int, val right: Int, val bottom: Int)
}