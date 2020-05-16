package de.develappers.facerecognition.utils

import android.Manifest
import android.hardware.camera2.CameraCharacteristics

const val VISITORS_GROUP_ID: String = "1"

const val LOGBOOK_EXTRA: String = "extra_logbook"
const val VISITOR_EXTRA: String = "extra_visitor"
const val VISITOR_FIRST_TIME: String = "extra_first_time_visitor"
const val CANDIDATES_EXTRA: String = "extra_candidates"

const val TYPE_PHOTO: String = "image_type_photo"
const val TYPE_SIGNATURE: String = "image_type_signature"

const val APP_MODE_REALTIME: String = "realtime"
const val APP_MODE_DATABASE: String = "database_testing"
const val APP_MODE: String = APP_MODE_REALTIME

const val CONFIDENCE_MATCH: Double = 0.9
const val CONFIDENCE_CANDIDATE: Double = 0.0

const val REQUEST_CAMERA_PERMISSION = 0
const val REQUEST_STORAGE_PERMISSION = 1
const val CAMERA_PERMISSION = Manifest.permission.CAMERA
const val STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
const val CAMERA_FACING = CameraCharacteristics.LENS_FACING_FRONT;

