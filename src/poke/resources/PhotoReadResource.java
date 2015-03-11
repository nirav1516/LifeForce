package poke.resources;

import java.net.UnknownHostException;
import java.util.UUID;

import javax.print.DocFlavor.READER;

import com.google.protobuf.ByteString;

import eye.Comm.Header;
import eye.Comm.Payload;
import eye.Comm.PhotoHeader;
import eye.Comm.PhotoPayload;
import eye.Comm.Header.Routing;
import eye.Comm.PhotoHeader.RequestType;
import eye.Comm.PhotoHeader.ResponseFlag;
import eye.Comm.Request;
import poke.databaseConnection.ImageObject;
import poke.databaseConnection.MongoConnection;
import poke.server.resources.Resource;

/*
 * This class handles image read request. It fetches uuid from protobuf request message 
 * and fetches image with that uuid from mongo db.
 *  Reading from mongodb is done by mongoconnection class.
 *  It returns failure if request is not completed successfully.
 */
public class PhotoReadResource implements Resource {

	@Override
	public Request process(Request request) {
		// TODO Auto-generated method stub
		String uuid=request.getBody().getPhotoPayload().getUuid();
		MongoConnection mc;
		
		Request.Builder reply=Request.newBuilder();
		
		
		 Payload.Builder paylBuilder=Payload.newBuilder();
		 Header.Builder headerBuilder=Header.newBuilder();
		 PhotoHeader.Builder phb=PhotoHeader.newBuilder();
		 PhotoPayload.Builder ppb=PhotoPayload.newBuilder();
		 
	
		
		
		try {
			mc = MongoConnection.getInstance();
			ImageObject img=new ImageObject();
			img.setUuid(uuid);
			img=mc.read(img);
			if(img.getImageData()==null){
				phb.setResponseFlag(ResponseFlag.failure);
				
			}
			else{
			ByteString imagedata=ByteString.copyFrom(img.getImageData());
			String name=img.getName();
		
			phb.setResponseFlag(ResponseFlag.success);
			ppb.setData(imagedata);
			ppb.setName(name);
			}
			// phb.setLastModified(System.currentTimeMillis());
			 
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			phb.setResponseFlag(ResponseFlag.failure);
			
			// phb.setLastModified(System.currentTimeMillis());
		
		}
		
		 headerBuilder.setOriginator(1);
		 headerBuilder.setToNode(request.getHeader().getToNode());
		 headerBuilder.setRoutingId(Routing.JOBS);
	
		 paylBuilder.setPhotoPayload(ppb.build());
		 
		 headerBuilder.setPhotoHeader(phb.build());
		 reply.setHeader(headerBuilder.build());
	
	
		 reply.setBody(paylBuilder.build());
	
		return reply.build();
	}

}