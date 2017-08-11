import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;

public class Solution {

    public static String getTitle(String arg0) throws Throwable {
        Connection v1 = HttpConnection.connect(arg0);
        Document v2 = v1.get();
        String v3 = v2.title();
        return v3;
    }

}
