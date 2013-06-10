package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual;

import com.yobotics.simulationconstructionset.DoubleYoVariable;
import com.yobotics.simulationconstructionset.EnumYoVariable;
import com.yobotics.simulationconstructionset.YoVariableRegistry;
import com.yobotics.simulationconstructionset.util.graphics.DynamicGraphicObjectsListRegistry;
import com.yobotics.simulationconstructionset.util.statemachines.*;
import com.yobotics.simulationconstructionset.util.trajectory.DoubleTrajectoryGenerator;
import com.yobotics.simulationconstructionset.util.trajectory.PositionTrajectoryGenerator;
import us.ihmc.commonWalkingControlModules.controlModules.RigidBodySpatialAccelerationControlModule;
import us.ihmc.commonWalkingControlModules.controlModules.SE3PDGains;
import us.ihmc.commonWalkingControlModules.controllers.HandControllerInterface;
import us.ihmc.commonWalkingControlModules.dynamics.FullRobotModel;
import us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.manipulation.individual.states.*;
import us.ihmc.commonWalkingControlModules.momentumBasedController.MomentumBasedController;
import us.ihmc.commonWalkingControlModules.sensors.MassMatrixEstimatingToolRigidBody;
import us.ihmc.commonWalkingControlModules.trajectories.*;
import us.ihmc.robotSide.RobotSide;
import us.ihmc.utilities.math.geometry.FramePose;
import us.ihmc.utilities.math.geometry.ReferenceFrame;
import us.ihmc.utilities.screwTheory.*;

import javax.media.j3d.Transform3D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.yobotics.simulationconstructionset.util.statemachines.StateMachineTools.addRequestedStateTransition;

public class IndividualHandControlModule
{
   private final YoVariableRegistry registry;

   private final StateMachine<IndividualHandControlState> stateMachine;
   private final Map<ReferenceFrame, RigidBodySpatialAccelerationControlModule> handSpatialAccelerationControlModules;
   private final MassMatrixEstimatingToolRigidBody toolBody;

   private final ChangeableConfigurationProvider initialConfigurationProvider;
   private final ChangeableConfigurationProvider finalConfigurationProvider;

   private final Map<OneDoFJoint, OneDoFJointQuinticTrajectoryGenerator> quinticPolynomialTrajectoryGenerators;

   private final Map<ReferenceFrame, StraightLinePositionTrajectoryGenerator> straightLinePositionWorldTrajectoryGenerators;
   private final Map<ReferenceFrame, OrientationInterpolationTrajectoryGenerator> orientationInterpolationWorldTrajectoryGenerators;
   private final YoVariableDoubleProvider trajectoryTimeProvider;

   private final ConstantPositionTrajectoryGenerator holdPositionInBaseTrajectoryGenerator;
   private final ConstantOrientationTrajectoryGenerator holdOrientationInBaseTrajectoryGenerator;

   private final TaskspaceHandPositionControlState taskspaceHandPositionControlState;
   private final JointSpaceHandControlControlState jointSpaceHandControlState;
   private final LoadBearingCylindricalHandControlState loadBearingCylindricalState;
   private final ObjectManipulationState objectManipulationState;
   private final LoadBearingPlaneHandControlState loadBearingPlaneFingersBentBackState;
   private final List<TaskspaceHandPositionControlState> taskSpacePositionControlStates = new ArrayList<TaskspaceHandPositionControlState>();

   private final EnumYoVariable<IndividualHandControlState> requestedState;
   private final HandControllerInterface handController;
   private final OneDoFJoint[] oneDoFJoints;
   private final GeometricJacobian jacobian;
   private final String name;
   private final TwistCalculator twistCalculator;
   private final FullRobotModel fullRobotModel;
   private final SE3PDGains defaultGains = new SE3PDGains();

