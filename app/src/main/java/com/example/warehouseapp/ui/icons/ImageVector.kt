package com.example.warehouseapp.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Если иконка Warehouse не доступна в вашей версии, используйте эту альтернативу:
val Icons.Filled.Warehouse: ImageVector
    get() {
        if (_warehouse != null) {
            return _warehouse!!
        }
        _warehouse = Builder(name = "Warehouse", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
            viewportWidth = 24.0f, viewportHeight = 24.0f).apply {
            path(fill = SolidColor(Color(0xFF000000)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero) {
                moveTo(12.0f, 2.0f)
                lineTo(2.0f, 7.0f)
                verticalLineToRelative(10.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                horizontalLineToRelative(1.0f)
                verticalLineToRelative(-7.0f)
                curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
                horizontalLineToRelative(14.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                verticalLineToRelative(7.0f)
                horizontalLineToRelative(1.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                lineTo(22.0f, 7.0f)
                lineTo(12.0f, 2.0f)
                close()
                moveTo(18.0f, 18.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-5.0f)
                horizontalLineToRelative(-2.0f)
                verticalLineToRelative(5.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-5.0f)
                lineTo(8.0f, 13.0f)
                verticalLineToRelative(5.0f)
                lineTo(6.0f, 18.0f)
                verticalLineToRelative(-6.0f)
                horizontalLineToRelative(12.0f)
                verticalLineToRelative(6.0f)
                close()
            }
        }
            .build()
        return _warehouse!!
    }

private var _warehouse: ImageVector? = null