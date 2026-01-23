// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.util.HashMap;
import zombie.statistics.Achievement;
import zombie.statistics.Statistic;

public final class AchievementManager {
    private final HashMap<String, Achievement> achievementHashMap = new HashMap<>();

    public static AchievementManager getInstance() {
        return AchievementManager.Holder.instance;
    }

    private AchievementManager() {
        Achievement achievement = new Achievement("Distance Walked", "Walked %.2f units.", 10.0F);
        this.achievementHashMap.put(achievement.getName(), achievement);
        achievement = new Achievement("Distance Ran", "Ran %.2f units.", 10.0F);
        this.achievementHashMap.put(achievement.getName(), achievement);
        achievement = new Achievement("Distance Driven", "Drove %.2f units.", 10.0F);
        this.achievementHashMap.put(achievement.getName(), achievement);
        achievement = new Achievement("Shots Fired", "%.0f shots fired.", 10.0F);
        this.achievementHashMap.put(achievement.getName(), achievement);
    }

    public void checkAchievementsOnStatisticChange(Statistic statistic) {
        for (Achievement achievement : this.achievementHashMap.values()) {
            if (statistic.getName().equals(achievement.getName()) && !achievement.isUnlocked() && statistic.getValue() >= achievement.getThreshold()) {
                achievement.onAchieved();
                return;
            }
        }
    }

    public void checkAchievementsOnStatisticLoad(Statistic statistic) {
        for (Achievement achievement : this.achievementHashMap.values()) {
            if (statistic.getName().equals(achievement.getName()) && !achievement.isUnlocked() && statistic.getValue() >= achievement.getThreshold()) {
                achievement.onAchieved();
                return;
            }
        }
    }

    public HashMap<String, Achievement> getAchievements() {
        return this.achievementHashMap;
    }

    public void reset() {
        for (Achievement achievement : this.achievementHashMap.values()) {
            achievement.reset();
        }
    }

    private static class Holder {
        private static final AchievementManager instance = new AchievementManager();
    }
}
