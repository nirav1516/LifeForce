package poke.resources;

import java.net.UnknownHostException;

import eye.Comm.Header;
import eye.Comm.Payload;
import eye.Comm.PhotoHeader;
import eye.Comm.PhotoPayload;
import eye.Comm.Request;
import eye.Comm.Header.Routing;
import eye.Comm.PhotoHeader.ResponseFlag;
import poke.databaseConnection.MongoConnection;
import poke.server.resources.Resource;

/*
 * This class handles image delete request. It fetches uuid from protobuf request message 
 * and deletes image with that uuid from mongodb.
 *  Deleting from mongodb is done by mongoconnection class.
 *  It returns failure if request is not completed successfully.
 */
public class PhotoDeleteResource implements Resource {

	@Override
	public Request process(Request request) {
		// TODO Auto-generated method stub
		String uuid=request.getBody().getPhotoPayload().getUuid();
		MongoConnection con;
		Request.Builder reply=Request.newBuilder();
		
		
		 Payload.Builder paylBuilder=Payload.newBuilder();
		 Header.Builder headerBuilder=Header.newBuilder();
		 PhotoHeader.Builder phb=PhotoHeader.newBuilder();
		 PhotoPayload.Builder ppb=PhotoPayload.newBuilder();
		 
	
		
		
		
		try {
			con = MongoConnection.getInstance();

			boolean result=con.delete(uuid);
			if(result){
				phb.setResponseFlag(ResponseFlag.success);
				System.out.println("Deleted");
			}
			else{
				phb.setResponseFlag(ResponseFlag.failure);
				System.out.println("not Deleted");

			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			phb.setResponseFlag(ResponseFlag.failure);
			e.printStackTrace();
		}
		 headerBuilder.setOriginator(1);
		 headerBuilder.setRoutingId(Routing.JOBS);
		 headerBuilder.setToNode(request.getHeader().getToNode());
		 paylBuilder.setPhotoPayload(ppb.build());
		 
		 headerBuilder.setPhotoHeader(phb.build());
		 reply.setHeader(headerBuilder.build());
	
	
		 reply.setBody(paylBuilder.build());
	
		return reply.build();
	
	}

}
