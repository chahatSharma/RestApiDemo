package Sorting;

import java.util.Arrays;

public class BubbleSort {

	public static void main(String[] args) {
		int[] input = { 5, 9, 4, 2, 8 };
		System.out.println("sorted array is" + Arrays.toString(sort(input)));
	}

	public static int[] sort(int[] inputArray) {
		for (int j = inputArray.length -1;j>=0; j--) {

			for(int i=0; i<inputArray.length-1;i++){
				if(inputArray[i]>inputArray[i+1]){
					int temp = inputArray[i +1];
					inputArray[i+1] = inputArray[i];
					inputArray[i]=temp;
				}
			}
		}
		return inputArray;
	}
}
