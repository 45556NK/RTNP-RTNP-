package com.rtnp.demo.ui.panel.tool

import android.content.Context
import android.graphics.Color
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.image.ImageManager
import com.rtnp.demo.ui.panel.data.InteractiveButtonComponent
import com.rtnp.demo.ui.systempanel.SystemPanelEntry

object SystemButtonTool {

    private const val ICON_PATH = "images/ui/system_btn.png"

    fun create(vw: Int, context: Context, unitSystem: UnitSystem): InteractiveButtonComponent {
        val size = InteractiveButtonTool.calculateSize(vw)
        val btn = InteractiveButtonComponent(
            label = "",
            width = size,
            height = size,
            backgroundColor = Color.argb(25, 0, 0, 0),
            iconBitmap = ImageManager.getInstance(context).getBitmap(ICON_PATH),
            animLabel = "系统管理",
            onClick = null,   // 稍后设置
            onPressed = { i -> ButtonAnimTool.defaultOnPress(i) },
            onReleased = { i -> ButtonAnimTool.defaultOnRelease(i) },
            onCancelled = { i -> ButtonAnimTool.defaultOnCancel(i) }
        )

        // 设置点击回调：计算按钮屏幕坐标，调用新系统入口
        btn.onClick = {
            val panelRect = PanelBox.rect()
            val centerX = panelRect.centerX()
            val centerY = panelRect.top + btn.layoutY + btn.computedHeight / 2f
            SystemPanelEntry.show(centerX, centerY, unitSystem)
        }

        return btn
    }
}