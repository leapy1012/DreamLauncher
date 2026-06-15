package com.android.launcher3.touch

import android.graphics.PointF

class CustomizeSingleAxisSwipeDetector {
    companion object {
        val HORIZONTAL_RTL: SingleAxisSwipeDetector.Direction =
            object : SingleAxisSwipeDetector.Direction() {
                override fun isPositive(displacement: Float): Boolean {
                    return displacement < 0
                }

                override fun isNegative(displacement: Float): Boolean {
                    return displacement > 0
                }

                override fun extractDirection(direction: PointF): Float {
                    return direction.x
                }

                override fun extractOrthogonalDirection(direction: PointF): Float {
                    return direction.y
                }

                override fun toString(): String {
                    return "HORIZONTAL_RTL"
                }
            }
    }
}