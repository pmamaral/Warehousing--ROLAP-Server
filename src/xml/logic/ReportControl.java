package logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import xml.client.Query;
import xml.lov.ListOfValues;
import xml.lov.ObjectFactory;

public class ReportControl {

	
	public ReportControl()
	{
		File f = new File("reports");
		if(!f.exists())
			f.mkdir();
	}

	public void storeReport(Query query, String name) throws AlreadyInsertedException 
	{
		File f = new File("reports/"+name);
		try {
			if(f.exists())
				throw new AlreadyInsertedException();
			else	
				f.createNewFile();
			
			Report report = new Report(query,name);
			
			new ObjectOutputStream(new FileOutputStream(f)).writeObject(report);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}


	public Query getReport(String name) 
	{
		File f = new File("reports/"+name);
		try {
			
			if(!f.exists())
				return null;
			
			return ((Report) new ObjectInputStream(new FileInputStream(f)).readObject()).getQuery();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public ListOfValues getList() 
	{
		ObjectFactory factory = new ObjectFactory();
		xml.lov.ListOfValues lov = factory.createListOfValues();
		
		File f = new File("reports");
		for(File file : f.listFiles())
			lov.getValue().add(file.getName());

		return lov;
	}
}
