package com.rtnp.demo.trait

import com.rtnp.demo.core.PlacedObject

abstract class TraitTemplate {

    abstract val id: String
    abstract val displayName: String
    abstract val description: String

    /** 宿主存活时每帧调用 */
    abstract fun onUpdate(host: PlacedObject, deltaTime: Float, tools: TraitTools)

    /** 宿主死亡后继续运行，直到返回 true 表示结束 */
    open fun onAfterDeath(host: PlacedObject, deltaTime: Float, tools: TraitTools): Boolean = true

    /** 是否需要后事处理 */
    open fun needsAfterDeath(): Boolean = false
    
    /** ★ 是否全局运行（宿主死亡后继续运行直到 onAfterDeath 返回 true） */
    open val isGlobal: Boolean = false
}