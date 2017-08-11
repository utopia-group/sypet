import javax.swing.text.Document;
import javax.swing.text.Element;

public class Solution {

    public static int getOffsetForLine(Document arg0, int arg1) {
        Element v1 = arg0.getDefaultRootElement();
        Element v2 = v1.getElement(arg1);
        int v3 = v2.getStartOffset();
        return v3;
    }

}

