package poke.databaseConnection;

import java.util.UUID;
//pojo class for image objects
public class ImageObject {

	byte[] imageData;
	public byte[] getImageData() {
		return imageData;
	}

	public void setImageData(byte[] imageData) {
		this.imageData = imageData;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	String name;
	String uuid;
	
	public ImageObject() {
		// TODO Auto-generated constructor stub
	}
}
