/*
* Home.java -Warbot: robots battles in MadKit
* Copyright (C) 2000-2002 Fabien Michel, Jacques Ferber
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package warbot.kernel;

import java.awt.geom.AffineTransform;
import java.awt.Point;


public class Home extends BasicBody
{
	/**
	 * Resources are used to create new agents...
	 */
	private int resourcelevel=0;
	final public static int RESOURCEUNIT=800; // with 2 hamburger you may create a new agent..
	final protected static int CREATE=6;	//Creation of agent
	String createWhat=null;

    public Home(WarbotEnvironment theWorld, Brain b, String team)
    {
        super(theWorld,b,"command center",team,40,10000,300);
        setSpeed(2);
    }

    public Home(){
	    super();
        setSpeed(2);
		setDetectingRange(300);
		this.setEnergy(10000);
	}

	public Percept makePercept(double dx, double dy, double d){
	   Percept p = super.makePercept(dx,dy,d);
	   p.setPerceptType("Home");
	   return p;
	}

    public String getEntityInterfaceType()
    {
        return "CommandCenter";
    }

    void getMissileShot(int value)
    {
        energy-=value;
        getShot=true;
        //System.err.println("my energy is "+energy);
        if(energy<0)
            delete();
    }
    
    protected void tryEat()
    {
    	/*System.err.println(this.toString());
    	System.err.println(eatWhat.toString()); */
    	//System.err.println(":: distance  "+distanceFrom(eatWhat));
    	if (distanceFrom(eatWhat) < 3)
      	{
      		increaseResourceLevel(eatWhat.getEnergy());
      		//eating=true;
            eatWhat.delete();
      	}
      	eatWhat=null;
      }
    
    protected void increaseResourceLevel(int v)
      {
    	resourcelevel += v;
      }
    
    public int getResourceLevel(){return resourcelevel;}

    boolean showResourceLevel(){return true;}
    
    public void createAgent(String type){
    	createWhat = type;
        action = CREATE;
    }
    
    private void tryCreateAgent(){
    	double lx = this.getX();
    	double ly = this.getY();
    	if (createWhat != null){
    		try {
    			if (getResourceLevel()>= RESOURCEUNIT){
	    	        getStructure().getAgent().doCommand(new SEdit.NewNodeCommand(createWhat,new Point(0,0)));
	    	        BasicBody r = (BasicBody)((WarbotStructure)getStructure()).getLastEntity();
	    	        r.setHeading(Math.random()*360);
	    	       // r.setXY( (radius+r.getRadius()+1)*r.getCosAlpha()+x,(radius+r.getRadius()+1)*r.getSinAlpha()+y);
	    	        double[] pt = getPointToCreateAgent(r);
	    	        if (pt == null)
	    	        {
	    	        	System.err.println(":: "+this+" cannot create " + createWhat + " : no free area to spot!");
	    	        	return;
	    	        }
	    	        r.setXY(pt[0], pt[1]);
	    	        resourcelevel -= RESOURCEUNIT;
    			} else {
    				this.setUserMessage("Resources are too low");
    			}
    		} catch(ClassCastException ex){
    			System.err.println(":: "+this+" cannot create " + createWhat + " : this is not an agent!");
    		} catch(Exception e){
    			System.err.println(":: "+this+ " error in creating a "+createWhat);
    			System.err.println(e.getMessage());
    			e.printStackTrace();
    		}
    		createWhat = null;
    	}
    }

    void doAction()
    {
    	switch(action)
    	{
    		case EAT:tryEat();break;
    		case CREATE: tryCreateAgent(); break;
    	}
    }

    /**
     * Find a free point aroud the home. 
     * You can adjust precision and margin.
     *  
     * @param r the body of the agent being created
     * @return return a position if it exists else return null
     */
    private double[] getPointToCreateAgent(BasicBody r)
    {
    	double precision = Math.PI / 16;
    	int margin = 1;
    	boolean collision;
    	int minRadius = radius+r.getRadius()+1;
    	double angle = 0;
    	Percept[] percepts = getPercepts();    	
    	double[] pt = {x+minRadius, y};
    	
    	for (angle = 0; angle < 2*Math.PI; angle += precision) {
        	AffineTransform.getRotateInstance(angle, x, y).transform(pt, 0, pt, 0, 1);
	    	collision = false;
//	    	System.err.println("getPointToCreateAgent -- point : " + pt[0] + "," + pt[1]); // debug
        	for (Percept percept : percepts) {
				if (!(percept instanceof Crossable))
				{
					// debug
			    	//System.err.println("getPointToCreateAgent -- agent " + percept.getPerceptType() + " : " + (x + percept.x) + "," + (y + percept.y) + " ; " + percept.radius);
			    	//System.err.println("getPointToCreateAgent -- intersect : "
			    	//				+ (x + percept.x - (percept.getRadius() + margin)) + ","
			    	//				+ (x - percept.x + (percept.getRadius() + margin)) + ","
			    	//				+ (y + percept.y - (percept.getRadius() + margin)) + ","
			    	//				+ (y - percept.y + (percept.getRadius() + margin)));
					if(pt[0] + r.radius >= x + percept.x - (percept.getRadius() + margin)
					&& pt[0] - r.radius <= x + percept.x + (percept.getRadius() + margin)
					&& pt[1] + r.radius >= y + percept.y - (percept.getRadius() + margin)
					&& pt[1] - r.radius <= y + percept.y + (percept.getRadius() + margin))
						collision = true;
				}
			}
        	if (!collision)
        		return pt;
    	}    	
    	return null;
    }
}