   public IndividualHandControlModule(final DoubleYoVariable simulationTime, final RobotSide robotSide, FullRobotModel fullRobotModel,
                                      final TwistCalculator twistCalculator, final DynamicGraphicObjectsListRegistry dynamicGraphicObjectsListRegistry,
                                      HandControllerInterface handController, double gravity, final double controlDT,
                                      MomentumBasedController momentumBasedController, GeometricJacobian jacobian, final YoVariableRegistry parentRegistry)
   {
      RigidBody endEffector = jacobian.getEndEffector();

      name = endEffector.getName() + getClass().getSimpleName();
      registry = new YoVariableRegistry(name);
      this.twistCalculator = twistCalculator;
      this.fullRobotModel = fullRobotModel;
      this.handController = handController;

      defaultGains.set(100.0, 1.0, 100.0, 1.0);

      oneDoFJoints = ScrewTools.filterJoints(jacobian.getJointsInOrder(), OneDoFJoint.class);

      requestedState = new EnumYoVariable<IndividualHandControlState>(name + "RequestedState", "", registry, IndividualHandControlState.class, true);
      requestedState.set(null);

      trajectoryTimeProvider = new YoVariableDoubleProvider(name + "TrajectoryTime", registry);

      quinticPolynomialTrajectoryGenerators = new LinkedHashMap<OneDoFJoint, OneDoFJointQuinticTrajectoryGenerator>();

      for (OneDoFJoint oneDoFJoint : oneDoFJoints)
      {
         OneDoFJointQuinticTrajectoryGenerator trajectoryGenerator = new OneDoFJointQuinticTrajectoryGenerator(oneDoFJoint.getName() + "Trajectory",
                                                                        oneDoFJoint, trajectoryTimeProvider, registry);
         quinticPolynomialTrajectoryGenerators.put(oneDoFJoint, trajectoryGenerator);
      }

      if (handController != null)
         this.toolBody = new MassMatrixEstimatingToolRigidBody(name + "Tool", handController.getWristJoint(), fullRobotModel, gravity, controlDT, registry,
                 dynamicGraphicObjectsListRegistry);
      else
         this.toolBody = null;

      this.jacobian = jacobian;

      stateMachine = new StateMachine<IndividualHandControlState>(name, name + "SwitchTime", IndividualHandControlState.class, simulationTime, registry);

      handSpatialAccelerationControlModules = new LinkedHashMap<ReferenceFrame, RigidBodySpatialAccelerationControlModule>();

      ReferenceFrame endEffectorFrame = jacobian.getEndEffectorFrame();
      initialConfigurationProvider = new ChangeableConfigurationProvider(new FramePose(endEffectorFrame));
      finalConfigurationProvider = new ChangeableConfigurationProvider(new FramePose(endEffectorFrame));    // FIXME: make Yo, but is difficult because frame can change

      straightLinePositionWorldTrajectoryGenerators = new LinkedHashMap<ReferenceFrame, StraightLinePositionTrajectoryGenerator>();
      orientationInterpolationWorldTrajectoryGenerators = new LinkedHashMap<ReferenceFrame, OrientationInterpolationTrajectoryGenerator>();

      holdPositionInBaseTrajectoryGenerator = new ConstantPositionTrajectoryGenerator(name + "HoldPosition", jacobian.getBaseFrame(),
              initialConfigurationProvider, 0.0, registry);
      holdOrientationInBaseTrajectoryGenerator = new ConstantOrientationTrajectoryGenerator(name + "HoldOrientation", jacobian.getBaseFrame(),
              initialConfigurationProvider, 0.0, registry);

      loadBearingCylindricalState = new LoadBearingCylindricalHandControlState(IndividualHandControlState.LOAD_BEARING_CYLINDRICAL, momentumBasedController,
              jacobian, fullRobotModel.getElevator(), parentRegistry, robotSide);

      loadBearingPlaneFingersBentBackState = new LoadBearingPlaneHandControlState(IndividualHandControlState.LOAD_BEARING_PLANE_FINGERS_BENT_BACK, robotSide,
              momentumBasedController, jacobian, fullRobotModel.getElevator(), handController, registry);

      jointSpaceHandControlState = new JointSpaceHandControlControlState(IndividualHandControlState.JOINT_SPACE, robotSide, jacobian, momentumBasedController,
              registry, 1.0);

      objectManipulationState = new ObjectManipulationState(IndividualHandControlState.OBJECT_MANIPULATION, robotSide, momentumBasedController, jacobian,
              handController, toolBody, dynamicGraphicObjectsListRegistry, parentRegistry);

      taskspaceHandPositionControlState = new TaskspaceHandPositionControlState(IndividualHandControlState.TASK_SPACE_POSITION, robotSide,
              momentumBasedController, jacobian, dynamicGraphicObjectsListRegistry, registry);

      addRequestedStateTransition(requestedState, false, jointSpaceHandControlState, taskspaceHandPositionControlState);
      addRequestedStateTransition(requestedState, false, jointSpaceHandControlState, objectManipulationState);
      addRequestedStateTransition(requestedState, false, jointSpaceHandControlState, jointSpaceHandControlState);

      addRequestedStateTransition(requestedState, false, taskspaceHandPositionControlState, objectManipulationState);
      addRequestedStateTransition(requestedState, false, taskspaceHandPositionControlState, jointSpaceHandControlState);
      addRequestedStateTransition(requestedState, false, taskspaceHandPositionControlState, taskspaceHandPositionControlState);

      addRequestedStateTransition(requestedState, false, objectManipulationState, jointSpaceHandControlState);
      addRequestedStateTransition(requestedState, false, objectManipulationState, taskspaceHandPositionControlState);
      addRequestedStateTransition(requestedState, false, objectManipulationState, objectManipulationState);

      addTransitionToCylindricalLoadBearing(requestedState, handController, jointSpaceHandControlState, loadBearingCylindricalState, simulationTime);
      addTransitionToCylindricalLoadBearing(requestedState, handController, taskspaceHandPositionControlState, loadBearingCylindricalState, simulationTime);
      addTransitionToLeaveCylindricalLoadBearing(requestedState, handController, loadBearingCylindricalState, taskspaceHandPositionControlState);

      addTransitionToPlaneLoadBearingFingersBentBack(requestedState, handController, taskspaceHandPositionControlState, loadBearingPlaneFingersBentBackState);
      addRequestedStateTransition(requestedState, true, loadBearingPlaneFingersBentBackState, taskspaceHandPositionControlState);

      stateMachine.addState(jointSpaceHandControlState);
      stateMachine.addState(taskspaceHandPositionControlState);
      stateMachine.addState(objectManipulationState);
      stateMachine.addState(loadBearingCylindricalState);
      stateMachine.addState(loadBearingPlaneFingersBentBackState);

      taskSpacePositionControlStates.add(taskspaceHandPositionControlState);
      taskSpacePositionControlStates.add(objectManipulationState);

      parentRegistry.addChild(registry);
   }

