// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Objects;
import zombie.UsedFromLua;

@UsedFromLua
public final class PZCalendar {
    private final Calendar calendar;

    public static PZCalendar getInstance() {
        return new PZCalendar(Calendar.getInstance());
    }

    public PZCalendar(Calendar calendar) {
        Objects.requireNonNull(calendar);
        this.calendar = calendar;
    }

    public void set(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        this.calendar.set(year, month, dayOfMonth, hourOfDay, minute);
    }

    public void setTimeInMillis(long millis) {
        this.calendar.setTimeInMillis(millis);
    }

    public int get(int field) {
        return this.calendar.get(field);
    }

    public final Date getTime() {
        return this.calendar.getTime();
    }

    public long getTimeInMillis() {
        return this.calendar.getTimeInMillis();
    }

    public boolean isLeapYear(int year) {
        return ((GregorianCalendar)this.calendar).isLeapYear(year);
    }
}
