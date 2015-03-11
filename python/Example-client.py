import comm_pb2
import socket               
import time
import struct
import os
import Image
import io

##############
def imageCreateRequest(imagename,imagebytes,ownerId):
    reqid=str(int(round(time.time() * 1000)))
    r=comm_pb2.Request()

    r.body.photoPayload.name=imagename
    r.body.photoPayload.data=imagebytes

    r.header.photoHeader.requestType=comm_pb2.PhotoHeader.write

    r.header.originator = 0  
    r.header.routing_id = comm_pb2.Header.JOBS
    r.header.toNode = 0
    
    msg = r.SerializeToString()
    return msg


def imageDeleteRequest(uuid):
    reqid=str(int(round(time.time() * 1000)))
    r=comm_pb2.Request()

    r.header.photoHeader.requestType=comm_pb2.PhotoHeader.delete
    r.header.routing_id=comm_pb2.Header.JOBS
    r.header.toNode=0
    r.header.originator=0

    r.body.photoPayload.uuid=uuid

    msg=r.SerializeToString()
    return msg



def imageRetreivalRequest(uuid):
    reqid=str(int(round(time.time() * 1000)))
    r=comm_pb2.Request()

    r.header.photoHeader.requestType=comm_pb2.PhotoHeader.read
    r.header.routing_id=comm_pb2.Header.JOBS
    r.header.toNode=0
    r.header.originator=0

    r.body.photoPayload.uuid=uuid

    msg=r.SerializeToString()
    return msg

#######################################
  


def sendMsg(msg_out, port, host):
    s = socket.socket()         
#    host = socket.gethostname()
#    host = "192.168.0.87"

    s.connect((host, port))        
    msg_len = struct.pack('>L', len(msg_out))    
    s.sendall(msg_len + msg_out)
    len_buf = receiveMsg(s, 4)
    msg_in_len = struct.unpack('>L', len_buf)[0]
    msg_in = receiveMsg(s, msg_in_len)
    r = comm_pb2.Request()
    r.ParseFromString(msg_in)
#    print msg_in
#    print r.body.job_status 
#    print r.body.img_creation_response.uniqueId
#    print r.body.job_op.data.options
    s.close
    return r
def receiveMsg(socket, n):
    buf = ''
    while n > 0:       
        data = socket.recv(n)          
        if data == '':
            raise RuntimeError('data not received!')
        buf += data
        n -= len(data)
    return buf  

def readimage(path):
    count = os.stat(path).st_size / 2
    with open(path, "rb") as f:
        return bytearray(f.read())

def getBroadcastMsg(port):

          
    sock = socket.socket(socket.AF_INET,  # Internet
                        socket.SOCK_DGRAM)  # UDP
   
    sock.bind(('', port))
   
    data = sock.recv(1024)  # buffer size is 1024 bytes
    return data
        
   
if __name__ == '__main__':
     
    
    host = raw_input("IP:")
    port = raw_input("Port:")

    port = int(port)
    whoAmI = 1;
    while True:
        input = raw_input("Welcome to our LifeForce client! Kindly select your desirable action:\n1.Upload Image\n2.Download Image\n3.Delete Image\n")
        if input == "1":
        
    	   image = raw_input("Image Path:");
           imagename=raw_input("Image Name");
           byte=readimage("/home/megha/Downloads/download.jpg")
           data=bytes(byte)    	   
           imagetosend=imageCreateRequest(imagename,data,1)
           result=sendMsg(imagetosend,port,host)
           print "Result",result.header.photoHeader.responseFlag
           print result.header.photoHeader.responseFlag
           if(result.header.photoHeader.responseFlag==comm_pb2.PhotoHeader.success):
              print "Image uploaded successfully."
              print "UUID: ",result.body.photoPayload.uuid
           else:
              print "Failed to upload the image"
        elif input == "2":        
           print("Please enter UUID of image you want to recieve") 
           uuid=raw_input("UUID: ")
           imagetoRead=imageRetreivalRequest(uuid)
           result=sendMsg(imagetoRead,port,host)
           name=result.body.photoPayload.name
           imagedata=result.body.photoPayload.data
           
           if(result.header.photoHeader.responseFlag==comm_pb2.PhotoHeader.success):
           # f1 = open("/home/nirav1516/Desktop/img1.jpg","wb")
               image=Image.open(io.BytesIO(bytearray(imagedata)))
               filepath="/home/megha/Desktop/img"+name+".jpg"
               print "Image ",name," storing at ",filepath
               image.save(filepath)
           else:
                print("Download failed")
           
        elif input == "3":
           print("Please enter UUID of image you want to recieve") 
           uuid=raw_input("UUID: ")
           imagetodelete=imageDeleteRequest(uuid)
           result=sendMsg(imagetodelete,port,host)
           if(result.header.photoHeader.responseFlag==comm_pb2.PhotoHeader.success):
                print "Image deleted successfully."
           else:
                print "Image was not deleted."
  