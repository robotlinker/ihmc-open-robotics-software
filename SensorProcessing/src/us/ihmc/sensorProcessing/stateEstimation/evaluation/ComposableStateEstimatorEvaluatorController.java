package us.ihmc.sensorProcessing.stateEstimation.evaluation;

import java.util.Collection;

import javax.vecmath.Vector3d;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import us.ihmc.controlFlow.ControlFlowGraph;
import us.ihmc.controlFlow.ControlFlowOutputPort;
import us.ihmc.sensorProcessing.simulatedSensors.SensorMap;
import us.ihmc.sensorProcessing.stateEstimation.ComposableOrientationAndCoMEstimatorCreator;
import us.ihmc.sensorProcessing.stateEstimation.ComposableOrientationEstimatorCreator;
import us.ihmc.sensorProcessing.stateEstimation.DesiredCoMAndAngularAccelerationOutputPortsHolder;
import us.ihmc.sensorProcessing.stateEstimation.JointStateFullRobotModelUpdater;
import us.ihmc.sensorProcessing.stateEstimation.OrientationEstimator;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.AngularVelocitySensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.LinearAccelerationSensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.OrientationSensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.PointVelocitySensorConfiguration;
import us.ihmc.sensorProcessing.stateEstimation.sensorConfiguration.SensorConfigurationFactory;
import us.ihmc.utilities.math.MathTools;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.RigidBody;
import us.ihmc.utilities.screwTheory.SpatialAccelerationCalculator;
import us.ihmc.utilities.screwTheory.TwistCalculator;

import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.robotController.RobotController;

public class ComposableStateEstimatorEvaluatorController implements RobotController
{
   private static final boolean ESTIMATE_COM = true;

   private final double comAccelerationProcessNoiseStandardDeviation = Math.sqrt(1e-1);
   private final double angularAccelerationProcessNoiseStandardDeviation = Math.sqrt(1e-1);

   private final Vector3d gravitationalAcceleration;

   private final YoVariableRegistry registry = new YoVariableRegistry(getClass().getSimpleName());

   private final TwistCalculator estimatedTwistCalculator;
   private final SpatialAccelerationCalculator estimatedSpatialAccelerationCalculator;

   private final ControlFlowGraph controlFlowGraph;
   private final OrientationEstimator orientationEstimator;

   private final ComposableStateEstimatorEvaluatorErrorCalculator composableStateEstimatorEvaluatorErrorCalculator;

