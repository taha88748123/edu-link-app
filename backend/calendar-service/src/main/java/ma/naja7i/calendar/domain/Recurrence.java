package ma.naja7i.calendar.domain;

public enum Recurrence {
    /** Repeats every week on {@code dayOfWeek} (0=Mon ... 6=Sun). */
    WEEKLY,
    /** Single calendar day declared via {@code date}. */
    ONE_OFF
}
