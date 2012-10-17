package com.paperbag.javarequestkit.bb;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import com.paperbag.javarequestkit.*;

import net.rim.device.api.io.IOUtilities;

public class RequestCache {

	private static final String storageLocation = "file:///SDCard/Powerscreen/RequestCache/";
	private static final String cacheLocation = "/xml/requestCache/";
	private static final String dealerListUrl = "http://api.powerscreen.co.uk/dealers/?action=list&country=";
	
	public static Response get(Request request) {
		
		Response response = null;
		
		try {
			FileConnection connection = (FileConnection)Connector.open(storageLocation + filterFilename(request.getUrl()));
			InputStream inputStream = connection.openInputStream();
			
			byte[] fileContents = new byte[(int)connection.fileSize()];
			int length = 0;  
            StringBuffer rawResponse = new StringBuffer();  
            
            while (-1 != (length = inputStream.read(fileContents)))  
            {  
                rawResponse.append(new String(fileContents, 0, length));  
            }  
			
			inputStream.close();
			inputStream = null;
			
			// Create the response with the raw data
			response = new Response(rawResponse.toString(), 200);
			
		} catch (IOException e) {
			System.out.println("(" + e.getClass() + "): " + e.getMessage());
		}
		
		System.out.println("Cache hit");
		return response;
	}
	

	public static Response getFromResource(Request request) {
		Response response = null;
		
		InputStream inputStream = null;
		String filename = filterFilename(request.getUrl());
		try {
			
			// Pull the file out of the resources bundle
			inputStream = RequestCache.class.getResourceAsStream(cacheLocation + filename);
			
			byte[] fileContents = IOUtilities.streamToBytes(inputStream);
			
			inputStream.close();
			inputStream = null;
			
			// Create the response with the raw data
			response = new Response(new String(fileContents), 200);
		
		} catch (Exception ex) {}
		
		return response;
	}
	
	public static boolean exists(Request request) {
		
		boolean exists = false;
		
		try {
			FileConnection connection = (FileConnection)Connector.open(storageLocation + filterFilename(request.getUrl()));
			exists = connection.exists();
			
		} catch (Exception e) {
			System.out.println("(" + e.getClass() + "):exists():" + e.getMessage());
		}
		
		return exists;
	}

	public static void store(Request request, Response response) {
		
		FileConnection connection;
		try {
			
			// Create folder if not already created
			connection = (FileConnection)Connector.open(storageLocation);
	        if (!connection.exists()) {
	        	connection.mkdir();
	        }
	        connection.close();
			
	        // Now create the file
			connection = (FileConnection)Connector.open(storageLocation + filterFilename(request.getUrl()));
			
			// If we already have a cache of this request, delete it
			if (connection.exists()) {
				connection.delete();
			}
			connection.create();
		
			// Convert the response text to a byte array and write it out
			OutputStream outputStream = connection.openOutputStream();
			outputStream.write(response.getTextResponse().getBytes());
		
			// Clean up
			outputStream.close();
			outputStream = null;
			
		} catch (IOException e) {
			System.out.println("(" + e.getClass() + "): " + e.getMessage());
		}
	}

	private static String filterFilename(String filename) {
		
		char[] illegalChars = {'<','>',':','"','\\' ,'|' , '*' , '?' ,'/' };
		
		String filteredFilename = filename;
		for (int i = 0; i < illegalChars.length ; i++) {
			filteredFilename = filteredFilename.replace(illegalChars[i], '_');
		}
		
		return filteredFilename;
	}

