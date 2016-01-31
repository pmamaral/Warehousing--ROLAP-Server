package logic;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SQLBuilder {

	public List<String> select, groupby, having ;
	public Set<String> from, where;
	
	public SQLBuilder()
	{
		select = new LinkedList<String>();
		from = new HashSet<String>();
		where = new HashSet<String>();
		groupby = new LinkedList<String>();
		having = new LinkedList<String>();
	}
	
	
	public String produceStatement()
	{
		String SELECT = "SELECT",
				FROM = " FROM",
				WHERE = " WHERE",
				GROUPBY = " GROUP BY",
				HAVING = " HAVING";
		
		for(int i = 0; i<select.size(); i++)
		{
			SELECT += " "+select.get(i);
			if( (select.size()-1) != i ) 
				SELECT+=",";
		}

		Iterator<String> it = from.iterator();
		for(int i = 0; it.hasNext(); i++)
		{
			FROM += " "+it.next();
			if( (from.size()-1) != i ) 
				FROM += ",";
		}
			
		it = where.iterator();
		for(int i = 0; i<where.size(); i++)
		{
			WHERE += " "+it.next();
			if( (where.size()-1) != i ) 
				WHERE+=" and ";
		}
				
		for(int i = 0; i<groupby.size(); i++)
		{
			GROUPBY += " " + groupby.get(i);
			if( (groupby.size()-1) != i ) 
				GROUPBY+=",";
		}
		
		for(int i = 0; i<having.size(); i++)
		{
			HAVING += " " + having.get(i);
			if( ( having.size() - 1 ) != i ) 
				HAVING += " and ";
		}
				
				
		return SELECT + "\n" +  FROM + "\n" +  (where.size() > 0 ? WHERE+ "\n" : "")  +  (groupby.size() > 0 ? GROUPBY+ "\n" : "") + (having.size() > 0 ?  HAVING : "");
				
		
	}
	
	
}
