package ca.mcgill.ecse211.RingRetrieval;

import java.util.HashMap;

import ca.mcgill.ecse211.Ev3Boot.Ev3Boot;
import ca.mcgill.ecse211.Ev3Boot.MotorController;
import ca.mcgill.ecse211.Localization.Localizer;
import ca.mcgill.ecse211.Navigation.Navigator;
import ca.mcgill.ecse211.odometer.Odometer;
import ca.mcgill.ecse211.odometer.OdometerExceptions;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.SampleProvider;

/**
 * This is the classes's constructor. Inside this class is implemented the
 * searching method, which makes the robot turn around the tree until it detects
 * a ring.
 */
public class RingSearch extends MotorController{

	private static float[] usData = Ev3Boot.getUSData();
	private static SampleProvider usAverage = Ev3Boot.getUSAverage();
	private static int distance;
	private static HashMap<Integer, double[]> positionMap;
	private static Odometer odo;
	
	private static int color;
	private static int elevation;
	private static int ring_number = 0;


	/**
	 * This method makes the robot turn around the tree. The switch case decides
	 * which side the robot travels to first, based on the value of position.
	 * Position is computed inside the Ev3Boot class, where the distance between the
	 * robot and each side of the tree is computed, and the closest side is recorded
	 * in the position field. At each side of the tree, we call the colorDetection
	 * method from the checkColor class, which uses the light sensor to look for one
	 * of the four ring colors. Once the colorDetection method has returned, if the
	 * detected color is the one with the highest value it attempts to grasp the ring, 
	 * otherwise it keeps turning around the tree to find a higher value ring.
	 * 
	 * @param position: Which tree side is the closest side to the robot
	 * @param ringSet_x: The tree's x coordinate
	 * @param ringSet_y: The tree's y coordinate
	 */
	public static void turnAroundTree(int position, int ringSet_x, int ringSet_y) {
		// Initialize Hash Map with tree search positions and integer indicating visits.
		try {
			odo = Odometer.getOdometer();
		}
		catch (OdometerExceptions e) {
			//donothing;
		}

		positionMap = new HashMap<Integer, double[]>();
		positionMap.put(0, new double[] { ringSet_x - 0.1 , ringSet_y - 1, 0 });
		positionMap.put(1, new double[] { ringSet_x + 1, ringSet_y - 0.1, 0 });
		positionMap.put(2, new double[] { ringSet_x + 0.1, ringSet_y + 1, 0 });
		positionMap.put(3, new double[] { ringSet_x - 1, ringSet_y + 0.1, 0 });

		int getColor = 0;

		double[] posArray = positionMap.get(position);
		double[] odoPosition = odo.getXYT();
		int[] currentPosition = new int[3];
		currentPosition[0] = (int)Math.round(odoPosition[0] / Ev3Boot.getTileSize());
		currentPosition[1] = (int)Math.round(odoPosition[1] / Ev3Boot.getTileSize());

//		if(Math.sqrt(Math.pow(posArray[0] - currentPosition[0], 2) + Math.pow(posArray[1] - currentPosition[1], 2)) > 4) {
//			Navigator.travelTo(currentPosition[0] + (posArray[0] - currentPosition[0])/2, currentPosition[1] + (posArray[1] - currentPosition[1])/2, 4, true);
//		}
		System.out.println(position);
		System.out.println("Going To"+ posArray[0] +"," + posArray[1]);
//		Navigator.toStraightNavigator(posArray[0], posArray[1], 7);	
		switch(position)
		{
		case 0:
			Navigator.toStraightNavigator(posArray[0], posArray[1] + 0.5, 8);	
			break;
		case 1:
			Navigator.toStraightNavigator(posArray[0] - 0.5, posArray[1], 8);	
			break;
		case 2:
			Navigator.toStraightNavigator(posArray[0], posArray[1] - 0.5, 8);	
			break;
		case 3:
			Navigator.toStraightNavigator(posArray[0] + 0.5, posArray[1], 8);	
			break;
		
		}
//		Navigator.turnTo((360 - 90 * (position+1)) % 360);
//		forwardBy(-10);
//		
//		Navigator.travelUntil();
		
				
		turnTo((360 - 90 * position) % 360);
//		forwardBy(-1 * SENSOR_OFFSET);
		
		forwardBy(-0.5 * TILE_SIZE);
		
		Localizer.circleLocalize(posArray[0], posArray[1]);
		
		

		Navigator.travelUntil();
		
		CheckColor.colorDetection();
		
		color = CheckColor.getDetectedColor();
		elevation = CheckColor.getElevation();
		
		RingGrasp.grasp(color, elevation, ring_number);
		
		if(color != 0)
		{
			ring_number++;
		}

//		if (getColor != 0) {
//			beepColor(getColor);
//			Navigator.turnTo((360 - 90 * position) % 360);
//			forwardBy(-3);
//			Ev3Boot.getArmHook().rotateTo(-205);
//			Ev3Boot.getBigArmHook().rotateTo(CheckColor.getElevation() == 1 ? 102 : 32, false);
//			forwardBy(5);
//			Ev3Boot.getArmHook().rotateTo(-230);
//			forwardBy(20);
//			return;
//		}

		if (!travelPosition(position, position, (position + 3) % 4)) {
			travelPosition(position, position, (position + 5) % 4);
			return;
		}

	}

