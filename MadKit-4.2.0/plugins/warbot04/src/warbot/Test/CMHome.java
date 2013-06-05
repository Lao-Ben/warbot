package warbot.Test;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import madkit.kernel.AgentAddress;

import com.sun.tools.javac.util.Pair;
import com.sun.tools.jdi.LinkedHashMap;

import warbot.kernel.*;

/**
 * @author PeYoTlL
 *
 */
public class CMHome extends Brain
{
	String groupName="warbot-";
	final static String roleName="Home";

	LinkedHashMap rcvFoodList;
	LinkedHashMap sntFoodList;
	LinkedHashMap lastCollectors;
	final static int stepsBeforeReply 		= 4;
	final static int stepsToKeepCollectors 	= 6;
	
	public CMHome(){
		rcvFoodList		= new LinkedHashMap();
		sntFoodList 	= new LinkedHashMap();
		lastCollectors 	= new LinkedHashMap();
	}
	
	void eatFood(Food p){
		if(distanceTo(p) < 3){
			eat((Food)p);
		}
	}

	public void activate()
	{
		groupName=groupName+getTeam();
		println("Base Test opérationnelle");		
		createGroup(false,groupName,null,null);
		requestRole(groupName,roleName,null);
	}

		
	/**
	 * Store a food entity percepted by an explorer
	 * 
	 * @param id the id of the food entity
	 * @param posX
	 * @param posY
	 */
	private void storeFood(String id, double posX, double posY) {
		rcvFoodList.put(id, new Point2D.Double(posX, posY));
	}
	
	/**
	 * Send messages to ask explorers to take a food
	 * 
	 * 1. search the first available food in the reveived food list
	 * 2. broadcast a message to tell to explorer to collect that food
	 * 3. store a record in a list (used later to know if an explorer handled that food)
	 */
	private void sendMsgForFirstFreeFood() {
		if (!rcvFoodList.isEmpty()) {
			String foodId = null;
			Point2D foodPosition = null;
			//println("Home send msg STEP1");
			for (Object key : rcvFoodList.keySet()) {
				foodId = (String) key;
			//	println("Home send msg STEP2 key : " + foodId + " / " + rcvFoodList);
				if (sntFoodList.get(key) == null) {
			//		println("Home send msg STEP3");
					foodPosition = (Point2D)rcvFoodList.get(key);		
				}
			}
			//println("Home send msg STEP4");
			if (foodPosition != null)
			{
				//println("Home send msg STEP4");
				broadcast(groupName, "Explorer", Constants.MSG_TAKEFOOD, new String[] {
						Double.toString(foodPosition.getX()),
						Double.toString(foodPosition.getY()),
						foodId});
				//println("Home send msg STEP5");
				sntFoodList.put(foodId, new Integer(0));
			}
		//	println("Home send msg STEP6");
		}
	}
	
	
	/**
	 * Check if food should be marked as available
	 * 
	 * 1. increments the counter of the records
	 * 2. if the counter is over stepsBeforeReply, remove the record
	 * 	  (so the food is marked as available for other)
	 */
	private void manageSentFoodOrders() {
		if (sntFoodList == null || sntFoodList.isEmpty())
			return;
		
		for (Object key : sntFoodList.keySet()) {
			Integer stepsCount = (Integer) sntFoodList.get(key);
			if (stepsCount++ > stepsBeforeReply)
				sntFoodList.remove(key);
			;
		}
	}
	
	
	/**
	 * 
	 * 
	 * 1. Removes the food from lists
	 * 2. Send confirmation to the explorer
	 * 
	 * @param id the id of the food entity
	 */
	private void letExplorersHandleFood(LinkedHashMap candidates) {
		if (candidates == null || candidates.isEmpty())
			return;
		
		// iterate on foods
		for (Object foodId : candidates.keySet()) {
			
			if (!sntFoodList.containsKey(foodId))
				continue;
			
			@SuppressWarnings("unchecked")
			ArrayList<AgentAddress> agents = (ArrayList<AgentAddress>) candidates.get(foodId);
			AgentAddress collector = null;

			// iterate on collectors
			for (AgentAddress candidate : agents) {
				// take already used collector
				if (lastCollectors.containsKey(candidate)) {
					collector = candidate;
					Integer stepsCount = (Integer) lastCollectors.get(candidate); 
					stepsCount = 0;
					break;
				}
			}
		
			// if not, take the first candidate
			if (collector == null) {
				collector = agents.get(0);
				lastCollectors.put(collector, new Integer(0));
			}
			Point2D foodPosition = (Point2D)rcvFoodList.get(foodId);		
			send(collector, Constants.MSG_TAKINGFOODACK, new String[] {
					Double.toString(foodPosition.getX()),
					Double.toString(foodPosition.getY()),
					(String) foodId});
			
			sntFoodList.remove(foodId);
			rcvFoodList.remove(foodId);
		}
	}
	
