// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

public class ExecuteTimeAnalyse {
    String caption;
    ExecuteTimeAnalyse.TimeStamp[] list;
    int listIndex;

    public ExecuteTimeAnalyse(String caption, int size) {
        this.caption = caption;
        this.list = new ExecuteTimeAnalyse.TimeStamp[size];

        for (int i = 0; i < size; i++) {
            this.list[i] = new ExecuteTimeAnalyse.TimeStamp();
        }
    }

    public void reset() {
        this.listIndex = 0;
    }

    public void add(String comment) {
        this.list[this.listIndex].time = System.nanoTime();
        this.list[this.listIndex].comment = comment;
        this.listIndex++;
    }

    public long getNanoTime() {
        return this.listIndex == 0 ? 0L : System.nanoTime() - this.list[0].time;
    }

    public int getMsTime() {
        return this.listIndex == 0 ? 0 : (int)((System.nanoTime() - this.list[0].time) / 1000000L);
    }

    public void print() {
        long startTime = this.list[0].time;
        System.out.println("---------- START --- " + this.caption + " -------------");

        for (int i = 1; i < this.listIndex; i++) {
            System.out.println(i + " " + this.list[i].comment + ": " + (this.list[i].time - startTime) / 1000000L);
            startTime = this.list[i].time;
        }

        System.out.println("END: " + (System.nanoTime() - this.list[0].time) / 1000000L);
        System.out.println("----------  END  --- " + this.caption + " -------------");
    }

    static class TimeStamp {
        long time;
        String comment;

        public TimeStamp(String comment) {
            this.comment = comment;
            this.time = System.nanoTime();
        }

        public TimeStamp() {
        }
    }
}
