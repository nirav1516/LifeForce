package poke.resources;

/*
 * This class handles image write request. It fetches image from protobuf request message 
 * and converts it to byte array and stores it in mongo db.
 *  Writing into mongodb is done by mongoconnection class.
 *  It returns failure if request is not completed successfully.
 */

import java.io.ByteArrayOutputStream;
import java.net.UnknownHostException;
import java.util.UUID;

import poke.databaseConnection.MongoConnection;

import com.google.protobuf.ByteString;

import eye.Comm.Header;
import eye.Comm.Header.Routing;
import eye.Comm.Payload;
import eye.Comm.PhotoHeader;
import eye.Comm.PhotoHeader.ResponseFlag;
import eye.Comm.PhotoPayload;
import eye.Comm.Request;

public class PhotoCreatorResource {
	public Request processRequest(Request request){
		  Request.Builder request2=Request.newBuilder();
          
		
			Payload reqBody=request.getBody();
			Header reqHeader=request.getHeader();
			PhotoHeader photoheader=reqHeader.getPhotoHeader();
			
			PhotoPayload photopayload=reqBody.getPhotoPayload();
			String name=photopayload.getName();
			
			ByteString image=photopayload.getData();
			
			ByteArrayOutputStream baos=new ByteArrayOutputStream();
			Integer time=(int) System.currentTimeMillis();;
			
			UUID uuid=UUID.randomUUID();
			byte[] imagebytes=image.toByteArray();
			String UniqueId=time.toString();
			
			try {
				MongoConnection con=MongoConnection.getInstance();
				con.insert(uuid, imagebytes,name);
			} catch (Exception e) {
				
				e.printStackTrace();
				 Payload.Builder paylBuilder=Payload.newBuilder();
				 Header.Builder headerBuilder=Header.newBuilder();
				 PhotoHeader.Builder phb=PhotoHeader.newBuilder();
				 phb.setResponseFlag(ResponseFlag.failure);
				 phb.setLastModified(System.currentTimeMillis());
				 PhotoPayload.Builder ppb=PhotoPayload.newBuilder();
				 headerBuilder.setOriginator(1);
				 headerBuilder.setRoutingId(Routing.REPORTS);
				 headerBuilder.setToNode(request.getHeader().getToNode());
				 paylBuilder.setPhotoPayload(ppb.build());
				 
				 headerBuilder.setPhotoHeader(phb.build());
				 request2.setHeader(headerBuilder.build());
			
			
				 request2.setBody(paylBuilder.build());
				 
		            return request2.build();
			}
		
			 Payload.Builder paylBuilder=Payload.newBuilder();
			 Header.Builder headerBuilder=Header.newBuilder();
			 PhotoHeader.Builder phb=PhotoHeader.newBuilder();
			 phb.setResponseFlag(ResponseFlag.success);
			 phb.setLastModified(System.currentTimeMillis());
			 
			 PhotoPayload.Builder ppb=PhotoPayload.newBuilder();
			 ppb.setUuid(uuid.toString());
			 
			 headerBuilder.setOriginator(1);
			 headerBuilder.setToNode(request.getHeader().getToNode());
			 headerBuilder.setRoutingId(Routing.JOBS);
			 
			 paylBuilder.setPhotoPayload(ppb.build());
			 
			 headerBuilder.setPhotoHeader(phb.build());
			 request2.setHeader(headerBuilder.build());
		
		
			 request2.setBody(paylBuilder.build());
			 
	            return request2.build();
			}
}
