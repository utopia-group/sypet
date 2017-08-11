import java.io.File;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

public class Solution {

    public static Object evaluateByXpath(File arg0, String arg1, QName arg2) throws Throwable {
        XPathFactory v1 = XPathFactory.newInstance();
        DocumentBuilderFactory v2 = DocumentBuilderFactory.newInstance();
        XPath v3 = v1.newXPath();
        DocumentBuilder v4 = v2.newDocumentBuilder();
        Document v5 = v4.parse(arg0);
        Object v6 = v3.evaluate(arg1, v5, arg2);
        return v6;
    }

}
