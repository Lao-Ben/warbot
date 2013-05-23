package warbot.Test;


import warbot.kernel.Food;

public class FoodExplorer extends CMExplorer{

	public FoodExplorer() {
		// TODO Auto-generated constructor stub
	}

	public void activate() {
		super.activate();
		role = 1;
	}

	int takeFood(Food p) {
		return super.takeFood(p);
	}

	public void doIt() {
		super.doIt();
		return;
	}

}
