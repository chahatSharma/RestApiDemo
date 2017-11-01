package stringrelated;

import java.util.HashMap;
import java.util.Set;

public class FindDuplicates {
	
	public static void findDuplicates(String s)
	{
		s= s.replace("//s+", "");
		char[] charArr= s.toCharArray();
		HashMap<Character, Integer> dump = new HashMap<>();
		if(s.length()>0)
		{
			for(int i=0;i<charArr.length;i++)
			{
				if(dump.containsKey(charArr[i]))
				{
					dump.put(charArr[i], dump.get(charArr[i])+1);
				}
				else
				{
					dump.put(charArr[i], 1);
				}
			}
		}
		
		
		Set<Character> set = dump.keySet();
		for(Character c : set)
		{
			System.out.println("The occurrence of character" + c + " is :" +  dump.get(c) + " times");
		}
	}
	
	public static void main(String[] args)
	{
		findDuplicates("I love you Angad so much");
	
	}

}
