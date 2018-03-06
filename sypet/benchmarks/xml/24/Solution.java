import javax.swing.text.Document;
import javax.swing.text.Element;

public class Solution {

    public static Element getParagraphElement(Document arg0, int arg1) {
        Element v1 = arg0.getDefaultRootElement();
        int v2 = v1.getElementIndex(arg1);
        Element v3 = v1.getElement(v2);
        return v3;
    }

}
