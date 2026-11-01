package com.tradewind.model;

/** Mutable per-profession counters, owned by {@link PlayerData}. */
public final class ProfessionStats {

    private long actions;
    private double money;
    private long xp;
    private int level = 1;

    public long actions() {
        return actions;
    }

    public double money() {
        return money;
    }

    public long xp() {
        return xp;
    }

    public int level() {
        return level;
    }

    public void addAction() {
        actions++;
    }

    public void addMoney(double amount) {
        money += amount;
    }

    public void addXp(long amount) {
        xp += amount;
    }

    public void level(int level) {
        this.level = level;
    }

    public void restore(long actions, double money, long xp, int level) {
        this.actions = actions;
        this.money = money;
        this.xp = xp;
        this.level = Math.max(1, level);
    }
}

