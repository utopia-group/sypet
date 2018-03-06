import org.joda.time.DateTime;

public class Solution {

    public static boolean isLeapYear(int arg0) {
        DateTime v1 = DateTime.now();
        DateTime v2 = v1.withWeekyear(arg0);
        DateTime.Property v3 = v2.year();
        boolean v4 = v3.isLeap();
        return v4;
    }

}
