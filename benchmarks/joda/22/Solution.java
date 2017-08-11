import org.joda.time.DateTime;
import org.joda.time.Years;

public class Solution {

    public static int getAge(DateTime arg0) {
        DateTime v1 = DateTime.now();
        Years v2 = Years.yearsBetween(arg0, v1);
        int v3 = v2.getYears();
        return v3;
    }

}
