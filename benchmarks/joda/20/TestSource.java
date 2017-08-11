public static boolean test0() throws Throwable {		
	return (daysOfMonth("2012/02", "yyyy/MM") == 29);	
}
	
public static boolean test1() throws Throwable {
	return (daysOfMonth("2014/03", "yyyy/MM") == 31);					
}
	

public static boolean test() throws Throwable {
		
    return test0() && test1();

} 
