package poke.loadBalancer;

import java.util.HashMap;
import java.util.Map;
/*
 * this class is used for storing request count for each worker node.
 * This information is used to determine how much loaded any worker node is
 * This is one of the two factors by which we are determining how loaded 
 * any worker node is.
 * 
 * */
public class RequestCountMap {
	
	private static Map<Integer,Integer> reqCntMap = new HashMap<Integer, Integer>();

	public static Map<Integer, Integer> getReqCntMap() {
		return reqCntMap;
	}

	public static void setReqCntMap(Map<Integer, Integer> reqCntMap) {
		RequestCountMap.reqCntMap = reqCntMap;
	}

}
