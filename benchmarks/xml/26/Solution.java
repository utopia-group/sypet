import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.xml.sax.InputSource;

public class Solution {

    public static DocumentType getDoctypeByString(String arg0) throws Throwable {
        DocumentBuilderFactory v1 = DocumentBuilderFactory.newInstance();
        StringReader v2 = new StringReader(arg0);
        DocumentBuilder v3 = v1.newDocumentBuilder();
        InputSource v4 = new InputSource(v2);
        Document v5 = v3.parse(v4);
        DocumentType v6 = v5.getDoctype();
        return v6;
    }

}
