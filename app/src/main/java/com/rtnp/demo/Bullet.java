package com.rtnp.demo;

public class Bullet {
    public float x, y, vx, vy;
    public int color, damage, faction;
    public float maxRange, startX, startY;

    public Bullet(float x, float y, float vx, float vy, int col, int dmg, int fac, float range, float sx, float sy) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        color = col; damage = dmg; faction = fac; maxRange = range; startX = sx; startY = sy;
    }
}
