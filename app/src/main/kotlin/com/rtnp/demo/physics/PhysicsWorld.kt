package com.rtnp.demo.physics

import com.rtnp.demo.core.PlacedObject
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import kotlin.math.abs

class PhysicsWorld {

    private val world: World
    private val bodyMap = mutableMapOf<PlacedObject, Body>()

    private val scale = 30f
    @JvmField
    var enabled: Boolean = false

    init {
        world = World(Vec2(0f, 0f))
        world.isAllowSleep = false
    }

    fun addUnit(unit: PlacedObject) {
        if (bodyMap.containsKey(unit)) return
        if (unit.category == "missile") return
        if (unit.physicsMass == null) return

        val body = createBody(unit)
        bodyMap[unit] = body
    }

    fun removeUnit(unit: PlacedObject) {
        val body = bodyMap.remove(unit) ?: return
        world.destroyBody(body)
    }

    fun update(deltaTime: Float) {
        if (!enabled) return
        val dt = deltaTime.coerceIn(0.001f, 0.05f)

        // 同步所有刚体位置和角度
        for ((unit, body) in bodyMap) {
            body.setTransform(
                Vec2(unit.worldX / scale, unit.worldY / scale),
                Math.toRadians(unit.heading.toDouble()).toFloat()
            )
            body.setLinearVelocity(Vec2(0f, 0f))
            body.setAngularVelocity(0f)
        }

        // 根据速度决定是否启用碰撞
        for ((unit, body) in bodyMap) {
            val fixture = body.fixtureList ?: continue
            val filter = Filter()
            filter.categoryBits = 0x0001
            filter.maskBits = if (unit.currentSpeed > 80f) 0x0000 else 0x0001
            fixture.filterData = filter
        }

        // 步进物理世界
        world.step(dt, 10, 5)

        // 将碰撞推开的位置写回游戏单位
        for ((unit, body) in bodyMap) {
            val physX = body.position.x * scale
            val physY = body.position.y * scale
            val dx = physX - unit.worldX
            val dy = physY - unit.worldY

            if (abs(dx) > 1f || abs(dy) > 1f) {
                val pushDist = kotlin.math.sqrt(dx * dx + dy * dy)

                if (pushDist > 5f && unit.isMoving) {
                    // 被卡住了：修正位置，停止移动，目标不变
                    unit.worldX = physX
                    unit.worldY = physY
                    unit.isMoving = false
                    unit.isStopping = true
                    unit.targetX = physX
                    unit.targetY = physY
                    unit.currentSpeed = 0f
                } else {
                    // 轻微推开：只修正位置
                    unit.worldX = physX
                    unit.worldY = physY
                }
            }
        }
    }

    fun clear() {
        for (body in bodyMap.values) {
            world.destroyBody(body)
        }
        bodyMap.clear()
    }

    private fun createBody(unit: PlacedObject): Body {
        val bodyDef = BodyDef()
        bodyDef.type = BodyType.DYNAMIC
        bodyDef.position.set(unit.worldX / scale, unit.worldY / scale)
        bodyDef.linearDamping = 0f
        bodyDef.angularDamping = 0f
        bodyDef.fixedRotation = true
        bodyDef.bullet = true
        bodyDef.gravityScale = 0f

        val body = world.createBody(bodyDef)
        body.userData = unit

        val fixtureDef = FixtureDef()
        fixtureDef.friction = 0f
        fixtureDef.restitution = 0.3f
        fixtureDef.filter.categoryBits = 0x0001
        fixtureDef.filter.maskBits = 0x0000

        val mass = unit.physicsMass ?: 0.1f
        fixtureDef.density = when {
            mass < 0 -> 999999f
            mass == 0f -> 0.01f
            else -> mass
        }

        when (unit.shape) {
            "circle" -> {
                val shape = CircleShape()
                shape.radius = (unit.size / 2f) / scale
                fixtureDef.shape = shape
                body.createFixture(fixtureDef)
            }
            "square" -> {
                val shape = PolygonShape()
                val half = (unit.size / 2f) / scale
                shape.setAsBox(half, half)
                fixtureDef.shape = shape
                body.createFixture(fixtureDef)
            }
            else -> {
                val shape = PolygonShape()
                val hw = (unit.width / 2f) / scale
                val hh = (unit.height / 2f) / scale
                shape.setAsBox(hw, hh)
                fixtureDef.shape = shape
                body.createFixture(fixtureDef)
            }
        }

        body.setTransform(
            body.position,
            Math.toRadians(unit.heading.toDouble()).toFloat()
        )

        return body
    }
}