   private static void addTransitionToCylindricalLoadBearing(final EnumYoVariable<IndividualHandControlState> requestedState,
           final HandControllerInterface handControllerInterface, State<IndividualHandControlState> fromState, final State<IndividualHandControlState> toState,
           final DoubleYoVariable time)
   {
      StateTransitionCondition stateTransitionCondition = new StateTransitionCondition()
      {
         public boolean checkCondition()
         {
            boolean transitionRequested = requestedState.getEnumValue() == toState.getStateEnum();
            boolean ableToBearLoad = handControllerInterface.isAbleToBearLoad();
            boolean initializedClosedHack = time.getDoubleValue() < .01;    // FIXME: get rid of this. Currently necessary for getting into car

            return transitionRequested && (ableToBearLoad || initializedClosedHack);
         }
      };
      StateTransition<IndividualHandControlState> stateTransition = new StateTransition<IndividualHandControlState>(toState.getStateEnum(),
                                                                       stateTransitionCondition);
      fromState.addStateTransition(stateTransition);
   }

   private static void addTransitionToLeaveCylindricalLoadBearing(final EnumYoVariable<IndividualHandControlState> requestedState,
           final HandControllerInterface handControllerInterface, State<IndividualHandControlState> fromState, final State<IndividualHandControlState> toState)
   {
      StateTransitionCondition stateTransitionCondition = new StateTransitionCondition()
      {
         public boolean checkCondition()
         {
            boolean transitionRequested = requestedState.getEnumValue() == toState.getStateEnum();

            return transitionRequested;
         }
      };
      StateTransitionAction stateTransitionAction = new StateTransitionAction()
      {
         public void doTransitionAction()
         {
            handControllerInterface.openFingers();
         }
      };
      StateTransition<IndividualHandControlState> stateTransition = new StateTransition<IndividualHandControlState>(toState.getStateEnum(),
                                                                       stateTransitionCondition, stateTransitionAction);
      fromState.addStateTransition(stateTransition);
   }

