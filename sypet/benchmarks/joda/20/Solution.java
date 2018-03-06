import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Solution {

    public static int daysOfMonth(String arg0, String arg1) {
        DateTimeFormatter v1 = DateTimeFormat.forPattern(arg1);
        DateTime v2 = DateTime.parse(arg0, v1);
        DateTime.Property v3 = v2.dayOfMonth();
        int v4 = v3.getMaximumValue();
        return v4;
    }

}
