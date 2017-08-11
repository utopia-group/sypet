import org.joda.time.Days;
import org.joda.time.LocalDate;

public class Solution {

    public static int daysUtilNow(LocalDate arg0) {
        LocalDate v1 = new LocalDate();
        Days v2 = Days.daysBetween(arg0, v1);
        int v3 = v2.getDays();
        return v3;
    }

}