	/**
	 * Check if collectors should be removed from buffer
	 * 
	 * 1. increments the counter of the records
	 * 2. if the counter is over stepsToKeepCollectors, remove the record
	 */
	private void manageLastCollectors() {
		if (lastCollectors == null || lastCollectors.isEmpty())
			return;
		
		for (Object key : lastCollectors.keySet()) {
			Integer stepsCount = (Integer) lastCollectors.get(key);
			if (stepsCount++ > stepsToKeepCollectors)
				sntFoodList.remove(key);
			;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void doIt()
	{
		int distanceDetectionHome 		= 200;		// périmètre de prise en compte pour base ennemie
		int distanceDetectionExplorer 	= 150;		// périmètre de prise en compte pour explorer ennemi
		int distanceDetectionLauncher 	= 80*2;		// périmètre de prise en compte pour launcher ennemi
		int distanceSecurite 			= 150;		// périmètre de sécurité
		int nbEnnemisProches 			= 0;
		int nbAmisProches 				= 0;
		int nbExplorer					= 0;
		int nbLauncher					= 0;
		
		String actMessageAideL 			= "HELP-BL";// cas où risque d'attaque ennemie des launchers
		String actMessageAideE 			= "HELP-BE";// cas où risque d'attaque ennemie car explorers rôdent
		String actMessageAttaque 		= "ATAQ";	// cas de détection de la base ennemie
		String argMessageX 				= "";		// pour envoyer position relative en X de l'ennemi
		String argMessageY 				= "";		// pour envoyer position relative en Y de l'ennemi
		String argLauncherX				= "";		// pour envoyer position relative en X de l'ennemi
		String argLauncherY				= "";		// pour envoyer position relative en Y de l'ennemi
		
		WarbotMessage currentMsg		= null;
		
		LinkedHashMap currentCollectors = new LinkedHashMap();
		
		while((currentMsg = readMessage())!= null)
		{
			if(currentMsg.getAct() != null)
			{
				if(currentMsg.getAct() == "ExplorerAlive") {
					nbExplorer++;
				} else if(currentMsg.getAct() == "LauncherAlive") {
					nbLauncher++;
				} else if (currentMsg.getAct() == Constants.MSG_FOODFOUND) {
					// add food to the LinkedHashMap
					storeFood(currentMsg.getArgN(3),
							currentMsg.getFromX() + Double.valueOf(currentMsg.getArg1()),
							currentMsg.getFromY() + Double.valueOf(currentMsg.getArg2()));
/*				} else if(currentMsg.getAct() == "FOODEATEN") {
 					if (currentMsg.getArg1() != null)
 						foodList.remove(currentMsg.getArg1());*/
				} else if (currentMsg.getAct() == Constants.MSG_TAKINGFOOD) {
					String foodId = currentMsg.getArg1();
					if (currentCollectors.containsKey(foodId))
						((ArrayList<AgentAddress>) currentCollectors.get(foodId)).add(currentMsg.getSender());
					else
						currentCollectors.put(foodId, new ArrayList<AgentAddress>(Arrays.asList(currentMsg.getSender())));
				}
			}
		}

		println("Home Handling BEGIN");

		letExplorersHandleFood(currentCollectors);
//		println("Home Handling STEP1");
		
		manageSentFoodOrders();
//		println("Home Handling STEP2");
		manageLastCollectors();
//		println("Home Handling STEP3");
		sendMsgForFirstFreeFood();

//		println("Home Handling END");
		
		setUserMessage(Integer.toString(nbExplorer));
		
		broadcast(groupName,"Explorer","basepos");
		broadcast(groupName,"Launcher","basepos");
		Percept[]objetsPercus = getPercepts();	// entités dans le périmètre de perception
		
		for(int i=0;i<objetsPercus.length;i++)  // pour toutes les entités perçues...
		{
			Percept objetCourant = objetsPercus[i];
			// on observe les "ennemis"
			if (!objetCourant.getTeam().equals(getTeam()))
			{
				// si la base percoit la base ennemie
				if(objetCourant.getPerceptType().equals("Home") && distanceTo(objetCourant) < distanceDetectionHome)
				{
					argMessageX = Double.toString(objetCourant.getX());	
					argMessageY = Double.toString(objetCourant.getY());
					broadcast(groupName,"Launcher",actMessageAttaque,argMessageX,argMessageY);
//					println("Base ennemie détectée");
				}
				// si la base percoit des explorateurs ennemis
				if(objetCourant.getPerceptType().equals("Explorer") && distanceTo(objetCourant) < distanceDetectionExplorer)
				{
					argMessageX = Double.toString(objetCourant.getX());	
					argMessageY = Double.toString(objetCourant.getY());
					broadcast(groupName,"Launcher",actMessageAideE,argMessageX,argMessageY);
//					println("Explorer ennemi détecté");
				}
				// si la base percoit des launchers ennemis
				if(objetCourant.getPerceptType().equals("RocketLauncher") && distanceTo(objetCourant) < distanceDetectionLauncher)
				{
//					println("Launcher ennemi détecté");
					nbEnnemisProches ++;
					argLauncherX = Double.toString(objetCourant.getX());
					argLauncherY = Double.toString(objetCourant.getY());
				}
			}
			if (objetCourant.getPerceptType().equals("Food")) // si nourriture
			{
				setUserMessage("eating");
				eatFood((Food) objetCourant);
			}
			// on regarde le nombre de nos launchers "proches"
			if (objetCourant.getTeam().equals(getTeam()) && objetCourant.getPerceptType().equals("RocketLauncher") && distanceTo(objetCourant) < distanceSecurite)	
				nbAmisProches ++;
		}
		// appel à l'aide que si nombre launchers ennemis >= nombre launchers amis
		if (nbAmisProches <= nbEnnemisProches && nbEnnemisProches != 0)
		{
//			println("HELP ! nb ennemis : "+nbEnnemisProches);
			broadcast(groupName,"Launcher",actMessageAideL,argLauncherX,argLauncherY);

			if (getResourceLevel() >= 800)
				createAgent("TestRocketLauncher");
			return;
		}
		if (getResourceLevel() >= 800)
		{
			if (nbLauncher > nbExplorer && nbExplorer < 10)
				createAgent("TestExplorer");
			else
				createAgent("TestRocketLauncher");
		}
			
	}
}
