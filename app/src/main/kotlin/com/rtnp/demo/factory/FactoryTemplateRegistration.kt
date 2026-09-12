package com.rtnp.demo.factory

import com.rtnp.demo.factory.templates.RepairFactoryTemplate
import com.rtnp.demo.factory.templates.ShipyardFactoryTemplate

object FactoryTemplateRegistration {

    fun registerFactoryTemplates() {
        FactoryManager.register(RepairFactoryTemplate())
        FactoryManager.register(ShipyardFactoryTemplate())
    }
}