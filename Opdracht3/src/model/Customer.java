/**
 * 
 */
package model;

import common.MagicStrings;

/**
 * A customer's details, including name, adress, e-mail and an unique ID.
 * 
 * @author Andre
 *
 */
public class Customer extends ModelBase{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6548433533448484653L;
	private Person person;
	private Adress adress;
	//private int id;
	private String email;
	private MagicStrings magicString;
	
	public Customer(Person person, Adress adress, String email) throws Exception{
		try{
			setPerson(person);
			setAdress(adress);
			setEmail(email);
		}
		catch (Exception e){
			throw e;
		}
	}
	
	/*
	public Customer(Person person, Adress adress, String email, int ID) throws Exception{
		try{
			Customer cust = new Customer(person, adress, email);
			super(ID);
		}
		catch (Exception e){
			throw e;
		}
	}
	*/
	
	public Person getPerson() {
		return person;
	}
	public void setPerson(Person person) {
		this.person = person;
	}
	public Adress getAdress() {
		return adress;
	}
	public void setAdress(Adress adress) {
		this.adress = adress;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
}