import java.util.Arrays;
import java.util.HashSet;

public class LinkedList {
	static ListNode head;
	 
	private static ListNode removeDups(ListNode head)
	{
		if(head == null)return null;
		if(head.next == null)return head;
		
		HashSet<Integer> set = new HashSet<>();
		ListNode current = head;
		ListNode prev = null;
		while(current != null)
		{
			int curval = current.data;
			if(set.contains(curval))
			{
				prev.next = current.next;
			}
			else
			{
				set.add(curval);
				prev =current;
			}
			current = current.next;
		}
		return head;
		
	}
	
	public static ListNode reverseRecursion(ListNode curr, ListNode prev)
	{
		if(curr.next == null)
		{
			head = curr;
			curr.next = prev;
			return null;
		}
		ListNode next1 = curr.next;
		curr.next = prev;
		reverseRecursion(next1, curr);
		return head;
	}
	
	public static ListNode reverseList(ListNode head)
	{
		ListNode prev = null;
		ListNode current = head;
		ListNode next = null;
		while(current != null)
		{
			next = current.next;
			current.next = prev;
			prev = current;
			current = next;
			
		}
		head = prev;
		return head;
	}
	
	public static void main(String[] args){
		
		
		ListNode start = new ListNode(10);
		start.next = new ListNode(12);
		start.next.next = new ListNode(11);
		start.next.next.next = new ListNode(11);
		start.next.next.next.next = new ListNode(12);
		start.next.next.next.next.next = new ListNode(11);
		
		prinList(start);
		System.out.println("after");
		/*removeDups(start);
		prinList(start);*/
		
		//start = reverseList(start);
		//prinList(start);
		
		ListNode output = reverseRecursion(start, null);
		System.out.println("the recursive output is" + " /n" );
		prinList( output);
		
		
	}
	static void prinList(ListNode node)
	{
		while (node !=null)
		{
			System.out.println(node.data+ " ");
			node = node.next;
		}
	}

}
