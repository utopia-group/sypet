public static boolean test0() throws Throwable {
	return (isLeapYear(2000) == true);
}

public static boolean test1() throws Throwable {
	return (isLeapYear(1900) == false);
}

public static boolean test2() throws Throwable {
	return (isLeapYear(2011) == false);
}

public static boolean test() throws Throwable {
	return test0() && test1() && test2();
}