	public void initalise(Class resourceContainer, ICallback callback) {
	
		FileConnection connection = null;
		
		// Create the resources directory on the SD Card
		try {
			connection = (FileConnection)Connector.open(storageLocation);
	
	        if (!connection.exists()) {
	        	connection.mkdir();
	        	
	        	// Copy the country list
	    		this.copyFileToResourceCache(resourceContainer, "http___api.powerscreen.co.uk_dealers__action=countries");
	    		
	    		// Copy all the country files from the resources bundle onto the SD Card
	    		String[] files = new String[] { "Alabama","Alaska","Alberta","Algeria","Argentina","Arizona","Arkansas","Armenia","Australia","Austria","Azerbaijan","Bahrain","Bangladesh","Belgium","Belize","Bolivia","Botswana","Brazil","British%20Colombia","Bulgaria","California","Caribbean","Chile","Colombia","Colorado","Connecticut","Costa%20Rica","Croatia","Cyprus","Czech%20Republic","Delaware","Denmark","District%20of%20Colombia","Dominican%20Republic","Ecuador","El%20Salvador","Estonia","Finland","Florida","France","Georgia","Germany","Ghana","Greece","Guatemala","Haiti","Hawaii","Honduras","Hong%20Kong","Hungary","Iceland","Illinois","India","Indiana","Indonesia","Iowa","Iraq","Ireland","Israel","Italy","Japan","Kansas","Kazakhstan","Kentucky","Latvia","Lesotho","Libya","Lithuania","Louisiana","Maine","Malaysia%20East","Malaysia%20West","Maryland","Massachusetts","Mexico","Michigan","Minnesota","Mississippi","Missouri","Montana","Morocco","Namibia","Nebraska","Nepal","Netherlands","Nevada%20North","Nevada%20South","New%20Hampshire","New%20Jersey","New%20Mexico","New%20York","New%20Zealand","Nicaragua","Nigeria","North%20Carolina","North%20Dakota","Norway","Ohio","Oklahoma","Oman","Ontario","Oregon","Pakistan","Panama","Paraguay","Pennsylvania","Peru","Phillippines","Poland","Portugal","Puerto%20Rico","Qatar","Rhode%20Island","Romania","Russia","Singapore","Slovakia","Slovenia","South%20Africa","South%20Carolina","South%20Dakota","South%20Korea","Spain","Suriname","Swaziland","Sweden","Switzerland","Tahiti","Taiwan","Tennessee","Texas","Thailand","Tunisia","Turkey","UK","Ukraine","United%20Arab%20Emirates","Uruguay","Utah","Venezuela","Vermont","Virginia","Virginia%20West","Washington","Wisconsin","Wyoming%20East","Wyoming%20West" };
	    		
	    		for (int i=0; i < files.length; i++) {
	    			String filename = filterFilename(dealerListUrl + files[i]);
	    			this.copyFileToResourceCache(resourceContainer, filename);		
	    			System.out.println("*************" + filename);
	    		}
	        }
        
		} catch (IOException e) {
			System.out.println("Could not create the resources directory on the SD Card");
			callback.failed(null);
		} finally {
			try { connection.close(); } catch(Exception e) {}
		}
		
		callback.success(null);

	}
	
	public static boolean existsInResource(Request request) {
		boolean exists = false;
		
		InputStream inputStream = null;
		String filename = filterFilename(request.getUrl());
		try {
			System.out.println("********************** Check resources for FILENAME:" + filename);
			
			// Pull the file out of the resources bundle
			inputStream = RequestCache.class.getResourceAsStream(cacheLocation + filename);
			
			exists = (inputStream != null);
			
		} catch (Exception ex) {}
		
		return exists;
	}

	
	private void copyFileToResourceCache(Class resourceContainer, String filename) {
		
		if ((filename == null) || filename.equals("")) {
			throw new IllegalArgumentException("Input stream not supplied");
		}
		
		InputStream inputStream = null;
		FileConnection outputConnection = null;
		OutputStream outputStream = null;
		try {
			System.out.println("********************** FILENAME:" + filename);
			
			// Pull the file out of the resources bundle
			inputStream = resourceContainer.getResourceAsStream(cacheLocation + filename);
			
			// Open a file on the SD card with the same name
			outputConnection = (FileConnection)Connector.open(storageLocation + filename);
			outputConnection.create();
			
			// Output the contents of the file to the SDCard
			outputStream = outputConnection.openOutputStream();
			outputStream.write(IOUtilities.streamToBytes(inputStream));
		
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				// Clean up
				if (inputStream != null) {
					inputStream.close();
					inputStream = null;
				}
				if (outputStream != null) {
					outputStream.close();
					outputStream = null;
				}
				if (outputConnection != null) {
					outputConnection.close();
					outputConnection = null;
				}
			} catch (IOException e) { }
		}
		
		
	}

	public boolean initRequired() {
		FileConnection connection = null;
		boolean initRequired = false;
		
		// Check if the cache directory has already been created on the SD Card
		try {
			connection = (FileConnection)Connector.open(storageLocation);
			initRequired = !connection.exists();
		} catch (IOException e) { }
	
		return initRequired;
	}
}