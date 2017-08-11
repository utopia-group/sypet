import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class Solution {

    public static Element stringToElement(String arg0) throws Throwable {
        DocumentBuilderFactory v1 = DocumentBuilderFactory.newInstance();
        DocumentBuilder v2 = v1.newDocumentBuilder();
        StringReader v3 = new StringReader(arg0);
        InputSource v4 = new InputSource(v3);
        Document v5 = v2.parse(v4);
        Element v6 = v5.getDocumentElement();
        return v6;
    }

}
