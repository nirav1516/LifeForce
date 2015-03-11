/*
 * copyright 2012, gash

 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

/*
 * this class creates appropriate resource for each type of request by 
 * client and handles these requests accordingly
 * 
 * */
package poke.resources;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.google.protobuf.ByteString;

import poke.server.resources.Resource;
import eye.Comm.Header;
import eye.Comm.NameValueSet;
import eye.Comm.Payload;
import eye.Comm.PhotoHeader;
import eye.Comm.Request;

public class JobResource implements Resource {

	@Override
	public Request process(Request request) {
			if(request.hasHeader() && request.hasBody()){
			Header reqheader=request.getHeader();
			Payload reqBody = request.getBody();
			
			if(reqheader.hasPhotoHeader()){
				//for image write requests
				if(reqheader.getPhotoHeader().getRequestType()==PhotoHeader.RequestType.read){
					PhotoReadResource photoread=new PhotoReadResource();
					return photoread.process(request);
				}
				//for image read requests
				else if(reqheader.getPhotoHeader().getRequestType()==PhotoHeader.RequestType.write){
					PhotoCreatorResource imgc=new PhotoCreatorResource();
					return imgc.processRequest(request);
				}
				//for image delete requests
				else if(reqheader.getPhotoHeader().getRequestType()==PhotoHeader.RequestType.delete){
					PhotoDeleteResource imgc=new PhotoDeleteResource();
					return imgc.process(request);
				}
			}
				//////////////////////////////
			NameValueSet set = reqBody.getJobOp().getData().getOptions();
			set.getNodeList();
			
		}
		return request;
	}

}
