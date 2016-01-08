package common.enums;

public enum EventEnum {
	HUURSTATUSBUTTONEVENT("huurstatusButtonEvent"),
	CUSTOMEROVERVIEWBUTTONEVENT("customerOverviewButtonEvent"),
	RENTBUTTON1("rentButtonEvent1"),
	RENTBUTTON2("rentButtonEvent2"),
	ITEMMANAGEMENT("itemManagement"),
	YELLOWBUTTONEVENT("yellowButtonEvent"),
	REDBUTTONEVENT("redButtonEvent"), 
	ADDITEMBUTTONEVENT("addItemButtonEvent");
	
	
	private String actionCommand;
	
	private EventEnum(String actionCommand){
		this.actionCommand = actionCommand;
	}
	
	public String getActionCommand(){
		return actionCommand;
	}
	
	public String toString(){
		return actionCommand;
	}
}