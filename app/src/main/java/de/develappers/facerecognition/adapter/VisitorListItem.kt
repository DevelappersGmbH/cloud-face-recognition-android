package de.develappers.facerecognition.adapter

import de.develappers.facerecognition.database.model.entities.Company

class VisitorListItem {
    var id: String? = null
    var firstName: String? = null
    var lastName: String? = null
    var company: Company? = null
    var privacyAccepted: Boolean? = false
    var sigPath: String? = null
    var imgPath: String? = null

}