package poke.server.usage;

import java.io.IOException;

public class changeIp {
	private static String oldHost ;

	public static void changeip() {

		String[] command1 = { "/bin/bash", "-c",
				"echo \"Meena.1990\" | sudo -S ifconfig eth0 192.168.0.100 netmask 255.255.255.0" };
		try {
			Process pp = java.lang.Runtime.getRuntime().exec(command1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void resetip() {
		System.out.println(" reset ip is called");
		String[] command1 = {
				"/bin/bash",
				"-c",
				"echo \"jerryroot\" | sudo -S ifconfig eth0 " + oldHost
						+ " netmask 255.255.255.0" };
		try {
			Process pp = java.lang.Runtime.getRuntime().exec(command1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		changeip();
	}

}
