public static boolean test0() throws Throwable {		
    org.joda.time.DateTime birth = new org.joda.time.DateTime(1990, 11, 13, 2, 0);
    int age = getAge(birth);
	return (age == 26);	
}
	

public static boolean test1() throws Throwable {		
    org.joda.time.DateTime birth = new org.joda.time.DateTime(1980, 11, 13, 2, 0);
    int age = getAge(birth);
	return (age == 36);	
}

public static boolean test2() throws Throwable {		
    org.joda.time.DateTimeZone SH = org.joda.time.DateTimeZone.forID("Asia/Shanghai");
    org.joda.time.DateTime birth = new org.joda.time.DateTime(1980, 11, 13, 2, 0, SH);
    int age = getAge(birth);
	return (age == 35);	
}

public static boolean test() throws Throwable {
		
    return test0() && test1(); 

} 
