package de.develappers.facerecognition.listeners

import android.graphics.Bitmap
import de.develappers.facerecognition.database.model.RecognisedCandidate
import de.develappers.facerecognition.database.model.Visitor

interface OnVisitorItemClickedListener {
    fun onVisitorItemClicked(visitor: Visitor)
}