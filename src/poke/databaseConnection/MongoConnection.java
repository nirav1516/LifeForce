package poke.databaseConnection;
/*
 * we are using mongoDB as our backend database for storing images
 * this class us used to create connection with mongoDB server and also 
 * to read, write and delete images 
 * */
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.bson.NewBSONDecoder;
import org.h2.engine.DbObject;

import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;

public class MongoConnection {
	DBCollection collection;

	protected static AtomicReference<MongoConnection> instance = new AtomicReference<MongoConnection>();

	private static ServerConf conf;

	public static MongoConnection initConnection(ServerConf conf)
			throws UnknownHostException {
		MongoConnection.conf = conf;
		instance.compareAndSet(null, new MongoConnection());
		return instance.get();
	}

	public static MongoConnection getInstance() {
		
		return instance.get();
	}

	public MongoConnection() throws UnknownHostException {
		
		//MongoClient mongo = new MongoClient("localhost" , 27017);
		
		ArrayList<ServerAddress> addr = new ArrayList<ServerAddress>();
		addr.add(new ServerAddress("192.168.0.103:27017"));

		for (NodeDesc nn : conf.getAdjacent().getAdjacentNodes().values()) {
			System.out.println("connection string " + nn.getHost() + ":27017");
			addr.add(new ServerAddress(nn.getHost() + ":27017"));
		}

		
		MongoClient mongo = new MongoClient( addr);

		ReadPreference preference = ReadPreference.secondaryPreferred();
		mongo.setReadPreference(preference);
		mongo.slaveOk();
		
		DB db1 = mongo.getDB("lifeforce_rep");
		collection = db1.getCollection("images_rep");
	}

	public void insert(UUID uuid, byte[] imagebytes, String name) {
		BasicDBObject dbobj = new BasicDBObject();
		dbobj.append("UUID", uuid.toString()).append("Image", imagebytes)
				.append("Name", name);
		collection.insert(dbobj);
	}

	public ImageObject read(ImageObject img) {
		try{
		DBObject obj = collection.findOne(new BasicDBObject("UUID", img
				.getUuid()));
		
		byte[] imagedata = (byte[]) obj.get("Image");
		System.out.println("Photo received: " + imagedata);
		String name = (String) obj.get("Name");
		img.setImageData(imagedata);
		img.setName(name);
		return img;
		}catch(NullPointerException ne){
			return null;
		}
	}

	public boolean delete(String uuid) {
		BasicDBObject query = new BasicDBObject();
		query.append("UUID", uuid);
		DBObject obj = collection.findOne(new BasicDBObject("UUID", uuid));
		try {
			byte[] imagedata = (byte[]) obj.get("Image");

			System.out.println("Image found");
			WriteResult wr = collection.remove(query);

		} catch (NullPointerException e) {
			System.out.println("Image not found cant delte");
			return false;
		}

		try {
			obj = collection.findOne(new BasicDBObject("UUID", uuid));

			byte[] imagedata = (byte[]) obj.get("Image");

			return false;

		} catch (NullPointerException e) {
			System.out.println("image deleted successfully");
			return true;
		}

	}
}
