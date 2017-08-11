public static boolean test() throws Throwable {
    org.w3c.dom.Document doc = readXML(new java.io.File("benchmarks/xml/28/popl.xml"));
    org.w3c.dom.Element e = (org.w3c.dom.Element) doc.getFirstChild();
    java.lang.String id = e.getAttribute("id");
    if(id.equals("popl")) 
        return true;
    else 
        return false;
}
