package warbot.Test;

import warbot.kernel.Food;

public class NoFoodExplorer extends CMExplorer{

	public NoFoodExplorer() {
		// TODO Auto-generated constructor stub
	}

	public void activate() {
		super.activate();
		role = 2;
	}

	int takeFood(Food p) {
		return super.takeFood(p);
	}

	public void doIt() {
		super.doIt();
		return;
	}

}
