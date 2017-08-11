import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;

public class Solution {

    public static int daysBetween(DateTime arg0, DateTime arg1) {
        LocalDate v1 = arg1.toLocalDate();
        LocalDate v2 = arg0.toLocalDate();
        Days v3 = Days.daysBetween(v2, v1);
        int v4 = v3.getDays();
        return v4;
    }

}
