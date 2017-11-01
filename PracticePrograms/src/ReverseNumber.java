
public class ReverseNumber {

	public static int reverse(int n){
		int rev=0;
		if(n ==0)
		{
			return n;
		}
		else {
			while (n !=0)
			{
				rev = rev*10 + n%10;
				n = n/10;
				
			}
			return rev;
		}
	}
	
	public static void main(String[] args)
	{
		System.out.println(reverse(5123));
	}
}

