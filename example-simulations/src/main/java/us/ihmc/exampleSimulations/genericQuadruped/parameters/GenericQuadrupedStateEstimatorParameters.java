package us.ihmc.exampleSimulations.genericQuadruped.parameters;

import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.robotics.sensors.FootSwitchFactory;
import us.ihmc.sensorProcessing.sensorProcessors.SensorProcessing;
import us.ihmc.sensorProcessing.sensorProcessors.SensorProcessing.SensorType;
import us.ihmc.sensorProcessing.simulatedSensors.SensorNoiseParameters;
import us.ihmc.sensorProcessing.stateEstimation.StateEstimatorParameters;
import us.ihmc.yoVariables.providers.DoubleProvider;

public class GenericQuadrupedStateEstimatorParameters extends StateEstimatorParameters
{
   private static final boolean ENABLE_SENSOR_NOISE = false;

   private final SensorNoiseParameters sensorNoiseParams = new SensorNoiseParameters();
   private final boolean runningOnRealRobot;
   private final double estimatorDT;

   public GenericQuadrupedStateEstimatorParameters(boolean runningOnRealRobot, double estimatorDT)
   {
      this.runningOnRealRobot = runningOnRealRobot;
      this.estimatorDT = estimatorDT;
   }

   @Override
   public void configureSensorProcessing(SensorProcessing sensorProcessing)
   {
      // imu filtering
      DoubleProvider angularVelocityAlphaFilter = sensorProcessing.createAlphaFilter("angularVelocityAlphaFilter", 20.0);
      DoubleProvider linearAccelerationAlphaFilter = sensorProcessing.createAlphaFilter("linearAccelerationAlphaFilter", 100.0);
      sensorProcessing.addSensorAlphaFilter(angularVelocityAlphaFilter, false, SensorType.IMU_ANGULAR_VELOCITY);
      sensorProcessing.addSensorAlphaFilter(linearAccelerationAlphaFilter, false, SensorType.IMU_LINEAR_ACCELERATION);

      if (!runningOnRealRobot && ENABLE_SENSOR_NOISE)
      {
         sensorNoiseParams.setOrientationMeasurementStandardDeviation(0.00001);
         sensorNoiseParams.setAngularVelocityMeasurementStandardDeviation(0.00001);
         sensorNoiseParams.setLinearAccelerationMeasurementStandardDeviation(0.00001);

         sensorNoiseParams.setJointVelocityMeasurementStandardDeviation(0.00001);
      }
   }


   @Override
   public SensorNoiseParameters getSensorNoiseParameters()
   {
      return sensorNoiseParams;
   }

   @Override
   public double getEstimatorDT()
   {
      return estimatorDT;
   }

   @Override
   public boolean isRunningOnRealRobot()
   {
      return runningOnRealRobot;
   }

   @Override
   public double getKinematicsPelvisPositionFilterFreqInHertz()
   {
      return Double.POSITIVE_INFINITY;
   }

   @Override
   public double getCoPFilterFreqInHertz()
   {
      return 4.0;
   }

   @Override
   public boolean useAccelerometerForEstimation()
   {
      return true;
   }

   @Override
   public boolean cancelGravityFromAccelerationMeasurement()
   {
      return true;
   }

   @Override
   public boolean enableIMUBiasCompensation()
   {
      return true;
   }

   @Override
   public boolean enableIMUYawDriftCompensation()
   {
      return true;
   }

   @Override
   public double getIMUBiasFilterFreqInHertz()
   {
      return 6.0e-3;
   }

   @Override
   public double getIMUYawDriftFilterFreqInHertz()
   {
      return 1.0e-3;
   }

   @Override
   public double getIMUBiasVelocityThreshold()
   {
      return 0.02;
   }

   @Override
   public double getPelvisPositionFusingFrequency()
   {
      return 0.5;
   }

   @Override
   public double getPelvisLinearVelocityFusingFrequency()
   {
      return 0.5;
   }

   @Override
   public double getDelayTimeForTrustingFoot()
   {
      return 0.02;
   }

   @Override
   public double getForceInPercentOfWeightThresholdToTrustFoot()
   {
      return 0.24;
   }

   @Override
   public boolean trustCoPAsNonSlippingContactPoint()
   {
      return false;
   }

   @Override
   public double getPelvisLinearVelocityAlphaNewTwist()
   {
      return 0.15;
   }

   // TODO Need to figure out how to get the quadruped to use this factory.
   @Override
   public FootSwitchFactory getFootSwitchFactory()
   {
      return null;
   }

   @Override
   public boolean requestFootForceSensorCalibrationAtStart()
   {
      return false;
   }

   @Override
   public SideDependentList<String> getFootForceSensorNames()
   {
      return null;
   }

   @Override
   public boolean getPelvisLinearStateUpdaterTrustImuWhenNoFeetAreInContact()
   {
      return true;
   }

   @Override
   public double getCenterOfMassVelocityFusingFrequency()
   {
      return 0.4261;
   }

   @Override
   public boolean useGroundReactionForcesToComputeCenterOfMassVelocity()
   {
      return false;
   }

   @Override
   public boolean correctTrustedFeetPositions()
   {
      return true;
   }

   @Override
   public boolean requestFrozenModeAtStart()
   {
      return true;
   }

}