   private static void addTransitionToPlaneLoadBearingFingersBentBack(final EnumYoVariable<IndividualHandControlState> requestedState,
           final HandControllerInterface handControllerInterface, State<IndividualHandControlState> fromState, final State<IndividualHandControlState> toState)
   {
      StateTransitionCondition stateTransitionCondition = new StateTransitionCondition()
      {
         public boolean checkCondition()
         {
            boolean transitionRequested = requestedState.getEnumValue() == toState.getStateEnum();
            boolean ableToBearLoad = handControllerInterface.areFingersBentBack();

            return transitionRequested && ableToBearLoad;
         }
      };
      StateTransition<IndividualHandControlState> stateTransition = new StateTransition<IndividualHandControlState>(toState.getStateEnum(),
                                                                       stateTransitionCondition);
      fromState.addStateTransition(stateTransition);
   }

   public void doControl()
   {
      stateMachine.checkTransitionConditions();
      stateMachine.doAction();
   }

   public boolean isDone()
   {
      return stateMachine.getCurrentState().isDone();
   }

   public void executeTaskSpaceTrajectory(PositionTrajectoryGenerator positionTrajectory, OrientationTrajectoryGenerator orientationTrajectory,
           ReferenceFrame frameToControlPoseOf, RigidBody base, boolean estimateMassProperties, SE3PDGains gains)
   {
      TaskspaceHandPositionControlState state = estimateMassProperties ? objectManipulationState : taskspaceHandPositionControlState;
      RigidBodySpatialAccelerationControlModule rigidBodySpatialAccelerationControlModule =
         getOrCreateRigidBodySpatialAccelerationControlModule(frameToControlPoseOf);
      rigidBodySpatialAccelerationControlModule.setGains(gains);
      state.setTrajectory(positionTrajectory, orientationTrajectory, base, rigidBodySpatialAccelerationControlModule);
      requestedState.set(state.getStateEnum());
      stateMachine.checkTransitionConditions();
   }

   public void moveInStraightLine(FramePose finalDesiredPose, double time, RigidBody base, ReferenceFrame frameToControlPoseOf, ReferenceFrame trajectoryFrame,
                                  boolean holdObject, SE3PDGains gains)
   {
      FramePose pose;
      if (stateMachine.getCurrentState() instanceof TaskspaceHandPositionControlState)
      {
         // start at current desired
         pose = getCurrentDesiredPose((TaskspaceHandPositionControlState) stateMachine.getCurrentState(), frameToControlPoseOf, trajectoryFrame);
      }
      else
      {
         // start at current actual
         pose = new FramePose(frameToControlPoseOf);
      }

      initialConfigurationProvider.set(pose);
      finalConfigurationProvider.set(finalDesiredPose);
      trajectoryTimeProvider.set(time);
      executeTaskSpaceTrajectory(getOrCreateStraightLinePositionTrajectoryGenerator(trajectoryFrame),
                                 getOrCreateOrientationInterpolationTrajectoryGenerator(trajectoryFrame), frameToControlPoseOf, base, holdObject, gains);
   }

