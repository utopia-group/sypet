public static boolean test0() throws Throwable {
				
		if (getDayFromString("2015/10/21", "yyyy/MM/dd") == 21)
			return true;
		else
			return false;
	}
	
	public static boolean test1() throws Throwable {

		if (getDayFromString("2013/6/13", "yyyy/MM/dd") == 13)
			return true;
		else
			return false;
	}
	
	public static boolean test() throws Throwable {
		
		
		return test0() && test1();

	}
