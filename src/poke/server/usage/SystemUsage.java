package poke.server.usage;

import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import poke.server.managers.HeartbeatManager;

import com.jezhumble.javasysmon.CpuTimes;
import com.jezhumble.javasysmon.JavaSysMon;
/*
 * this class runs a thread which constantly monitors current system usage and updates it in the 
 * heartbeat manage class so that it can embed it in the heartbeat to send it to load balancer node.
 * 
 */
public class SystemUsage extends Thread{
	
	public static AtomicReference<SystemUsage> su = new AtomicReference<SystemUsage>();
	
	public static SystemUsage init(){
		//System.out.println("system usage's inti is called");
		su.compareAndSet(null, new SystemUsage());
		return su.get();
	}
	
	public SystemUsage getInstance(){
		return su.get();
	}
	
	
	 public static void Initialize() {
		 
		 System.out.println("initialize is called");
	        final JavaSysMon mon = new JavaSysMon();

	        final java.util.Timer timer1 = new java.util.Timer();
	        timer1.schedule(new TimerTask() {
	            CpuTimes oldTime = mon.cpuTimes();
	            float[] usageCache = new float[1];
	            int count = 0;

	            @Override
	            public void run() {
	            	
	                CpuTimes newTime = mon.cpuTimes();
	                float val = newTime.getCpuUsage(oldTime);
	                usageCache[count] = val;
	                
	                if (count < (usageCache.length - 1)) {
	                    count++;
	                } else {
	                    count = 0;
	                    int sum = 0;
	                    for (int i = 0; i < usageCache.length; i++) {
	                        sum = sum + (int) (usageCache[i] * 100);
	                    }
	                    //updates CPU usage value in the heartbeat manager instance
	                    HeartbeatManager.getInstance().setCpuUsage(sum / usageCache.length);
	                }
	               
	                
	                long mem = (mon.physical().getTotalBytes() - mon.physical().getFreeBytes());
	                
	                
	                oldTime = newTime;
	            }
	        }, 100, 1000);

	    }

}