   private FramePose getCurrentDesiredPose(TaskspaceHandPositionControlState taskspaceHandPositionControlState, ReferenceFrame frameToControlPoseOf,
           ReferenceFrame trajectoryFrame)
   {
      FramePose pose = taskspaceHandPositionControlState.getDesiredPose();
      pose.changeFrame(trajectoryFrame);

      Transform3D oldTrackingFrameTransform = new Transform3D();
      pose.getTransformFromPoseToFrame(oldTrackingFrameTransform);
      Transform3D transformFromNewTrackingFrameToOldTrackingFrame =
         frameToControlPoseOf.getTransformToDesiredFrame(taskspaceHandPositionControlState.getFrameToControlPoseOf());

      Transform3D newTrackingFrameTransform = new Transform3D();
      newTrackingFrameTransform.mul(oldTrackingFrameTransform, transformFromNewTrackingFrameToOldTrackingFrame);
      pose.set(trajectoryFrame, newTrackingFrameTransform);

      return pose;
   }

   public boolean isInCylindricalLoadBearingState()
   {
      return stateMachine.isCurrentState(IndividualHandControlState.LOAD_BEARING_CYLINDRICAL);
   }

   public void requestLoadBearing()
   {
      if (handController.isClosing())
         requestedState.set(loadBearingCylindricalState.getStateEnum());
      else if (handController.areFingersBendingBack())
       requestedState.set(loadBearingPlaneFingersBentBackState.getStateEnum());
   }

   public void executeJointSpaceTrajectory(Map<OneDoFJoint, ? extends DoubleTrajectoryGenerator> trajectories)
   {
      jointSpaceHandControlState.setTrajectories(trajectories);
      requestedState.set(jointSpaceHandControlState.getStateEnum());
      stateMachine.checkTransitionConditions();
   }

   public void moveUsingQuinticSplines(Map<OneDoFJoint, Double> desiredJointPositions, double time)
   {
      if (!desiredJointPositions.keySet().containsAll(quinticPolynomialTrajectoryGenerators.keySet()))
         throw new RuntimeException("not all joint positions specified");

      trajectoryTimeProvider.set(time);

      for (OneDoFJoint oneDoFJoint : desiredJointPositions.keySet())
      {
         quinticPolynomialTrajectoryGenerators.get(oneDoFJoint).setFinalPosition(desiredJointPositions.get(oneDoFJoint));
      }

      executeJointSpaceTrajectory(quinticPolynomialTrajectoryGenerators);
   }

   public void moveJointsInRange(Map<OneDoFJoint, Double> minJointPositions, Map<OneDoFJoint, Double> maxJointPositions, double time)
   {
      checkLimitsValid(minJointPositions, maxJointPositions);

      Map<OneDoFJoint, Double> allJointPositions = new LinkedHashMap<OneDoFJoint, Double>();
      for (OneDoFJoint oneDoFJoint : oneDoFJoints)
      {
         double q = oneDoFJoint.getQ();
         double qFinal = q;
         Double minJointPosition = minJointPositions.get(oneDoFJoint);

         if ((minJointPosition != null) && (q < minJointPosition))
         {
            qFinal = minJointPosition;
         }

         Double maxJointPosition = maxJointPositions.get(oneDoFJoint);
         if ((maxJointPosition != null) && (q > maxJointPosition))
            qFinal = maxJointPosition;

         allJointPositions.put(oneDoFJoint, qFinal);
      }

      moveUsingQuinticSplines(allJointPositions, time);
   }

   public boolean isHoldingObject()
   {
      return stateMachine.getCurrentStateEnum() == IndividualHandControlState.OBJECT_MANIPULATION;
   }

