import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Solution {

    public static String getAttributeById(File arg0, String arg1) throws Throwable {
        DocumentBuilderFactory v1 = DocumentBuilderFactory.newInstance();
        DocumentBuilder v2 = v1.newDocumentBuilder();
        Document v3 = v2.parse(arg0);
        Element v4 = v3.getDocumentElement();
        String v5 = v4.getAttribute(arg1);
        return v5;
    }

}
