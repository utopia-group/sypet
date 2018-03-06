public static boolean test0() throws Throwable {
		
    org.joda.time.DateTimeZone PORTUGAL = org.joda.time.DateTimeZone.forID("Europe/Lisbon");
    org.joda.time.DateTime start = new org.joda.time.DateTime(2013, 9, 16, 5, 0, 0, PORTUGAL);
    org.joda.time.DateTime end = new org.joda.time.DateTime(2013, 10, 21, 13, 0, 0, PORTUGAL);
		
    if (Source.daysBetween(start, end) == 35)
	return true;
    else
	return false;
}


public static boolean test1() throws Throwable {

    org.joda.time.DateTimeZone BRAZIL = org.joda.time.DateTimeZone.forID("America/Sao_Paulo");
    org.joda.time.DateTimeZone PORTUGAL = org.joda.time.DateTimeZone.forID("Europe/Lisbon");
    org.joda.time.DateTime start = new org.joda.time.DateTime(2013, 10, 13, 23, 59, BRAZIL);
    org.joda.time.DateTime end = new org.joda.time.DateTime(2013, 10, 20, 3, 0, PORTUGAL);

		
    if (Source.daysBetween(start, end) == 7)
	return true;
    else
	return false;
}

public static boolean test2() throws Throwable {

    org.joda.time.DateTimeZone SH = org.joda.time.DateTimeZone.forID("Asia/Shanghai");
    org.joda.time.DateTimeZone CT = org.joda.time.DateTimeZone.forID("America/Chicago");

    org.joda.time.DateTime start = new org.joda.time.DateTime(2013, 11, 13, 10, 59, SH);
    org.joda.time.DateTime end = new org.joda.time.DateTime(2013, 11, 20, 5, 0, CT);
		
    if (Source.daysBetween(start, end) == 7)
	return true;
    else
	return false;
}

public static boolean test() throws Throwable {
    return test0() && test1() && test2();
}
