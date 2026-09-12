package com.rtnp.demo;

import com.rtnp.demo.core.PlacedObject;
import com.rtnp.demo.trait.TargetFilter;
import com.rtnp.demo.logic.BuildManager;

import java.util.*;

public class CombatManager {
    private final UnitSystem host;

    public CombatManager(UnitSystem host) {
        this.host = host;
    }

    public boolean hasMovingUnits() {
        for (PlacedObject u : host.placedObjects) if (u.isMoving) return true;
        return !host.bullets.isEmpty() || !host.missileSystem.getEffects().isEmpty();
    }

    public void applyExplosionDamage(PlacedObject missile) {
        List<PlacedObject> nearby = new ArrayList<>();
        host.spatialGrid.queryCircle(
            missile.targetX, missile.targetY,
            missile.explosionRange, nearby
        );
        for (PlacedObject t : nearby) {
            if (t.faction == missile.faction) continue;
            host.applyDamage(t, missile.missileDamage);
        }
    }

    public void checkAndStartServiceForBase(PlacedObject base) {
    }

    public void applyDamage(PlacedObject t, float dmg) {
    // 建造中单位：扣除建造进度而非血量
        if ("建造中单位".equals(t.category)) {
            BuildManager.INSTANCE.damageBuildProcess(t, dmg);
            return;
        }

        Float hp = host.healthMap.get(t);
        if (hp != null) {
            float nh = hp - dmg;
            host.healthMap.put(t, nh);
            t.health = (int) nh;
        }
    }

    public boolean checkMissileMinRange(PlacedObject a, PlacedObject t) {
        float dx = t.worldX - a.worldX;
        float dy = t.worldY - a.worldY;
        return (dx * dx + dy * dy) >= a.missileMinRange * a.missileMinRange;
    }

    /**
     * 单位级别索敌（不含武器槽过滤）
     * 返回范围内最近的敌方单位，不做任何过滤
     */
    public PlacedObject findTarget(PlacedObject att, float range) {
        // 中立单位不攻击任何人
        if (att.faction == 0) {
            att.currentTarget = null;
            return null;
        }

        // 1. 检查当前锁定的目标是否仍然有效
        PlacedObject locked = att.currentTarget;
        if (locked != null) {
            if (locked.faction == 0) {
                att.currentTarget = null;
            } else {
                Float hp = host.healthMap.get(locked);
                if (hp != null && hp > 0) {
                    float dx = locked.worldX - att.worldX;
                    float dy = locked.worldY - att.worldY;
                    if (dx * dx + dy * dy <= range * range) {
                        return locked;
                    }
                }
                att.currentTarget = null;
            }
        }

        // 2. 寻找最近的敌方单位（不做任何过滤）
        PlacedObject best = null;
        float bestDist2 = Float.MAX_VALUE;
        float range2 = range * range;

        for (PlacedObject t : host.placedObjects) {
            if (t.faction == att.faction) continue;
            if (t.faction == 0) continue;

            Float hp = host.healthMap.get(t);
            if (hp == null || hp <= 0) continue;

            float dx = t.worldX - att.worldX;
            float dy = t.worldY - att.worldY;
            float dist2 = dx * dx + dy * dy;
            if (dist2 <= range2 && dist2 < bestDist2) {
                bestDist2 = dist2;
                best = t;
            }
        }

        att.currentTarget = best;
        return best;
    }

    /**
     * 武器槽级别索敌：跳过被该武器槽特性过滤的目标
     * 这是所有武器类型的统一过滤入口
     * 
     * @param att   攻击者
     * @param ws    武器槽（用于检查过滤特性和白名单）
     * @param range 射程
     * @return 最近的有效目标，如果全部被过滤则返回 null
     */
    public PlacedObject findTargetForWeaponSlot(PlacedObject att, PlacedObject.WeaponSlot ws, float range) {
        // 中立单位不攻击任何人
        if (att.faction == 0) {
            ws.lockedTarget = null;
            return null;
        }

        // 1. 检查当前锁定的目标是否仍然有效
        PlacedObject locked = ws.lockedTarget;
        if (locked != null) {
            // 检查目标是否已被移除或死亡
            if (!host.placedObjects.contains(locked)) {
                ws.lockedTarget = null;
            } else if (locked.faction == 0 || locked.faction == att.faction) {
                ws.lockedTarget = null;
            } else {
                Float hp = host.healthMap.get(locked);
                if (hp != null && hp > 0) {
                    float dx = locked.worldX - att.worldX;
                    float dy = locked.worldY - att.worldY;
                    if (dx * dx + dy * dy <= range * range) {
                        // ★ 统一过滤入口：检查该武器槽是否要过滤此目标
                        if (!TargetFilter.shouldFilter(ws, locked)) {
                            return locked;
                        }
                    }
                }
                ws.lockedTarget = null;
            }
        }

        // 2. 寻找最近的未被过滤的敌方单位
        PlacedObject best = null;
        float bestDist2 = Float.MAX_VALUE;
        float range2 = range * range;

        for (PlacedObject t : host.placedObjects) {
            // 不攻击同阵营
            if (t.faction == att.faction) continue;
            // 不攻击中立
            if (t.faction == 0) continue;

            // ★ 统一过滤入口：索敌时就跳过被过滤的目标
            if (TargetFilter.shouldFilter(ws, t)) continue;

            // 检查目标是否存活
            Float hp = host.healthMap.get(t);
            if (hp == null || hp <= 0) continue;

            float dx = t.worldX - att.worldX;
            float dy = t.worldY - att.worldY;
            float dist2 = dx * dx + dy * dy;
            if (dist2 <= range2 && dist2 < bestDist2) {
                bestDist2 = dist2;
                best = t;
            }
        }

        ws.lockedTarget = best;
        return best;
    }
}