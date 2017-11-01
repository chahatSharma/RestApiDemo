
public class Singleton {

	private static Singleton myobj;
	
	static Singleton getInstance(){
		if(myobj == null)
		myobj = new Singleton();
		
		return myobj;
	}
	private Singleton(){
		
	}
	
	
}
