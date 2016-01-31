package logic;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import db.FailedConnectionException;
import db.QueryFailedException;
import db.SQLModule;

import xml.client.Dimension;
import xml.client.Filter;
import xml.client.Query;
import xml.client.Slice;
import xml.client.SelectedFacts.Fact;
import xml.client.Slice.SliceCollumns.Collumn;
import xml.lov.ListOfValues;
import xml.meta.Databases.Database;
import xml.meta.Derived;
import xml.meta.Fact.AdditionDimensions;
import xml.meta.Fact.Aggregation;
import xml.meta.Level;
import xml.meta.LevelAttribute;
import xml.meta.MetaModel;
import xml.meta.ObjectFactory;
import xml.meta.Snowflaked;
import xml.meta.Table;
import xml.meta.Table.ForeignKeys.ForeignKey;
import xml.meta.TableAttribute;

import xml.server.Response;
import xml.server.Response.Headers;
import xml.server.Response.Headers.Head;
import xml.server.Response.RowSet;
import xml.server.Response.RowSet.Row;

public class MetaModelControl implements MetaModelHandler
{
	private MetaModel model; 
	private final String DB_PATH = "metafile";
	
	public MetaModelControl()
	{
		File f = new File( DB_PATH);
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
			model = (MetaModel) ois.readObject();
			ois.close();
		}
		catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			try {
				f.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} 
		
		
	}
	
	@Override
	public Response doQuery(Query query) throws QueryFailedException, FailedConnectionException 
	{
		//< SQL , List < TableAttibute, 
		Pair<String,List<HeaderInfo>> marshalled = marshall(query);
		
		String sqlStatement = marshalled.one;
		List<HeaderInfo> headers = marshalled.two;
		
		Database db = getDatabase(query.getCube().getCubeID());
		
		String driver = db.getJdbcDriver(),
				url= db.getUrl(),
				username= db.getUsername(),
				password= db.getPassword();
		System.out.println(sqlStatement);
		
		ResultSet resp = new SQLModule(driver, url, username, password).executeQuery(sqlStatement);
		return toResult(resp,headers);

	}

	SQLBuilder builder;
	public Pair<String, List<HeaderInfo>> marshall(Query query) 
	{
		LinkedList<HeaderInfo> headers = new LinkedList<HeaderInfo>();
		builder = new SQLBuilder();
		
		//get cube meta
		Table factTable = getTable(query.getCube().getCubeID());
		xml.meta.Cube thisCube = getCube(query.getCube().getCubeID());
		//set cube in from
		builder.from.add(factTable.getName());
		
		
		for(Dimension d : query.getDimensions().getDimension()) //for each selected dimension
		{
			//get dimension meta
			xml.meta.Dimension metaDim = getDimension(d.getDimID()); //O(#dimensoes)
			Table current = getTable(metaDim.getTableID()); //O(#tabelas)
			String name = current.getName();
			
			//add to from and add join clause to where
			builder.from.add(name);
			builder.where.add(
					name + "." + getPrimaryKey(current)
					+ " = " + 
					factTable.getName() + "." + getForeignKey( factTable, current.getTableID() )
			);

			//for each selected level convert levels to attributes
			//List < level, attr, display > in the order of the parameter 1 selected collumns
			List<LevelInfo> collumns = levelToAttribute( d.getSelectedCollumns().getSelectedCollumn(), metaDim ,current); 
			//for each attribute id convert search for it's correspondent table attribute info
			//TableAttribute[] in the order of the list given by parameter and with the same size
			TableAttribute [] collumnAttrs = toTableAttributes(collumns,current);
			 
			//for each selected level and corresponding information (table attribute and Triple<attrID,levelID,isSnowflaked>)
			Iterator<LevelInfo> it1 = collumns.iterator();
			for(TableAttribute ta : collumnAttrs) //add each attribute to the query's select and groupby clause 
			{
				LevelInfo info = it1.next();
				
				String attr;
				
				if(info.isSnowflaked) //isSnowflaked
					attr = info.snowflakedName;
				else
					attr = name + "." + ta.getName();
				HeaderInfo head = new HeaderInfo( ta.getType(),false, info.levelID + "/" + d.getDimID() , attr);
				
				headers.addLast( head );
				//add selected attribute to the select and groupby clauses
				builder.select.add(attr);
				builder.groupby.add(attr);
			}
			
			
			Slice slice = d.getSlice();
			if(slice != null)
			{
				//covert the slice collumns level identifiers to table attributes
				Iterator<Collumn> it = slice.getSliceCollumns().getCollumn().iterator();
				
				// Pair<TableAttribute[], isSnowflaked[]>
				Pair<TableAttribute[],Boolean[]> attrSnow = toTableAttributes(current,slice.getSliceCollumns().getCollumn(),metaDim,current);
				TableAttribute [] trueCollumns = attrSnow.one;
				Boolean [] isSnowflaked = attrSnow.two;
				for(int i = 0; it.hasNext(); i++) //por cada slice attribute
				{
					String whereResult = "( ";
					//construir where clause
					List<String> next = it.next().getByValues().getValue();
					Iterator<String> nextIt = next.iterator();
					
					String attrName; 
					if(!isSnowflaked[i]) 
						attrName = name + "." + trueCollumns[i].getName() ;
					else 
						attrName = trueCollumns[i].getAttrID();
					
					for(int j = 0 ; nextIt.hasNext(); j++)//por cada valor
					{
						String value = nextIt.next();
						
						if(trueCollumns[i].getType().equals("text") )
							value = "\"" + value + "\"";
						else if(trueCollumns[i].getType().equals("date"))
							value = "'" + value + "'";
						
						whereResult += (
								attrName 
								+ " = " + value ) 
								+ ( (j+1) == next.size() ? " " : " or " );
					}
					whereResult += " )";
					builder.where.add(whereResult);
	
				}
				
			}
		}
		Pair<TableAttribute[],Boolean[]> pair = getFactAttributes(query.getCube().getSelectedFacts().getFact(), factTable , thisCube); 
		TableAttribute [] factAttributes = pair.one;
		Boolean[] isDerived = pair.two;
		Iterator<Fact> it = query.getCube().getSelectedFacts().getFact().iterator();
		for(int i = 0; it.hasNext(); i++)
		{
			Fact f = it.next();
	
			Filter before = f.getFilterBefore(), after = f.getFilterAfter();
			String thisFact = "" ;
			
			
			if(isDerived[i])
				thisFact = "(" + factAttributes[i].getAttrID() + ")" ;
			thisFact = factTable.getName() + "." + factAttributes[i].getName();
			
			String selector = f.getAggrOperation() + "(" + thisFact + ")";
			
			builder.select.add(selector);
			headers.addLast( new HeaderInfo(factAttributes[i].getType(),true, f.getFactID(), selector ) );
			
			if(before != null)
			{
				String op = getSign(before.getOp());
				builder.where.add( thisFact + op + before.getValue() );
			}
			
			if(after != null)
			{
				String op = getSign(after.getOp());
				builder.having.add( selector + op + after.getValue() );
			}
		}
				
		return new Pair<String, List<HeaderInfo>>(builder.produceStatement(),headers);
	}
	
	private String getSign(String op) 
	{
		if(op.equals("greater"))
			return " > ";
		else if(op.equals("less"))
			return " < ";
		else return " = ";
	}

	private xml.meta.Cube getCube(String cubeID) 
	{
		for(xml.meta.Cube c : model.getCubes().getCube())
			if(c.getFactTable().getTableID().equals(cubeID))
				return c;
		return null;			
	}

	private Pair<TableAttribute[],Boolean[]> getFactAttributes(List<Fact> facts, Table t, xml.meta.Cube cube) 
	{
		TableAttribute [] collumnAttrs =  new TableAttribute[facts.size()];
		xml.meta.Fact [] trueFacts = new xml.meta.Fact [facts.size()]; 
		Boolean [] derived = new Boolean[facts.size()];
		
		for(Object fact : cube.getFactTable().getFactOrDerivedFact())
		{
			Pair<xml.meta.Fact,Boolean> pair = getFact(fact); 
			xml.meta.Fact anyFact = pair.one;
			
			Iterator<Fact> it = facts.iterator();
			for(int i = 0 ; it.hasNext(); i++)
			{	
				if(anyFact.getFactID().equals(it.next().getFactID()))
				{
					derived[i] = pair.two;
					trueFacts[i] = anyFact;
				}
			}
		}
		
		for(TableAttribute ta : t.getAttributes().getAttribute())
			for(int i = 0 ; i<trueFacts.length ; i++)
				if( !derived[i] && ta.getAttrID().equals( trueFacts[i].getAttrID() ))
					collumnAttrs[i] = ta;
		
		xml.meta.ObjectFactory factory = new xml.meta.ObjectFactory();
		for(int i = 0; i<collumnAttrs.length; i++)
			if(collumnAttrs[i] == null)
			{
				TableAttribute ta = factory.createTableAttribute();
				ta.setAttrID(trueFacts[i].getAttrID());
				collumnAttrs[i] = ta;
			}

		return new Pair<TableAttribute[],Boolean[]>(collumnAttrs,derived);
	}

	private Pair<xml.meta.Fact,Boolean> getFact(Object fact) 
	{
		if(fact instanceof xml.meta.Fact)
			return new Pair<xml.meta.Fact,Boolean>(((xml.meta.Fact) fact), false);
		
		Derived derived = (Derived) fact;
		xml.meta.ObjectFactory factory = new xml.meta.ObjectFactory();
		xml.meta.Fact theFact = factory.createFact();
		
		theFact.setAdditionDimensions(convert(derived.getAdditionDimensions(), factory));
		theFact.setAggregation(convert(derived.getAggregation(),factory));
		theFact.setAttrID(derived.getExpr());
		theFact.setDisplayName(derived.getDisplayName());
		theFact.setFactID(derived.getFactID());
		
		return new Pair<xml.meta.Fact,Boolean>(theFact,true);
			
	}

	private Aggregation convert(xml.meta.Derived.Aggregation aggregation,
			ObjectFactory factory) {
		xml.meta.Fact.Aggregation dims = factory.createFactAggregation();
		for(String aggr : aggregation.getOperation())
			dims.getOperation().add(aggr);
		return dims;
	}

	private AdditionDimensions convert(
			xml.meta.Derived.AdditionDimensions additionDimensions,
			ObjectFactory factory) 
	{
		xml.meta.Fact.AdditionDimensions dims = factory.createFactAdditionDimensions();
		for(xml.meta.Derived.AdditionDimensions.Dimension d : additionDimensions.getDimension())
		{
			AdditionDimensions.Dimension newDim = factory.createFactAdditionDimensionsDimension();
			newDim.setDimID(d.getDimID());
			dims.getDimension().add(newDim);
		}
		return dims;
	}

	//only one attribute encodes the foreign key of a dimension in a fact table
	private String getForeignKey(Table factTable, String toKey) 
	{
		String attr = null;
		for(ForeignKey key : factTable.getForeignKeys().getForeignKey())
			if(key.getTableID().equals(toKey))
			{
				attr =  key.getAttribute().get(0).getAttrID();
				break;
			}
		
		for(TableAttribute ta : factTable.getAttributes().getAttribute())
			if(ta.getAttrID().equals(attr))
				return ta.getName();
		
		return null;
	}
	
	//only one attribute encodes the primary key of a dimension 
	private String getPrimaryKey(Table dimTable) 
	{
		String attr =  dimTable.getPrimaryKey().getAttribute().get(0).getAttrID();
		for(TableAttribute ta : dimTable.getAttributes().getAttribute())
			if(ta.getAttrID().equals(attr))
				return ta.getName();
		
		return null;
	}

	private TableAttribute[] toTableAttributes(List<LevelInfo> collumns,
			Table current) 
	{
		TableAttribute [] collumnAttrs =  new TableAttribute[collumns.size()];

		for(TableAttribute ta : current.getAttributes().getAttribute())
		{
			Iterator<LevelInfo> it = collumns.iterator();
			for(int i = 0 ; it.hasNext(); i++)
				if(ta.getAttrID().equals(it.next().attrID))
					collumnAttrs[i] = ta;
		}
		Iterator<LevelInfo> it = collumns.iterator();
		for(int i = 0 ; it.hasNext(); i++)
		{
			LevelInfo next = it.next();
			if(collumnAttrs[i] == null)
			{
				TableAttribute attr = new  xml.meta.ObjectFactory().createTableAttribute();
				attr.setType(next.snowflakeType);
				collumnAttrs[i]=attr;
			}
				
				
		}
		
		return collumnAttrs;
	}
	
	private Pair<TableAttribute[],Boolean[]> toTableAttributes(
			Table current,List<Collumn> collumns,xml.meta.Dimension dim, Table table) 
	{
		TableAttribute [] collumnAttrs =  new TableAttribute[collumns.size()];
		LinkedList<Triple<String,String,Boolean>> list = new LinkedList<Triple<String,String,Boolean>>();
		Iterator<Collumn> it ;
		Boolean[] snowflakes = new Boolean[collumns.size()];
		for(Level l : dim.getLevels().getLevel())//igual ao level-to-attribute
		{
			it = collumns.iterator();
			for(int i = 0; it.hasNext(); i++)
				if(it.next().getId().equals(l.getLevelID()))
					for(LevelAttribute la : l.getAttributes().getAttribute())
						if(la.getId().equals(l.getPrimary()))
						{
							LevelInfo snow = doSnowflake(la,table);
							snowflakes[i] = snow.isSnowflaked;
							if(snow.isSnowflaked)//if is snowflaked build dummy table attribute
							{
								ObjectFactory factory = new ObjectFactory();
								TableAttribute ta = factory.createTableAttribute();
								ta.setAttrID(snow.snowflakedName);
								ta.setType(snow.snowflakeType);
								collumnAttrs[i] = ta;
							}
							
							
							list.add( new Triple<String, String, Boolean>(
									l.getLevelID(), 
									(snow.isSnowflaked ? snow.snowflakedName : snow.attrID),
									snow.isSnowflaked));
						}
		}
		
		for( TableAttribute ta : current.getAttributes().getAttribute() )
		{
			Iterator<Triple<String,String,Boolean>> it2 = list.iterator();
			for(int i = 0 ; it2.hasNext(); i++)
				if(ta.getAttrID().equals(it2.next().two))
					collumnAttrs[i] = ta;
		}
		return new Pair<TableAttribute[],Boolean[]>(collumnAttrs,snowflakes);
	}


	//Triple<levelId, attID, isSnowflaked>
	private List<LevelInfo> levelToAttribute(List<String> collumns, xml.meta.Dimension dim, xml.meta.Table table) 
	{
		LinkedList<LevelInfo> list = new LinkedList<LevelInfo>();
		Iterator<String> it ;
		for(Level l : dim.getLevels().getLevel()) //por cada nivel existente na dimensao
		{
			it = collumns.iterator(); //por cada coluna seleccionada
			while(it.hasNext()) 
				if(it.next().equals(l.getLevelID())) //se match de levelID
					for(LevelAttribute la : l.getAttributes().getAttribute()) //procurar primary level attribute
						if(la.getId().equals(l.getPrimary())) //TODO: maybe sort e display attributes
						{
							LevelInfo attrValue = doSnowflake(la, table);
							attrValue.levelID = l.getLevelID();
							list.add( attrValue );
						}
		}
		return list;
	}

	private Pair<String, String> doSnowflake(Snowflaked flake, String attr, TableAttribute localAttr,String tableName)
	{

		String fTableID = flake.getFtable(),
				fTableKeyID = flake.getFtablepk();
		
		//get the table to join with
		Table fTable = getTable(fTableID); 
		TableAttribute foreignAttr = null;
		
		//get the info about the foreign table's attribute to join by
		foreignAttr = getTableAttribute(fTable, fTableKeyID);
		
		builder.from.add( fTable.getName() );
		builder.where.add( 
				fTable.getName() + "." + foreignAttr.getName()
				+ " = " + 
				tableName + "." + localAttr.getName()
				);
		
		String callAttr = flake.getAttr();
		if(callAttr != null)
		{
			TableAttribute ta = getTableAttribute(fTable,callAttr);
			return new Pair<String,String>(fTable.getName() + "." + ta.getName(), ta.getType());
		}
		
		return doSnowflake(flake.getSnowflaked(), callAttr, foreignAttr, fTable.getName());
	}
	
	private TableAttribute getTableAttribute(Table table, String key)
	{
		for(TableAttribute ta : table.getAttributes().getAttribute())
			if(ta.getAttrID().equals(key))
				return ta;
				
		return null;
	}
		
	//<value , isSnowflake, type>  ( isSnowflaked ? value=sql name & type : value=attr id & type = null)
	private LevelInfo doSnowflake(LevelAttribute la, Table table) 
	{
		TableAttribute localAttr = null;
		
		if(la.getAttr() == null)
		{
			String fkey = la.getSnowflaked().getFkey();
			localAttr = getTableAttribute(table,fkey);
			
			Pair<String, String> snow = doSnowflake( la.getSnowflaked(), null, localAttr, table.getName() );
			return new LevelInfo( null, null, snow.one, true, snow.two );
		}
		else
			return new LevelInfo( null, la.getAttr(), null, false, null );
		
	}

	private xml.meta.Dimension getDimension(String dimID)
	{
		for(xml.meta.Dimension d : model.getDimensions().getDimension())
			if(d.getDimID().equals(dimID))
				return d;
		return null;
	}
	
	private Table getTable(String tableID)
	{
		for(Table t : model.getTables().getTable())
			if(t.getTableID().equals(tableID))
				return t;
		return null;
	}

	//works with database and table identifiers
	private Database getDatabase(String ID) 
	{
		String dbID = ID;
		if(ID.startsWith("T"))
			for(Table t : model.getTables().getTable())
				if(t.getTableID().equals(ID))
				{
					dbID = t.getDbID();
					break;
				}
		
		for(Database db : model.getDatabases().getDatabase())
			if(dbID.equals(db.getDbID()))
				return db;
				
		return null;
	}

	private Response toResult(ResultSet resp, List<HeaderInfo> headers) 
	{	
		xml.server.ObjectFactory factory = new xml.server.ObjectFactory();
		Response response = factory.createResponse();
		Headers heads = factory.createResponseHeaders();
		Iterator<HeaderInfo> it = headers.iterator();
		while(it.hasNext())
		{
			HeaderInfo next = it.next();
			Head head = factory.createResponseHeadersHead();
			head.setType( (next.isFact ? "fact" : "level") );
			head.setID(next.id);
			heads.getHead().add(head);
		}
		
		RowSet rs = factory.createResponseRowSet();
		
		try {
			resp.beforeFirst();

			while(resp.next())
			{
				Row row = factory.createResponseRowSetRow();
				it = headers.iterator();
				for(int i = 0; it.hasNext(); i++)
				{
					HeaderInfo header = it.next();
					if(header.isFact) //isFact
					{
						row.getValue().add(""+resp.getDouble(header.name));
					}
					else
					{
						String col;
						if(header.type.equals("number"))
							col = ""+ resp.getDouble(header.name);
						else if(header.type.equals("boolean"))
						{
							col = ""+resp.getInt(header.name);
							col = (col.equals("0") ? "false" : "true");
						}
						else
							col = resp.getString(header.name);
						row.getValue().add(col);
					}
					
				}
				rs.getRow().add(row);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		response.setHeaders(heads);
		response.setRowSet(rs);
		return response;
	}
	
	@Override
	public MetaModel getSimpleMeta() 
	{
		return model;
	}
	
	private void loadMeta()
	{
		
		
//		if(model != null)
//			return;
//		
//		MetaModel m = new xml.meta.ObjectFactory().createMetaModel();
//		ObjectSet<MetaModel> resp = db.query(MetaModel.class);
//		
//		for(MetaModel o : resp)
//		{
//			if(o instanceof MetaModel)
//			{
//				System.out.println("meta found!!");
//				model = (MetaModel) o;
//				
//			}
//		}
//		db.close();
	}

	
	
	@Override
	public void storeMeta(MetaModel model) 
	{
		File f = new File(DB_PATH);
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f)); 
			oos.writeObject(model);
			oos.flush();
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		
//		try {
//
//			JAXBContext context =  JAXBContext.newInstance("xml.meta");
//			context.createMarshaller().marshal(model, System.out);
//		} catch (JAXBException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println("storing meta");
//		
//		db.store(model);
//		db.commit();
//		db.close();
//		System.out.println("finished storing");
		
		
	}

	private final String NUMBER = "number", DATE = "date", UNIQUE = "unique", TEXT = "text";
	@Override
	public ListOfValues getListOfValues(String dimID, String levelID) throws QueryFailedException, FailedConnectionException 
	{
		loadMeta();
		xml.meta.Dimension dim = getDimension(dimID);
		Table table = getTable(dim.getTableID());
		xml.meta.Level level = getLevel(dim, levelID);
		SQLBuilder lovBuilder = new SQLBuilder(); 
		String attrID = "";
		Table currentTable = table;
		boolean snowflaked = true;
		for(LevelAttribute la : level.getAttributes().getAttribute())
			if(la.getId().equals(level.getPrimary()))
			{
				attrID = la.getAttr();
				Snowflaked snow = la.getSnowflaked();
				currentTable = table;
				TableAttribute localAttr;
				while( attrID == null)
				{
					localAttr = getTableAttribute(currentTable,snow.getFkey());
					String fTableID = snow.getFtable(),
							fTableKeyID = snow.getFtablepk();
					
					//get the table to join with
					Table fTable = getTable(fTableID); 
					TableAttribute foreignAttr = null;
					
					//get the info about the foreign table's attribute to join by
					foreignAttr = getTableAttribute(fTable, fTableKeyID);
					
					lovBuilder.from.add( fTable.getName() );
					lovBuilder.where.add( 
							fTable.getName() + "." + foreignAttr.getName()
							+ " = " + 
							currentTable.getName() + "." + localAttr.getName()
							);
					
					currentTable = fTable;
					attrID = snow.getAttr();
					snow = la.getSnowflaked();
				}
			}
	
		TableAttribute collumnAttr = null;
		for(TableAttribute ta : currentTable.getAttributes().getAttribute())
			if(ta.getAttrID().equals(attrID))
				collumnAttr = ta;
		
		String query = "";
		boolean isText = false;
		if(collumnAttr.getType().equals(NUMBER))
		{
			lovBuilder.select.add("MIN(" + currentTable.getName() + "." + collumnAttr.getName() + ")");
			lovBuilder.select.add("MAX(" + currentTable.getName() + "." + collumnAttr.getName() + ")");
			lovBuilder.from.add(table.getName());
		}
		else if(collumnAttr.getType().equals(TEXT))
		{
			isText = true;
			lovBuilder.select.add( currentTable.getName() + "." + collumnAttr.getName() );
			lovBuilder.from.add( table.getName() ) ;
			lovBuilder.groupby.add( currentTable.getName() + "." + collumnAttr.getName() );
		}
		query = lovBuilder.produceStatement();
		Database db = getDatabase(table.getDbID());
		
		String driver = db.getJdbcDriver(),
				url= db.getUrl(),
				username= db.getUsername(),
				password= db.getPassword();
		System.out.println(query);
		ResultSet resp = new SQLModule(driver, url, username, password).executeQuery(query);
		
		if(isText)
			return toLOV(resp, currentTable.getName() + "." + collumnAttr.getName());
		return toSmallLOV(resp, currentTable.getName()+"."+collumnAttr.getName());
	}
	
	private ListOfValues toLOV(ResultSet resp, String label) 
	{
		xml.lov.ObjectFactory factory = new xml.lov.ObjectFactory();
		ListOfValues list = factory.createListOfValues();
		List<String> theList = list.getValue();
		
		try {
			
			resp.beforeFirst();
			while(resp.next())
				theList.add(resp.getString(label));
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}

	private xml.meta.Level getLevel(xml.meta.Dimension dim , String levelID )
	{
		for(Level l : dim.getLevels().getLevel())
			if(l.getLevelID().equals(levelID))
				return l;
		return null;
	}

	@Override
	public ListOfValues getMinMax(String tableID, String factID) throws QueryFailedException, FailedConnectionException {
		loadMeta();
		
		Table table = getTable(tableID);
		xml.meta.Cube cube = getCube(tableID);
		TableAttribute collumnAttr =  null;
		xml.meta.Fact trueFact =null; 
		Boolean derived = false;
		
		for(Object fact : cube.getFactTable().getFactOrDerivedFact())
		{
			Pair<xml.meta.Fact,Boolean> pair = getFact(fact); 
			xml.meta.Fact anyFact = pair.one;
			derived = pair.two;
			if(!anyFact.getFactID().equals(factID))
				continue;
			
			trueFact = anyFact;
			break;
		}
		
		for(TableAttribute ta : table.getAttributes().getAttribute())
		{
			if( !ta.getAttrID().equals( trueFact.getAttrID() ) )
				continue;
			
			collumnAttr = ta;
			break;
		}
		
		String name;
		
		if(derived)
			name = trueFact.getAttrID();
		else
			name = table.getName() + "." + collumnAttr.getName();
		
		
		String query = "SELECT MIN(" + name + ") ,  MAX(" + name + ") ";
		query += "FROM " + table.getName() + " ";
		
		Database db = getDatabase(table.getDbID());
		
		String driver = db.getJdbcDriver(),
				url= db.getUrl(),
				username= db.getUsername(),
				password= db.getPassword();
		System.out.println(query);
		ResultSet resp = new SQLModule(driver, url, username, password).executeQuery(query);
		
		return toSmallLOV(resp, name);

	}

	private ListOfValues toSmallLOV(ResultSet resp, String attr) 
	{
		
		xml.lov.ObjectFactory factory = new xml.lov.ObjectFactory();
		ListOfValues list = factory.createListOfValues();
		List<String> theList = list.getValue();
		
		try {
			
			resp.beforeFirst();
			if(resp.next())
			{
				theList.add(""+resp.getDouble("MIN("+attr+")"));
				theList.add(""+resp.getDouble("MAX("+attr+")"));
			}
			else
				System.out.println("notinhg in rs");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return list;
	}

}
