package com.rtnp.demo.movement;

import com.rtnp.demo.movement.weapon.WeaponManager;
import com.rtnp.demo.movement.weapon.WeaponTemplate;
import com.rtnp.demo.movement.ModuleManager;

import com.rtnp.demo.movement.MissileSystem;
import com.rtnp.demo.core.PlacedObject;

import java.util.*;

public class ModuleManager {

    // ========== 武器槽位工厂 ==========

    public static PlacedObject.WeaponSlot createSlotFromTemplate(String templateId) {
        WeaponTemplate t = WeaponManager.INSTANCE.getTemplate(templateId);
        if (t == null) return null;

        PlacedObject.WeaponSlot slot = new PlacedObject.WeaponSlot();
        slot.type = t.category;
        slot.range = t.range;
        slot.cooldown = t.cooldown;
        slot.cooldownRemaining = t.cooldown;
        slot.damage = t.damage;
        slot.bulletSpeed = t.bulletSpeed;
        slot.chargeTime = t.chargeTime;
        slot.minDamage = t.minDamage;
        slot.maxDamage = t.maxDamage;
        slot.minRange = t.minRange;
        slot.laserCharge = 0f;
        slot.laserTarget = null;
        slot.active = t.active;
        slot.templateId = templateId;
        slot.maxTargets = t.maxTargets;
        slot.energyDuration = t.energyDuration;
        slot.energyRegenDelay = t.energyRegenDelay;
        slot.energyRegenSpeed = t.energyRegenSpeed;
        slot.missileTypeName = t.missileTypeName;
        
        // ★ 复制武器特性
        if (t.traitIds != null) {
            slot.traitIds.addAll(t.traitIds);
        }
        
        // ★ 复制过滤白名单
        if (t.filterWhitelist != null) {
            for (Map.Entry<String, List<String>> entry : t.filterWhitelist.entrySet()) {
                slot.filterWhitelist.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }
        
        return slot;
    }

    @Deprecated
    public static PlacedObject.WeaponSlot createRailgunSlot(float damage, float bulletSpeed, float range, float cooldown) {
        PlacedObject.WeaponSlot slot = new PlacedObject.WeaponSlot();
        slot.type = "railgun";
        slot.damage = damage;
        slot.bulletSpeed = bulletSpeed;
        slot.range = range;
        slot.cooldown = cooldown;
        slot.cooldownRemaining = cooldown;
        slot.active = true;
        return slot;
    }

    @Deprecated
    public static PlacedObject.WeaponSlot createLaserSlot(float minDamage, float maxDamage, float chargeTime, float range) {
        PlacedObject.WeaponSlot slot = new PlacedObject.WeaponSlot();
        slot.type = "laser";
        slot.minDamage = minDamage;
        slot.maxDamage = maxDamage;
        slot.chargeTime = chargeTime;
        slot.range = range;
        slot.cooldown = 0f;
        slot.laserCharge = 0f;
        slot.active = true;
        return slot;
    }

    @Deprecated
    public static PlacedObject.WeaponSlot createMissileSlot(float range, float cooldown) {
        PlacedObject.WeaponSlot slot = new PlacedObject.WeaponSlot();
        slot.type = "missile";
        slot.range = range;
        slot.cooldown = cooldown;
        slot.cooldownRemaining = cooldown;
        slot.active = true;
        return slot;
    }

    // ========== 旧版模块（保留兼容） ==========

    public static class Module {
        public String type;
        public float range;
        public float damage;
        public float cooldown;
        public float chargeTime;
        public float minDamage;
        public float maxDamage;
        public float missileCooldown;
        public float missileMinRange;
        public float bulletSpeed;

        public BulletData execute(PlacedObject attacker, PlacedObject target, float deltaTime, MissileSystem missileSystem) {
            if (!attacker.weaponActive) return null;
            switch (type) {
                case "laser": return executeLaser(attacker, target, deltaTime);
                case "railgun": return executeRailgun(attacker, target, deltaTime);
                case "missile": return executeMissile(attacker, target, deltaTime, missileSystem);
                default: return null;
            }
        }

        private BulletData executeLaser(PlacedObject attacker, PlacedObject target, float deltaTime) {
            if (attacker.laserTarget == target) {
                attacker.laserCharge += deltaTime;
                if (attacker.laserCharge > chargeTime) attacker.laserCharge = chargeTime;
            } else {
                attacker.laserTarget = target;
                attacker.laserCharge = 0f;
            }
            float progress = Math.min(1f, attacker.laserCharge / chargeTime);
            float dps = minDamage + (maxDamage - minDamage) * progress;
            return new BulletData(0, 0, 0, 0, attacker.color, dps, attacker.faction, 0, 0, 0, true);
        }

        private BulletData executeRailgun(PlacedObject attacker, PlacedObject target, float deltaTime) {
            float dx = target.worldX - attacker.worldX;
            float dy = target.worldY - attacker.worldY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist <= 0) return null;
            float normX = dx / dist;
            float normY = dy / dist;
            return new BulletData(
                attacker.worldX, attacker.worldY,
                normX * bulletSpeed, normY * bulletSpeed,
                attacker.color, damage, attacker.faction, range,
                attacker.worldX, attacker.worldY, false
            );
        }

        private BulletData executeMissile(PlacedObject attacker, PlacedObject target, float deltaTime, MissileSystem missileSystem) {
            attacker.missileCooldown += deltaTime;
            if (attacker.missileCooldown < attacker.missileCooldownMax) return null;
            float dx = target.worldX - attacker.worldX;
            float dy = target.worldY - attacker.worldY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < missileMinRange) return null;
            missileSystem.launchMissile(attacker, target);
            attacker.missileCooldown = 0f;
            return null;
        }
    }

    public static class BulletData {
        public float x, y, vx, vy;
        public int color;
        public float damage;
        public int faction;
        public float maxRange;
        public float startX, startY;
        public boolean isLaserDamage;

        public BulletData(float x, float y, float vx, float vy, int color, float damage, int faction,
                          float maxRange, float startX, float startY, boolean isLaser) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.color = color; this.damage = damage; this.faction = faction;
            this.maxRange = maxRange; this.startX = startX; this.startY = startY;
            this.isLaserDamage = isLaser;
        }
    }

    public static final Module LASER_MODULE = new Module();
    public static final Module RAILGUN_MODULE = new Module();
    public static final Module MISSILE_MODULE = new Module();

    static {
        LASER_MODULE.type = "laser";
        LASER_MODULE.range = 300;
        LASER_MODULE.chargeTime = 10;
        LASER_MODULE.minDamage = 50;
        LASER_MODULE.maxDamage = 160;

        RAILGUN_MODULE.type = "railgun";
        RAILGUN_MODULE.cooldown = 1.0f;
        RAILGUN_MODULE.bulletSpeed = 100;

        MISSILE_MODULE.type = "missile";
        MISSILE_MODULE.missileCooldown = 1.0f;
    }

    @Deprecated
    public static Module getModuleForUnit(PlacedObject unit) {
        if (unit == null) return null;
        Module module = null;
        switch (unit.weaponType) {
            case "laser": module = LASER_MODULE; break;
            case "railgun": module = RAILGUN_MODULE; break;
            case "missile": module = MISSILE_MODULE; break;
            default: return null;
        }
        module.range = unit.range;
        switch (unit.weaponType) {
            case "railgun":
                module.damage = unit.damage;
                module.bulletSpeed = unit.bulletSpeed;
                module.cooldown = 1.0f;
                break;
            case "laser":
                module.chargeTime = unit.chargeTime;
                module.minDamage = unit.minDamage;
                module.maxDamage = unit.maxDamage;
                break;
            case "missile":
                module.missileCooldown = unit.missileCooldownMax;
                module.missileMinRange = unit.missileMinRange;
                module.range = unit.range;
                break;
        }
        return module;
    }
}