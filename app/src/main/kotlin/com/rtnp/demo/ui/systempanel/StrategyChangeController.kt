package com.rtnp.demo.ui.systempanel

import android.graphics.Color
import com.rtnp.demo.UnitSystem
import com.rtnp.demo.strategy.StrategyManager
import com.rtnp.demo.ui.systempanel.tools.SystemPanelChangeTool

object StrategyChangeController {

    fun show(
        unitSystem: UnitSystem?,
        slotIndex: Int,
        startX: Float,
        startY: Float,
        onFinished: () -> Unit
    ) {
        val unit = unitSystem?.selectedUnit
        if (unit == null) {
            onFinished()
            return
        }

        val hexItems = mutableListOf<SystemPanelChangeTool.ChangeHexItem>()
        hexItems.add(
            SystemPanelChangeTool.ChangeHexItem(
                backgroundColor = Color.rgb(255, 165, 0),
                label = "返回"
            )
        )

        val strategyIds = StrategyManager.getAllIds().filter { id ->
            StrategyManager.getTemplate(id)?.hiddenFromPlayer != true
        }
        for (strategyId in strategyIds) {
            val template = StrategyManager.getTemplate(strategyId) ?: continue
            val displayName = template.displayName

            if (template.iconPath != null) {
                hexItems.add(
                    SystemPanelChangeTool.ChangeHexItem(
                        backgroundColor = Color.rgb(190, 160, 255),
                        label = "",
                        iconPath = template.iconPath
                    )
                )
            } else {
                hexItems.add(
                    SystemPanelChangeTool.ChangeHexItem(
                        backgroundColor = Color.rgb(190, 160, 255),
                        label = displayName,
                        iconPath = null
                    )
                )
            }
        }

        SystemPanelChangeTool.show(
            startX = startX,
            startY = startY,
            topText = "",
            hexItems = hexItems,
            listener = object : SystemPanelChangeTool.ChangeToolListener {
                private var selectedStrategyIndex = -1

                override fun onHexSelected(index: Int, centerX: Float, centerY: Float) {
                    if (index == 0) {
                        SystemPanelChangeTool.hide()
                        onFinished()
                    } else {
                        selectedStrategyIndex = index - 1
                        val strategyId = strategyIds.getOrNull(selectedStrategyIndex)
                        val template = strategyId?.let { StrategyManager.getTemplate(it) }
                        if (template != null) {
                            // ★ 顶部信息框第一行显示名称，然后是简介
                            val infoText = "${template.displayName}\n${template.description}"
                            SystemPanelChangeTool.setTopText(infoText)
                        }
                        SystemPanelChangeTool.showBottomConfirm()
                    }
                }

                override fun onBottomButtonClicked(buttonType: SystemPanelChangeTool.BottomButtonType) {
                    when (buttonType) {
                        SystemPanelChangeTool.BottomButtonType.CANCEL -> {
                            SystemPanelChangeTool.hideBottomConfirm()
                            SystemPanelChangeTool.resetSelection()
                        }
                        SystemPanelChangeTool.BottomButtonType.CONFIRM -> {
                            if (selectedStrategyIndex >= 0 && selectedStrategyIndex < strategyIds.size) {
                                val strategyId = strategyIds[selectedStrategyIndex]
                                while (unit.strategyIds.size <= slotIndex) {
                                    unit.strategyIds.add("")
                                }
                                unit.strategyIds[slotIndex] = strategyId
                            }
                            SystemPanelChangeTool.hide()
                            onFinished()
                        }
                    }
                }
            }
        )
    }
}