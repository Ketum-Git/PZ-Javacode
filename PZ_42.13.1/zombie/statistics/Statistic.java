// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.statistics;

public class Statistic {
    private final String name;
    private final StatisticType statisticType;
    private final StatisticCategory statisticCategory;
    private float value;

    public Statistic(String name, float amount) {
        this.name = name;
        this.statisticType = StatisticType.Undefined;
        this.statisticCategory = StatisticCategory.Undefined;
        this.value = amount;
    }

    public Statistic(String name, StatisticType statisticType, StatisticCategory statisticCategory) {
        this.name = name;
        this.statisticType = statisticType;
        this.statisticCategory = statisticCategory;
        this.value = 0.0F;
    }

    public String getName() {
        return this.name;
    }

    public void incrementStatistic(float amount) {
        this.value += amount;
    }

    public float getValue() {
        return this.value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public StatisticType getStatisticType() {
        return this.statisticType;
    }

    public StatisticCategory getStatisticCategory() {
        return this.statisticCategory;
    }
}
