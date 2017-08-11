public static boolean test() throws Throwable {
    java.lang.String xmlStr = "<?xml version=\"1.0\"?><!DOCTYPE note [<!ELEMENT note (to,from,heading,body)><!ELEMENT body (#PCDATA)>]><note><heading>Reminder</heading><body>Don't forget me this weekend</body></note>";
    org.w3c.dom.DocumentType elem = Source.getDoctypeByString(xmlStr);
    boolean flag = (elem != null) && (elem.getName().equals("note"));

    if(flag) 
        return true;
    else 
        return false;
}

