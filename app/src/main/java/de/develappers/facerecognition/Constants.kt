package de.develappers.facerecognition

import android.Manifest
import android.hardware.camera2.CameraCharacteristics

const val FACE_URL: String = "https://api-us.faceplusplus.com/facepp/v3/"
const val KAIROS_URL: String = "https://api.kairos.com/"
//TODO: https://stackoverflow.com/questions/32605711/adding-header-to-all-request-with-retrofit-2
const val LUXAND_URL: String = "https://api.luxand.cloud/"

const val VISITORS_GROUP_ID: String = "1"
const val VISITORS_GROUP_DESCRIPTION: String = "all visitors"
const val VISITORS_GROUP_NAME: String = "visitors"

const val LOGBOOK_EXTRA: String = "extra_logbook"
const val VISITOR_EXTRA: String = "extra_visitor"
const val VISITOR_FIRST_TIME: String = "extra_first_time_visitor"
const val RECOGNISED_CANDIDATE_EXTRA: String = "extra_recognised_candidate"
const val NEW_IMAGE_PATH_EXTRA: String = "extra_new_image"
const val CANDIDATES_EXTRA: String = "extra_candidates"

const val MICROSOFT: Boolean = false
const val AMAZON: Boolean = false
const val KAIROS: Boolean = false
const val FACE: Boolean = true
const val LUXAND: Boolean = false

const val TYPE_PHOTO: String = "image_type_photo"
const val TYPE_SIGNATURE: String = "image_type_signature"

const val APP_MODE_REALTIME: String = "realtime"
const val APP_MODE_DATABASE: String = "database_testing"
const val APP_MODE: String = APP_MODE_DATABASE

const val CONFIDENCE_MATCH: Double = 0.99
const val CONFIDENCE_CANDIDATE: Double = 0.0
const val RETURN_RESULT_COUNT: Int = 5 //for Face++ from 1 to max 5 returned candidates
const val REMOVE_ALL_TOKENS: String = "RemoveAllFaceTokens" //remove all tokens from face++ before deleting FaceSet

const val REQUEST_CAMERA_PERMISSION = 0
const val REQUEST_STORAGE_PERMISSION = 1
const val CAMERA_PERMISSION = Manifest.permission.CAMERA
const val STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
const val CAMERA_FACING = CameraCharacteristics.LENS_FACING_FRONT;

