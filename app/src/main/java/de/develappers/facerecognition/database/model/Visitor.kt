package de.develappers.facerecognition.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.develappers.facerecognition.database.model.Company
import java.io.Serializable

@Entity
data class Visitor(
    @ColumnInfo(name = "first_name") var firstName: String?,
    @ColumnInfo(name = "last_name") var lastName: String?,
    @Embedded var company: Company?,
    @ColumnInfo(name = "privacy_accepted") var privacyAccepted: Boolean = false
):Serializable {
    @PrimaryKey (autoGenerate = true) var visitorId: Long = 0L
    @ColumnInfo(name = "sig_path") var sigPaths: MutableList<String> = mutableListOf()
    @ColumnInfo(name = "img_path") var imgPaths: MutableList<String> = mutableListOf()
    @ColumnInfo(name = "microsoft_id") var microsoftId: String = ""
    @ColumnInfo(name = "amazon_id") var amazonId: String = ""
    @ColumnInfo(name = "luxand_id") var luxandId: String = ""
    @ColumnInfo(name = "face_id") var faceId: String = ""
    @ColumnInfo(name = "kairos_id") var kairosId: String = ""

}