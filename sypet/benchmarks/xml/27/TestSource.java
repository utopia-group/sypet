public static boolean test() throws Throwable {
    java.lang.String xmlStr = "<MyXML id=\"pldi\">xml</MyXML>";
    org.w3c.dom.Element elem = Source.stringToElement(xmlStr);
    java.lang.String id = elem.getAttribute("id");
    if(id.equals("pldi")) 
        return true;
    else 
        return false;
}
