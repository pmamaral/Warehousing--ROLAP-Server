package view;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import db.FailedConnectionException;
import db.QueryFailedException;

import logic.AlreadyInsertedException;
import logic.MetaModelControl;
import logic.ReportControl;

import xml.client.Query;
import xml.lov.ListOfValues;
import xml.meta.MetaModel;
import xml.server.Response;



@Path("/")
public class API 
{

	@GET
	@Path("/init")
	@Produces(MediaType.TEXT_XML)
	public String setMetaSource(@QueryParam("url") String url)
	{
		System.out.println("setMetaSource(url: " + url + ")");
		MetaModel model = readToModel( streamFromURL(url) );
		new MetaModelControl().storeMeta(model);
		return "<xml>"+url+"</xml>";
	}

	@GET
	@Path("/test")
	@Produces(MediaType.TEXT_XML)
	public String test(@QueryParam("url") String url)
	{

		return "<xml>"+url+"</xml>";
	}

	@GET
	@Path("/metamodel")
	@Produces(MediaType.TEXT_XML)
	public MetaModel getMetaModel() 
	{
		return new MetaModelControl().getSimpleMeta();
	}


	@POST
	@Path("/doQuery")
	@Consumes(MediaType.TEXT_XML)
	@Produces(MediaType.TEXT_XML)
	public Response doQuery(Query q) 
	{
		try {
			return new MetaModelControl().doQuery(q);
		} catch (QueryFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FailedConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@POST
	@Path("/report")
	@Consumes(MediaType.TEXT_XML)
	public void storeReport(Query q, @QueryParam("name") String name) 
	{
		System.out.println("store report " +name );
		try {
			new ReportControl().storeReport(q, name);
		} catch (AlreadyInsertedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@GET
	@Path("/report")
	@Produces(MediaType.TEXT_XML)
	public Query retrieveReport(@QueryParam("name") String name)
	{
		return new ReportControl().getReport(name);
	}
	
	@GET
	@Path("/lov")
	@Produces(MediaType.TEXT_XML)
	public ListOfValues getListOfValues(@QueryParam("dim") String dimID, @QueryParam("level") String levelID)
	{
		try {
			return new MetaModelControl().getListOfValues(dimID,levelID);
		} catch (QueryFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FailedConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@GET
	@Path("/minmax")
	@Produces(MediaType.TEXT_XML)
	public ListOfValues getMinMax(@QueryParam("table") String tableID, @QueryParam("fact") String factID)
	{
		try {
			
			return new MetaModelControl().getMinMax(tableID,factID);
			
		} catch (QueryFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FailedConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@GET
	@Path("/lor")
	@Produces(MediaType.TEXT_XML)
	public ListOfValues getReportList()
	{
		return new ReportControl().getList();
	}
	
	private InputStream streamFromURL(String url)
	{
		InputStream returnStream = null;
		try {
			URLConnection conn = new URL(url).openConnection();
			returnStream = conn.getInputStream();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String result = "";
		Scanner s = new Scanner(returnStream);

		while(s.hasNext())
			result += (s.nextLine());
		;
		return new ByteArrayInputStream(result.getBytes());
	}
	

	private MetaModel readToModel(InputStream stream) 
	{
		MetaModel model=null;
		try {
			JAXBContext  context =JAXBContext.newInstance("xml.meta") ;
			Unmarshaller unmarshaller = context.createUnmarshaller() ;
			model = (MetaModel) (((JAXBElement)unmarshaller.unmarshal(stream)).getValue());
		} catch (JAXBException e) {
			e.printStackTrace();
		} 
		return model;
	}

}
