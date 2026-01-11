// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.statistics;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import zombie.AchievementManager;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;

public final class StatisticsManager {
    private final HashMap<String, Statistic> statisticHashMap = new HashMap<>();

    public static StatisticsManager getInstance() {
        return StatisticsManager.Holder.instance;
    }

    private StatisticsManager() {
    }

    public void incrementStatistic(StatisticType statisticType, StatisticCategory statisticCategory, String key, float amount) {
        this.statisticHashMap.computeIfAbsent(key, k -> new Statistic(key, statisticType, statisticCategory));
        Statistic statistic = this.statisticHashMap.get(key);
        statistic.incrementStatistic(amount);
        AchievementManager.getInstance().checkAchievementsOnStatisticChange(statistic);
    }

    public void setStatistic(StatisticType statisticType, StatisticCategory statisticCategory, String key, float amount) {
        this.statisticHashMap.computeIfAbsent(key, k -> new Statistic(key, statisticType, statisticCategory));
        Statistic statistic = this.statisticHashMap.get(key);
        statistic.setValue(amount);
        AchievementManager.getInstance().checkAchievementsOnStatisticChange(statistic);
    }

    public float getStatistic(String key) {
        return this.statisticHashMap.get(key).getValue();
    }

    public HashMap<String, Statistic> getStatistics() {
        return this.statisticHashMap;
    }

    public String getAllStatisticsDebug() {
        StringBuilder debug = new StringBuilder();

        for (Entry<String, Statistic> entry : this.statisticHashMap.entrySet()) {
            debug.append("\n  ").append(entry.getKey()).append(": ").append(entry.getValue().getValue());
        }

        return debug.toString();
    }

    public void load() {
        this.statisticHashMap.clear();
        AchievementManager.getInstance().reset();
        if (!Core.getInstance().isNoSave()) {
            File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("statistics.bin"));
            if (file.exists()) {
                try (
                    FileInputStream fileInputStream = new FileInputStream(file);
                    DataInputStream dataInputStream = new DataInputStream(fileInputStream);
                ) {
                    int size = dataInputStream.readInt();

                    for (int i = 0; i < size; i++) {
                        String name = dataInputStream.readUTF();
                        StatisticType type = StatisticType.valueOf(dataInputStream.readUTF());
                        StatisticCategory category = StatisticCategory.valueOf(dataInputStream.readUTF());
                        float value = dataInputStream.readFloat();
                        Statistic statistic = new Statistic(name, type, category);
                        statistic.setValue(value);
                        this.statisticHashMap.put(name, statistic);
                        AchievementManager.getInstance().checkAchievementsOnStatisticLoad(statistic);
                    }
                } catch (Exception var15) {
                    ExceptionLogger.logException(var15);
                }
            }
        }
    }

    public void save() {
        if (!Core.getInstance().isNoSave()) {
            File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("statistics.bin"));

            try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
            ) {
                dataOutputStream.writeInt(this.statisticHashMap.size());

                for (Statistic statistic : this.statisticHashMap.values()) {
                    dataOutputStream.writeUTF(statistic.getName());
                    dataOutputStream.writeUTF(statistic.getStatisticType().name());
                    dataOutputStream.writeUTF(statistic.getStatisticCategory().name());
                    dataOutputStream.writeFloat(statistic.getValue());
                }

                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    outputStream.write(byteArrayOutputStream.toByteArray());
                }
            } catch (Exception var13) {
                ExceptionLogger.logException(var13);
            }
        }
    }

    private static class Holder {
        private static final StatisticsManager instance = new StatisticsManager();
    }
}
