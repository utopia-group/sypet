public static boolean test() throws Throwable {
     java.lang.String html = " <html>\n"
                + "   <head>\n"
                + "     <title>An example HTMLDocument</title>\n"
                + "     <style type=\"text/css\">\n"
                + "       div { background-color: silver; }\n"
                + "       ul { color: red; }\n"
                + "     </style>\n"
                + "   </head>\n"
                + "   <body>\n"
                + "     <div id=\"BOX\">\n"
                + "       <p>Paragraph 1</p>\n"
                + "       <p>Paragraph 2</p>\n"
                + "     </div>\n"
                + "   </body>\n"
                + " </html>\n";
     
    java.io.Reader stringReader = new java.io.StringReader(html);
    javax.swing.text.html.HTMLEditorKit htmlKit = new javax.swing.text.html.HTMLEditorKit();
    javax.swing.text.html.HTMLDocument htmlDoc = (javax.swing.text.html.HTMLDocument) htmlKit.createDefaultDocument();
    htmlKit.read(stringReader, htmlDoc, 0);
     
    javax.swing.text.Element elem = Source.getParagraphElement(htmlDoc, 1);
    String head = elem.getName();

    if("head".equals(head))
        return true;
    else
        return false;
}
