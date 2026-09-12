// app/src/main/kotlin/com/rtnp/demo/ui/panel/tool/ExitButtonTool.kt
package com.rtnp.demo.ui.panel.tool

import android.content.Context
import android.graphics.Color
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent

object ExitButtonTool {

    private const val ICON_PATH = "images/ui/exit_icon.png"
    private const val PRESS_DURATION = 0.1f
    private const val RELEASE_DURATION = 0.1f
    private const val TARGET_ROTATION = 180f

    /**
     * 创建退出按钮
     */
    fun create(vw: Int, context: Context, onClick: (() -> Unit)? = null): InteractiveButtonComponent {
        val size = InteractiveButtonTool.calculateSize(vw)
        return InteractiveButtonComponent(
            label = "",
            width = size,
            height = size,
            backgroundColor = Color.argb(25, 0, 0, 0),
            iconBitmap = ImageManager.getInstance(context).getBitmap(ICON_PATH),
            animLabel = "退出",
            onClick = onClick,
            onPressed = { index ->
                ButtonAnimTool.onPress(index)
                ButtonAnimTool.setRotationTarget(index, TARGET_ROTATION)
            },
            onReleased = { index ->
                ButtonAnimTool.onRelease(index)
            },
            onCancelled = { index ->
                ButtonAnimTool.cancel(index)
            }
        )
    }
}