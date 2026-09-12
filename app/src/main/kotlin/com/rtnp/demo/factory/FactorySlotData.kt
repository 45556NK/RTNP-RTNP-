package com.rtnp.demo.factory

/**
 * 工厂槽参数容器。
 * 只提供基础的查找与修改服务。
 */
class FactorySlotData {
    private val params = mutableMapOf<String, Any>()

    fun put(key: String, value: Any) {
        params[key] = value
    }

    fun putAll(map: Map<String, Any>) {
        params.putAll(map)
    }

    fun getInt(key: String): Int? = params[key] as? Int
    fun getFloat(key: String): Float? = params[key] as? Float
    fun getString(key: String): String? = params[key] as? String
    fun getBoolean(key: String): Boolean? = params[key] as? Boolean

    fun hasKey(key: String): Boolean = params.containsKey(key)
    fun getAll(): Map<String, Any> = params.toMap()

    fun copy(): FactorySlotData {
        val newData = FactorySlotData()
        newData.params.putAll(params)
        return newData
    }
}