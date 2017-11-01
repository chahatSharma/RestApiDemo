package stringrelated;

public class ReverseString {
	
	public static void main(String[] args)
	{
		System.out.println("the ouput is " + reverseAString("Angad is great"));
		System.out.println("the ouput is " + reverseWordsInString("THE SKY IS BLUE"));
		
		System.out.println(reverseRecusrion("Angad is great"));
	}
	
	private static String reverseAString(String s){
		char[] charArray =s.toCharArray();
		StringBuffer result = new StringBuffer();
		int size = charArray.length;
		for(int i= size -1; i >= 0; i-- ){
			result.append(charArray[i]);
		}
		
		return result.toString();
	}
	
	private static String reverseRecusrion(String s)
	{
		String rev = "";
		if(s.length()==1)
		{
			return s;
		}
		else{
			rev = rev+s.charAt(s.length()-1)+reverseRecusrion(s.substring(0,s.length()-1));
			return rev;
		}
		
	}
	
	//angad is great
	//taerg si dagna
	
	private static String reverseWordsInString(String s){
		String[] str = s.split(" ");
		StringBuffer result = new StringBuffer();
		
		for(int i=str.length -1; i>=0; i--){
			result.append(str[i]);
			result.append(" ");
		}
		
		return result.toString();
	}
	//Angad is great!
	//!great is Angad
	
	
	
	

}
