package logic;

public class HeaderInfo 
{
	public String type, id, name;
	public boolean isFact;
	public HeaderInfo(String type, boolean isFact, String id, String name) 
	{
		this.isFact = isFact;
		this.type = type;
		this.id = id;
		this.name = name;
	}

}
