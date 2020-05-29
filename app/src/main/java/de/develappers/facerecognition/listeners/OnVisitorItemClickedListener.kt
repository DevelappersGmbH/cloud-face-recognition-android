package de.develappers.facerecognition.listeners

import de.develappers.facerecognition.database.model.RecognisedCandidate

interface OnVisitorItemClickedListener {
    fun onVisitorItemClicked(candidate: RecognisedCandidate)
}