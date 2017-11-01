package Sorting;

import java.util.Arrays;

public class SelectionSort {

	public static void main(String[] args){
		int[] input = {64,25,12,22,11};
		System.out.println("the sorted array is" + Arrays.toString(sort(input)));
	}
	
	public static int[] sort(int[] input){
		int min =0;
		for(int j = 0 ; j <input.length-1; j++){
			
			min = j;
			for(int i=j+1; i< input.length ;i++)
			{
				if(input[min] > input[i])
				{
					min = i;
				}
				
				
			}
			
			int temp = input[min];
			input[min] = input[j];
			input[j]=temp;
			
			
		}
		return input;
	}
}
