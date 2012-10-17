package com.paperbag.javarequestkit.bb;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import com.paperbag.javarequestkit.*;

import net.rim.device.api.system.CoverageInfo;
import net.rim.device.api.ui.UiApplication;

public class BBRequestHandler extends RequestHandler {
	
	public BBRequestHandler(Request request, ICallback callback) {
		super(request, callback);
	}

	public void run() {
		
		HttpConnection connection = null;  
		Response response = null;
		
        try  
        {  
        	if(!CoverageInfo.isOutOfCoverage()) {
        		connection = (HttpConnection) Connector.open(request.getUrl() + BBWebUtils.appendConnectionString(), Connector.READ, true);  
    
        		response = extractTextResponse(connection);
        	} else {
        		throw new NoCoverageException();
        	}
        	
			// If we got a valid response and we are allowed, cache it
			if ((response != null) && (request.isCacheable())) {
				RequestCache.store(request, response);
			}
  
        	this.success(response);
			
        }  
        catch (final Exception ex) {  
        	
        	if ((request.isCacheable()) && (RequestCache.exists(request))) {
        		response = RequestCache.get(request);
        		this.success(response);
        	} else if ((request.isCacheable()) && (RequestCache.existsInResource(request))) {
        		response = RequestCache.getFromResource(request);
        		RequestCache.store(request, response);
        		this.success(response);
        	} else {
        		this.failed(ex);
        	}

        } finally {  
            try	{  
            	connection.close();  
                connection = null; 
            } catch(Exception e){}  
        }  
	}

	private void success(final Response response) {
    	UiApplication.getUiApplication().invokeLater(new Runnable()  
        {  
    		public void run()  
            {  
    			callback.success(response);   
            }  
        });  
	}
	
	private void failed(final Exception ex) {
    	
    	UiApplication.getUiApplication().invokeLater(new Runnable()  
        {  
    		public void run()  
            {  
    			callback.failed("(" + ex.getClass() + "): " + ex.getMessage());
            }  
        });  
	}
	
	private Response extractTextResponse(HttpConnection connection) throws IOException {
		
		InputStream inputStream = null;
		Response response = null;
		
		try {
			inputStream = connection.openInputStream();
			
			byte[] responseData = new byte[10000];  
			int length = 0;  
			StringBuffer rawResponse = new StringBuffer();  
			
			while (-1 != (length = inputStream.read(responseData))) {  
				rawResponse.append(new String(responseData, 0, length));  
			}  
		 
			// Check we got a valid response
			int responseCode = connection.getResponseCode();  
			if (responseCode != HttpConnection.HTTP_OK) {  
				callback.failed("HTTP response code: " + responseCode);
			} else {
				response = new Response(rawResponse.toString(), responseCode);
			}
		} finally {
			try { 
				inputStream.close();
				inputStream = null;
            }  catch(Exception e){}  
		}
		
		return response;
	}
}