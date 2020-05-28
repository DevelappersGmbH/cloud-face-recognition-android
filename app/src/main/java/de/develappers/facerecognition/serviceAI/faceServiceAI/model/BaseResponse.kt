package de.develappers.facerecognition.serviceAI.faceServiceAI.model

import java.io.Serializable

open class BaseResponse : Serializable {
    var time_used // 用时
            = 0
    var image_id // 传入的图片在系统中的标识。
            : String? = null
    var request_id // 请求唯一标识
            : String? = null
    var error_message // 错误信息
            : String? = null

    override fun toString(): String {
        return "{" +
                "\"time_used\":" + time_used +
                ", \"image_id\":\'" + image_id + "\'" +
                ", \"request_id\":\'" + request_id + "\'" +
                ", \"error_message\":\'" + error_message + "\'" +
                '}'
    }
}