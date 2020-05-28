package de.develappers.facerecognition.serviceAI.faceServiceAI.model

import java.io.Serializable

class CommonRect : Serializable {
    var top // 矩形框左上角像素点的纵坐标
            = 0
    var left //  矩形框左上角像素点的横坐标
            = 0
    var width // 矩形框的宽度
            = 0
    var height // 矩形框的高度
            = 0

    override fun toString(): String {
        return "{" +
                "\"top\":" + top +
                ", \"left\":" + left +
                ", \"width\":" + width +
                ", \"height\":" + height +
                '}'
    }
}