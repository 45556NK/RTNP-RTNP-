// app/src/main/kotlin/com/rtnp/demo/ui/panel/LayoutItem.kt
package com.rtnp.demo.ui.panel

sealed class LayoutItem {
    object HealthText : LayoutItem()
    object SpeedText : LayoutItem()
    object RemainingLifetimeText : LayoutItem()
    object StorageText : LayoutItem()
    object CollectedItemsText : LayoutItem()
    data class WeaponSlot(val index: Int) : LayoutItem()
    data class StrategySlot(val index: Int) : LayoutItem()
    object ActionButton : LayoutItem()
    object MiningToggle : LayoutItem()
    object SystemManagementButton : LayoutItem()
    object FactoryButton : LayoutItem()
    object DetailsButton : LayoutItem()
    object ExitButton : LayoutItem()
    data class CustomText(val key: String) : LayoutItem()
}