public static boolean test() throws Throwable {
    java.io.File file = new java.io.File("benchmarks/xml/29/doc.xml");
    java.lang.String query = "/html/body/div[@id='container']";
    javax.xml.namespace.QName qname = javax.xml.xpath.XPathConstants.NODE;
    java.lang.Object node = Source.evaluateByXpath(file, query, qname);
    boolean flag = (node != null) && (((org.w3c.dom.Node)node).getNodeName().equals("div"));

    if(flag) 
        return true;
    else 
        return false;
}

