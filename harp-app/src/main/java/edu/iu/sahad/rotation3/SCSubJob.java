package edu.iu.sahad.rotation3;

public class SCSubJob {
	private String SubJobID;
    private String activeChild;
    private String passiveChild;
    public int referedNum;/*how many subjobs refer to this subjob; 
    					  For exmaple, if in a template: u3-1 u2 i, then u3-1 refers to u2 and i;
    					   so u2.referedNum++, i.referedNum++;
    					   Used to abolish tables which won't be used anymore.
    					*/
    public SCSubJob(){
    	
    }
    public SCSubJob(String id){
		SubJobID = id;
	}
    public String getSubJobID() {
		return SubJobID;
	}
	public void setSubJobID(String subJobID) {
		SubJobID = subJobID;
	}
	public String getActiveChild() {
		return activeChild;
	}
	public void setActiveChild(String activeChild) {
		this.activeChild = activeChild;
	}
	public String getPassiveChild() {
		return passiveChild;
	}
	public void setPassiveChild(String passiveChild) {
		this.passiveChild = passiveChild;
	}
	@Override
	public String toString() {
		return "SCSubJob [SubJobID=" + SubJobID + ", activeChild=" + activeChild + ", passiveChild=" + passiveChild
				+ ", referedNum=" + referedNum + "]";
	}
	
	
	
	
}