   public ComposableStateEstimatorEvaluatorController(StateEstimatorEvaluatorRobot robot, FullInverseDynamicsStructure robotStuffToEstimate, double controlDT,
           SensorMap sensorMap, DesiredCoMAndAngularAccelerationOutputPortsHolder desiredCoMAndAngularAccelerationOutputPortsHolder)
   {
      this.gravitationalAcceleration = new Vector3d();
      robot.getGravity(gravitationalAcceleration);

      estimatedTwistCalculator = robotStuffToEstimate.getTwistCalculator();
      estimatedSpatialAccelerationCalculator = robotStuffToEstimate.getSpatialAccelerationCalculator();

      SensorConfigurationFactory SensorConfigurationFactory = new SensorConfigurationFactory(gravitationalAcceleration);

      // Sensor configurations for estimator
      Collection<OrientationSensorConfiguration> orientationSensorConfigurations =
         SensorConfigurationFactory.createOrientationSensorConfigurations(sensorMap.getOrientationSensors());

      Collection<AngularVelocitySensorConfiguration> angularVelocitySensorConfigurations =
         SensorConfigurationFactory.createAngularVelocitySensorConfigurations(sensorMap.getAngularVelocitySensors());

      Collection<LinearAccelerationSensorConfiguration> linearAccelerationSensorConfigurations =
         SensorConfigurationFactory.createLinearAccelerationSensorConfigurations(sensorMap.getLinearAccelerationSensors());

      Collection<PointVelocitySensorConfiguration> pointVelocitySensorConfigurations =
         SensorConfigurationFactory.createPointVelocitySensorConfigurations(sensorMap.getPointVelocitySensors());

      controlFlowGraph = new ControlFlowGraph();
      JointStateFullRobotModelUpdater jointStateFullRobotModelUpdater = new JointStateFullRobotModelUpdater(controlFlowGraph, sensorMap,
                                                                           estimatedTwistCalculator, estimatedSpatialAccelerationCalculator);

      ControlFlowOutputPort<TwistCalculator> twistCalculatorOutputPort = jointStateFullRobotModelUpdater.getTwistCalculatorOutputPort();
      ControlFlowOutputPort<SpatialAccelerationCalculator> spatialAccelerationCalculatorOutputPort =
         jointStateFullRobotModelUpdater.getSpatialAccelerationCalculatorOutputPort();

      RigidBody estimationLink = robotStuffToEstimate.getEstimationLink();
      ReferenceFrame estimationFrame = estimationLink.getParentJoint().getFrameAfterJoint();

      DenseMatrix64F angularAccelerationNoiseCovariance = createDiagonalCovarianceMatrix(angularAccelerationProcessNoiseStandardDeviation, 3);

      if (ESTIMATE_COM)
      {
         DenseMatrix64F comAccelerationNoiseCovariance = createDiagonalCovarianceMatrix(comAccelerationProcessNoiseStandardDeviation, 3);

         ComposableOrientationAndCoMEstimatorCreator orientationEstimatorCreator =
            new ComposableOrientationAndCoMEstimatorCreator(angularAccelerationNoiseCovariance, comAccelerationNoiseCovariance, estimationLink,
               twistCalculatorOutputPort, spatialAccelerationCalculatorOutputPort);
         orientationEstimatorCreator.addOrientationSensorConfigurations(orientationSensorConfigurations);
         orientationEstimatorCreator.addAngularVelocitySensorConfigurations(angularVelocitySensorConfigurations);
         orientationEstimatorCreator.addLinearAccelerationSensorConfigurations(linearAccelerationSensorConfigurations);
         orientationEstimatorCreator.addPointVelocitySensorConfigurations(pointVelocitySensorConfigurations);

         updateInternalState();
         orientationEstimator = orientationEstimatorCreator.createOrientationEstimator(controlFlowGraph, controlDT,
                 robotStuffToEstimate.getRootInverseDynamicsJoint(), estimationLink, estimationFrame,
                 desiredCoMAndAngularAccelerationOutputPortsHolder.getDesiredAngularAccelerationOutputPort(),
                 desiredCoMAndAngularAccelerationOutputPortsHolder.getDesiredCenterOfMassAccelerationOutputPort(), registry);
      }
      else
      {
         ComposableOrientationEstimatorCreator orientationEstimatorCreator = new ComposableOrientationEstimatorCreator(angularAccelerationNoiseCovariance,
                                                                                estimationLink, twistCalculatorOutputPort);
         orientationEstimatorCreator.addOrientationSensorConfigurations(orientationSensorConfigurations);
         orientationEstimatorCreator.addAngularVelocitySensorConfigurations(angularVelocitySensorConfigurations);

         orientationEstimator = orientationEstimatorCreator.createOrientationEstimator(controlFlowGraph, controlDT, estimationFrame,
                 desiredCoMAndAngularAccelerationOutputPortsHolder.getDesiredAngularAccelerationOutputPort(), registry);
      }

      controlFlowGraph.initializeAfterConnections();

      this.composableStateEstimatorEvaluatorErrorCalculator = new ComposableStateEstimatorEvaluatorErrorCalculator(robot, orientationEstimator, registry);

      controlFlowGraph.visualize();

   }


   public void initialize()
   {
   }

   public YoVariableRegistry getYoVariableRegistry()
   {
      return registry;
   }

   public String getName()
   {
      return registry.getName();
   }

   public String getDescription()
   {
      return getName();
   }

   public void doControl()
   {
      updateInternalState();

      controlFlowGraph.startComputation();
      controlFlowGraph.waitUntilComputationIsDone();

      composableStateEstimatorEvaluatorErrorCalculator.computeErrors();
   }

   private void updateInternalState()
   {
      // TODO: Not sure if the following two should be here...
      estimatedTwistCalculator.compute();
      estimatedSpatialAccelerationCalculator.compute();
   }

   private static DenseMatrix64F createDiagonalCovarianceMatrix(double standardDeviation, int size)
   {
      DenseMatrix64F orientationCovarianceMatrix = new DenseMatrix64F(size, size);
      CommonOps.setIdentity(orientationCovarianceMatrix);
      CommonOps.scale(MathTools.square(standardDeviation), orientationCovarianceMatrix);

      return orientationCovarianceMatrix;
   }


   public OrientationEstimator getOrientationEstimator()
   {
      return orientationEstimator;      
   }

}
