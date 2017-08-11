import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class Solution {

    public static int getDayFromString(String arg0, String arg1) {
        DateTimeFormatter v1 = DateTimeFormat.forPattern(arg1);
        LocalDate v2 = LocalDate.parse(arg0, v1);
        int v3 = v2.getDayOfMonth();
        return v3;
    }

}
