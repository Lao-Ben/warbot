package warbot.Test;

import java.util.ArrayList;
import java.util.List;

import warbot.kernel.*;

public class CMExplorer extends Brain
{
	String groupName	="warbot-";
	String roleName		="Explorer";
	
	int memeDirection	=30;
	int tempsMax		=8;
	int temps			=tempsMax;	// variable permettant de garder la meme direction pendant tempsMax itérations
	Percept myhome = null;
	int cap = 0;
	int i = 0;
	List<double[]> tabfoodtake = new ArrayList<double[]>();
	List<double[]> tabfoodtakebyme = new ArrayList<double[]>();

	public CMExplorer(){}

	public void activate()
	{
		groupName		=groupName+getTeam();  // -> warbot-CM
		randomHeading();  // direction aléatoire
		println("Explorateur Test opérationnel");
		createGroup(false,groupName,null,null);
		requestRole(groupName,roleName,null);
		requestRole(groupName,"mobile",null);
		this.setUserMessage((new Integer(this.bagSize())).toString());
		cap = this.getBagCapacity();
	}
	
	public void decompteTemps()
	{
		temps--;	
	}

	boolean takeFood(Food p){
		if(distanceTo(p) < 2){
			if(Math.random()<.1)
				eat((Food)p);
			else
				take((Food) p);
			return true;
		  } else {
		     setHeading(towards(p.getX(),p.getY()));
		     move();
		     return false;
		  }
	}
	
	void dropallpers()
	{
		for (int i = bagSize()-1; i >= 0; i--)
		{
			drop(i);
		}
	}
	
	public void doIt()
	{		
		String chaineAide		="HELP-E";
		String chaineAtak		="ATAQ";
		double positionEnnemiX 	= 0;
		double positionEnnemiY 	= 0;
		String ennemiX 			= "";
		String ennemiY 			= "";
		WarbotMessage messCourant	= null;
				
		if (!isMoving())	// si bloqué : direction aléatoire
			randomHeading();
		
		while((messCourant = readMessage())!= null)
		{
			// message d'attaque de base ennemie : les coordonnées sont toujours présentes en argument du message
			if(messCourant.getAct() != null && messCourant.getAct() == "TAKEFOOD")
			{
				double[] d = new double[2];
				d[0] = Double.parseDouble(messCourant.getArg1());
				d[1] = Double.parseDouble(messCourant.getArg2());
				if (!tabfoodtakebyme.contains(d))
					tabfoodtake.add(d);
			}
		}
		
		/*if (!isMoving() && myhome != null && bagSize() == getBagCapacity())
		{
			move();
			return;
		}*/
		
		Percept[]objetsPercus = getPercepts();  // entités dans le périmètre de perception
		for(int i=0;i<objetsPercus.length;i++)  // pour toutes les entités perçues...
		{
			Percept objetCourant = objetsPercus[i];
			if (objetCourant.getTeam().equals(getTeam()) && objetCourant.getPerceptType().equals("Home")) // si objet courant = base allié
			{
				myhome = objetCourant;
				break;
			}
			if (myhome == objetCourant)
				break;
		}
		if (myhome != null && bagSize() == (getBagCapacity()))
		{
			if(distanceTo(myhome) < 2){
	             setUserMessage("drop all");
	             dropAll();
	             setUserMessage((new Integer(this.bagSize())).toString());
	             setHeading(towards(-myhome.getX(),-myhome.getY()));
	             move();
	             return;
			} else {
	             setHeading(towards(myhome.getX(),myhome.getY()));
	             setUserMessage("going to base");
	             move();
	             return;
			}
		}
		
		// 1. Si base ennemie trouvée : broadcast
		for(int i=0;i<objetsPercus.length;i++)  // pour toutes les entités perçues...
		{
			Percept objetCourant = objetsPercus[i];
			if (!objetCourant.getTeam().equals(getTeam()) && objetCourant.getPerceptType().equals("Home")) // si objet courant = base ennemie
			{
				ennemiX = Double.toString(objetCourant.getX());	
				ennemiY = Double.toString(objetCourant.getY());				
				broadcast(groupName,"Launcher",chaineAtak,ennemiX,ennemiY);
			}
		}		
			
		// 2. Si attaqué : demande d'aide sans argument
		if (getShot())
		{
			println("explorer attaqué");
			broadcast(groupName,"Launcher",chaineAide,"0","0");			
		}
			
		// 3. si rocketlauncher ennemi repéré
		for(int i=0;i<objetsPercus.length;i++)  // pour toutes les entités perçues...
		{
			Percept objetCourant = objetsPercus[i];
			if (!objetCourant.getTeam().equals(getTeam()) && objetCourant.getPerceptType().equals("RocketLauncher")) // si RocketLauncher ennemi
			{
				// stratégie d'évitement
				positionEnnemiX = objetCourant.getX();	// abscisse de l'ennemi
				positionEnnemiY = objetCourant.getY();	// ordonnée de l'ennemi
				ennemiX = Double.toString(objetCourant.getX());	
				ennemiY = Double.toString(objetCourant.getY());				
//				println("launcher ennemi repéré");
//				println(ennemiX);
//				println(ennemiY);	
				broadcast(groupName,"Launcher",chaineAide,ennemiX,ennemiY);
				for(int j=0;j<objetsPercus.length;j++)  // pour toutes les entités perçues...
				{
					Percept objetCourant2 = objetsPercus[j];
					if ((getHeading()-(towards(objetCourant2.getX(),objetCourant.getY()))>=20) && (objetCourant2.getPerceptType().equals("Obstacle")))
					// si détecte un obstacle et que cet obstacle est dans le champ de la direction de l'agent	
					{
						if (temps==tempsMax)	// si on vient de repérer l'obstacle
						{
							decompteTemps();
							setHeading(towards(-positionEnnemiX/2,-positionEnnemiY)); // on évite ET l'ennemi, Et l'obstacle
						}
						else	
						{
							if (temps==0)
								{temps=tempsMax;}
							else	// temps entre 8 et 0
								{decompteTemps();}		
						}
						move();
						return;
					}					
				}	
				setHeading(towards(-positionEnnemiX,-positionEnnemiY)); // direction opposée à l'ennemi repéré
			}
		}
		
		//5. Si nouriture
			for(int i=0;i<objetsPercus.length;i++)  // pour toutes les entités perçues...
			{
				Percept objetCourant = objetsPercus[i];
				if (objetCourant.getPerceptType().equals("Food")) // si nourriture
				{
					double[] d = new double[2];
					d[0] = objetCourant.getX();
					d[1] = objetCourant.getY();
					if (bagSize() != getBagCapacity() && !tabfoodtake.contains(d))
					{
						boolean b = takeFood((Food) objetCourant);
						/*if(b)
							broadcast(groupName,"Explorer","TAKEFOOD",Double.toString(objetCourant.getX()),Double.toString(objetCourant.getY()));*/
						setUserMessage((new Integer(this.bagSize())).toString());
						broadcast(groupName,"Explorer","TAKEFOOD",Double.toString(objetCourant.getX()),Double.toString(objetCourant.getY()));
						tabfoodtakebyme.add(d);
						return;
					}
				}
			}
		
		//6. déplacement aléatoire (randomHeading au début)
		temps=tempsMax;	//remise à 0 du compteur : on est hors de risque...
		move();
		return;			
	}
}