   public void holdPositionInBase()
   {
      // NOTE here we make poseToHolinBase in chestFrame, not in the endEffectorFrame. Otherwise the hand will sag.
      ReferenceFrame endEffectorFrame = jacobian.getEndEffectorFrame();
      FramePose poseToHoldInBase = new FramePose(endEffectorFrame);
      ReferenceFrame chestFrame = fullRobotModel.getChest().getBodyFixedFrame();
      poseToHoldInBase.changeFrame(chestFrame);
      initialConfigurationProvider.set(poseToHoldInBase);
      PositionTrajectoryGenerator positionTrajectory = holdPositionInBaseTrajectoryGenerator;
      OrientationTrajectoryGenerator orientationTrajectory = holdOrientationInBaseTrajectoryGenerator;
      executeTaskSpaceTrajectory(positionTrajectory, orientationTrajectory, endEffectorFrame, jacobian.getBase(), isHoldingObject(), defaultGains);
   }

   private void checkLimitsValid(Map<OneDoFJoint, Double> minJointPositions, Map<OneDoFJoint, Double> maxJointPositions)
   {
      for (OneDoFJoint oneDoFJoint : oneDoFJoints)
      {
         Double minJointPosition = minJointPositions.get(oneDoFJoint);
         Double maxJointPosition = maxJointPositions.get(oneDoFJoint);
         if ((minJointPosition != null) && (maxJointPosition != null) && (minJointPosition > maxJointPosition))
            throw new RuntimeException("min > max");
      }
   }

   private StraightLinePositionTrajectoryGenerator getOrCreateStraightLinePositionTrajectoryGenerator(ReferenceFrame referenceFrame)
   {
      StraightLinePositionTrajectoryGenerator ret = straightLinePositionWorldTrajectoryGenerators.get(referenceFrame);
      if (ret == null)
      {
         ret = new StraightLinePositionTrajectoryGenerator(name + referenceFrame.getName(), referenceFrame, trajectoryTimeProvider,
                 initialConfigurationProvider, finalConfigurationProvider, registry);
         straightLinePositionWorldTrajectoryGenerators.put(referenceFrame, ret);
      }

      return ret;
   }

   private OrientationInterpolationTrajectoryGenerator getOrCreateOrientationInterpolationTrajectoryGenerator(ReferenceFrame referenceFrame)
   {
      OrientationInterpolationTrajectoryGenerator ret = orientationInterpolationWorldTrajectoryGenerators.get(referenceFrame);
      if (ret == null)
      {
         ret = new OrientationInterpolationTrajectoryGenerator(name + referenceFrame.getName(), referenceFrame, trajectoryTimeProvider,
                 initialConfigurationProvider, finalConfigurationProvider, registry);
         orientationInterpolationWorldTrajectoryGenerators.put(referenceFrame, ret);
      }

      return ret;
   }

   private RigidBodySpatialAccelerationControlModule getOrCreateRigidBodySpatialAccelerationControlModule(ReferenceFrame handPositionControlFrame)
   {
      RigidBodySpatialAccelerationControlModule ret = handSpatialAccelerationControlModules.get(handPositionControlFrame);
      if (ret == null)
      {
         ret = new RigidBodySpatialAccelerationControlModule(name + handPositionControlFrame.getName(), twistCalculator, jacobian.getEndEffector(),
                 handPositionControlFrame, registry);

         handSpatialAccelerationControlModules.put(handPositionControlFrame, ret);
      }

      return ret;
   }

   public boolean isControllingPoseInWorld()
   {
      State<IndividualHandControlState> currentState = stateMachine.getCurrentState();

      for (TaskspaceHandPositionControlState taskSpacePositionControlState : taskSpacePositionControlStates)
      {
         if (currentState == taskSpacePositionControlState)
            return taskSpacePositionControlState.getReferenceFrame() == ReferenceFrame.getWorldFrame();
      }

      return false;
   }
}
