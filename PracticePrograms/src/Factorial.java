
public class Factorial {
	
	public static int computeFactorial(int n)
	{
		if(n >=0)
		{
		int result =n;
		if(n ==1 || n ==0)
		{
			result = n;
		}
		else{
			result  = result * computeFactorial(n-1);
		}
		
		return result;
		}
		else
			return n;
	}
	
	public static void main(String[] args)
	{
		System.out.println(computeFactorial(5));
	}

}
