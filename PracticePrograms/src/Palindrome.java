
public class Palindrome {
	
	public static boolean isPalindrome(String s)
	{
		s = s.replace("[^a-zA-Z]+", "");//remping punctuation marks
		s=s.replaceAll("\\s+", "");//replacing spaces
		int len = s.length();
		System.out.println(s);
		for(int i=0;i < len/2; i++){
			System.out.println(s.toLowerCase().charAt(i));
			System.out.println(s.toLowerCase().charAt(len-i-1));
			if(s.toLowerCase().charAt(i) != s.toLowerCase().charAt(len-1-i))
			{
				return false;
			}
		}
		return true;
	}
	
	public static void main(String[] args)
	{
		System.out.println(isPalindrome("Was it a car or a cat I saw"));
		
	}

}
