package logic;

import db.FailedConnectionException;
import db.QueryFailedException;
import xml.client.Query;
import xml.lov.ListOfValues;
import xml.meta.MetaModel;
import xml.server.Response;

public interface MetaModelHandler 
{

	public Response doQuery(Query query) throws QueryFailedException, FailedConnectionException;
	
	public MetaModel getSimpleMeta();
	
	public void storeMeta(MetaModel model);
	
	public ListOfValues getMinMax(String tableID, String factID) throws QueryFailedException, FailedConnectionException;
	
	public ListOfValues getListOfValues(String dimID, String levelID) throws QueryFailedException, FailedConnectionException;
}