	public static boolean travelPosition(int sequenceStart, int currentPosition, int previousPosition) {
		System.out.println("Travelling");
		int i = ((currentPosition == 0 ? 4 : currentPosition) -  previousPosition) == 1 ? 1 : -1;
		int nextPosition = (currentPosition + 4 + i) % 4;
		int getColor;
		System.out.println("Next position: " + nextPosition);

		double[] posArray = positionMap.get(nextPosition);

		if (nextPosition == sequenceStart) {
			System.out.println(posArray[0] + "," + posArray[1]);
			System.out.println("Back at start");
			return true;
		}

		// Check that not already marked as blocked.;
		if (posArray[2] == 0) {
			if (!(posArray[0] > 0 && posArray[0] < 8 && posArray[1] > 0 && posArray[1] < 8)) {
				System.out.println(posArray[0] + "," + posArray[1]);
				System.out.println("Next position out of bounds: " + nextPosition);
				positionMap.remove(nextPosition);
				positionMap.put(nextPosition, new double[] { posArray[0], posArray[1], -1 });
				return false;
			}
			System.out.println("Going To"+posArray[0] +"," + posArray[1]);
			
			// Travel to position
			Navigator.travelTo(posArray[0], posArray[1] , 3, false);
			
			turnTo((360 - 90 * nextPosition+1) % 360);
			Navigator.travelUntil();
			forwardBy(-1 * SENSOR_OFFSET);
			
			// Turn towards tree
			turnTo((360 - 90 * (nextPosition)) % 360);
			forwardBy(-10);
			Navigator.travelUntil();
			
			CheckColor.colorDetection();
			
			color = CheckColor.getDetectedColor();
			elevation = CheckColor.getElevation();
			
			RingGrasp.grasp(color, elevation, ring_number);
			
			if(color != 0)
			{
				ring_number++;
			}

//			if (color != 0) {
//				beepColor(color);
//				Navigator.turnTo((360 - 90 * nextPosition) % 360);
//				forwardBy(3);
//				Ev3Boot.getArmHook().rotateTo(-205);
//				Ev3Boot.getBigArmHook().rotateTo(CheckColor.getElevation() == 1 ? 102 :32, false);
//				forwardBy(5);
//				Ev3Boot.getArmHook().rotateTo(-230);
//				forwardBy(20);
//				return true;
//			}
			if (!travelPosition(sequenceStart, nextPosition, currentPosition)) {
				double[] returnPosArray = positionMap.get(currentPosition);
				Navigator.travelTo(returnPosArray[0], returnPosArray[1], 7, false);
				return false;
			}
			else {
				return true;
			}
	    // If already marked as blocked, we've already attempted to visit all possible positions.
		} else {
			return true;
		}
	}
	
	public static void beepColor(int color) {
		for(int i = 0; i<5-color; i++) Sound.beep();
	}
}
