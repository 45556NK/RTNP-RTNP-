// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/ActionButtonTool.kt
package com.rtnp.demo.ui.panel.tool

import android.content.Context
import android.graphics.Color
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent

object ActionButtonTool {

    private const val ICON_PATH = "images/ui/action_btn.png"

    fun create(vw: Int, context: Context, onClick: (() -> Unit)? = null): InteractiveButtonComponent {
        val size = InteractiveButtonTool.calculateSize(vw)
        return InteractiveButtonComponent(
            label = "",
            width = size,
            height = size,
            backgroundColor = Color.argb(25, 0, 0, 0),
            iconBitmap = ImageManager.getInstance(context).getBitmap(ICON_PATH),
            animLabel = "行动",
            onClick = onClick,
            onPressed = { i -> ButtonAnimTool.defaultOnPress(i) },
            onReleased = { i -> ButtonAnimTool.defaultOnRelease(i) },
            onCancelled = { i -> ButtonAnimTool.defaultOnCancel(i) }
        )
    }
}