package us.ihmc.darpaRoboticsChallenge.networkProcessor.depthData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

import us.ihmc.utilities.code.agileTesting.BambooAnnotations.EstimatedDuration;
import us.ihmc.utilities.math.geometry.AngleTools;

public class WheelRotationEstimatorTest
{
   private static final double eps = 1e-7;

	@EstimatedDuration
	@Test(timeout=300000)
   public void testAngleDefinitions()
   {
      int sectors = 370;
      WheelRotationEstimator wheelRotationEstimator = new WheelRotationEstimator(0.01,20,1e-8,sectors,0.3);
      Random random = new Random(10230L);
      for (int i = 0; i<1000; i++)
      {
         double testAngle = random.nextGaussian();
         double expectedAngle = AngleTools.trimAngleMinusPiToPi(testAngle);
         double y = Math.sin(expectedAngle);
         double z = -Math.cos(expectedAngle);
         double calculatedAngle = WheelRotationEstimator.toAngle(y,z);
         assertEquals(expectedAngle,calculatedAngle,eps+Math.PI*2 / (double) sectors);
      }
      assertEquals(0,wheelRotationEstimator.angleToIndex(0.0),eps);
      
   }

	@EstimatedDuration
	@Test(timeout=300000)
   public void testIndexBounds()
   {
      int sectors = 370;
      WheelRotationEstimator wheelRotationEstimator = new WheelRotationEstimator(0.01,20,1e-8,sectors,0.3);
      Random random = new Random(10230L);
      for (int i = 0; i<1000; i++)
      {
         double testAngle = random.nextGaussian()*7;
         double expectedAngle = AngleTools.trimAngleMinusPiToPi(testAngle);
         double y = Math.sin(expectedAngle);
         double z = -Math.cos(expectedAngle);
         double calculatedAngle = WheelRotationEstimator.toAngle(y,z);
         int index = wheelRotationEstimator.angleToIndex(calculatedAngle);
         assertTrue(index>=0);
         assertTrue(index<sectors);
      }
      assertEquals(0,wheelRotationEstimator.angleToIndex(0.0),eps);
      
   }

	@EstimatedDuration
	@Test(timeout=300000)
   public void testAngleTautology()
   {
      int sectors = 11100;
      WheelRotationEstimator wheelRotationEstimator = new WheelRotationEstimator(0.01,20,1e-8,sectors,0.3);
      Random random = new Random(10230L);
      for (int i = 0; i<10000; i++)
      {
         double testAngle = random.nextGaussian();
         double expectedAngle = AngleTools.trimAngleMinusPiToPi(testAngle);
         int index = wheelRotationEstimator.angleToIndex(expectedAngle);
         double calculatedAngle = wheelRotationEstimator.indexToAngle(index);
         assertEquals(expectedAngle,calculatedAngle,eps+Math.PI*2 / (double) sectors);
      }
      
      
   }

}
