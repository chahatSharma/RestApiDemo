package stringrelated;

import java.util.Arrays;

public class RotateArray {
	static int[] output;
	public static void main(String[] args){
		int[] input = {1,2,3,4,5,6,7};
		int k =3;
		output = reverseArray(input);
		System.out.println(Arrays.toString(output));
		System.out.println(Arrays.toString(rotateArray(input, 3)));
	}
	//[1,2,3,4,5,6,7]
	//k=3
	//[5,6,7,1,2,3,4]
	private static int[] rotateArray(int[] nums,int k){
		int[] result =new int[nums.length];
		System.arraycopy(nums, 0, result, k, nums.length-k);
		System.arraycopy(nums, nums.length-k, result, 0, k);
		return result;
	}
	private static int[] reverseArray(int[] nums)
	{
		int[] result = new int[nums.length];
		
		for(int i=0;i<nums.length;i++)
		{
			
				result[nums.length-1-i]=nums[i];//4,3,2,1
				
			
		}
		//System.arraycopy(nums, 0, result, 0, k);
		return result;
	}

	
}
