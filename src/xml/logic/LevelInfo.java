package logic;

public class LevelInfo 
{
	public String levelID, attrID, snowflakeType, snowflakedName;
	public Boolean isSnowflaked;
	
	public LevelInfo(String levelID, String attrID, String snowflakedName, Boolean isSnowflaked, String snowflakeType) 
	{
		this.levelID =  levelID;
		this.attrID =  attrID;
		this.snowflakeType = snowflakeType;
		this.snowflakedName = snowflakedName;
		this.isSnowflaked = isSnowflaked;
	}
	


}
