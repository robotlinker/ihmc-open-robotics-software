package us.ihmc.exampleSimulations.genericQuadruped.controller.force;

import controller_msgs.msg.dds.*;
import org.apache.commons.lang3.SystemUtils;
import org.ejml.data.DenseMatrix64F;
import org.ejml.data.DenseMatrixBool;
import org.junit.Test;
import us.ihmc.commons.PrintTools;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.exampleSimulations.genericQuadruped.GenericQuadrupedTestFactory;
import us.ihmc.pubsub.DomainFactory.PubSubImplementation;
import us.ihmc.quadrupedRobotics.QuadrupedTestFactory;
import us.ihmc.quadrupedRobotics.communication.QuadrupedMessageTools;
import us.ihmc.quadrupedRobotics.controller.force.QuadrupedTOWRTrajectoryTest;
import us.ihmc.quadrupedRobotics.planning.trajectoryConverter.QuadrupedTOWRTrajectoryConverter;
import us.ihmc.quadrupedRobotics.planning.trajectoryConverter.TowrCartesianStates.LegIndex;
import us.ihmc.quadrupedRobotics.util.TimeInterval;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.robotSide.SideDependentList;
import us.ihmc.ros2.*;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner;
import us.ihmc.quadrupedRobotics.planning.trajectoryConverter.TowrCartesianStates;
import us.ihmc.util.PeriodicNonRealtimeThreadSchedulerFactory;
import us.ihmc.util.PeriodicRealtimeThreadSchedulerFactory;
import us.ihmc.util.PeriodicThreadSchedulerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class GenericQuadrupedTOWRTest extends QuadrupedTOWRTrajectoryTest
{
   static ArrayList<Point3D> basePositions;
   @Override
   public QuadrupedTestFactory createQuadrupedTestFactory()
   {
      return new GenericQuadrupedTestFactory();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 74.7)
   @Test(timeout = 370000)
   public void testQuadrupedTOWRTrajectory() throws BlockingSimulationRunner.SimulationExceededMaximumTimeException
   {
      super.testQuadrupedTOWRTrajectory();
   }

   @Override
   public Point3D getFinalPlanarPosition()
   {
      return new Point3D(1.684, 0.077, 0.0);
   }


   private final SideDependentList<RobotStateCartesianTrajectory> subscribers = new SideDependentList<>();

   public static TowrCartesianStates subscribeToTowrRobotStateCartesianTrajectory() throws IOException
   {
      PeriodicThreadSchedulerFactory threadFactory = SystemUtils.IS_OS_LINUX ?
            new PeriodicRealtimeThreadSchedulerFactory(20) :
            new PeriodicNonRealtimeThreadSchedulerFactory();
      RealtimeRos2Node node = new RealtimeRos2Node(PubSubImplementation.FAST_RTPS, threadFactory, "NonRealtimeRos2PublishSubscribeExample", "");
      RealtimeRos2Publisher<RobotStateCartesianTrajectory> publisher = node.createPublisher(RobotStateCartesianTrajectory.getPubSubType().get(), "towr_ros2");

      RealtimeRos2Subscription<RobotStateCartesianTrajectory> subscription = node.createQueuedSubscription(RobotStateCartesianTrajectory.getPubSubType().get(), "towr_ros2");

      RobotStateCartesianTrajectory message = new RobotStateCartesianTrajectory();

      RobotStateCartesianTrajectory incomingMessage = new RobotStateCartesianTrajectory();
      while (!subscription.poll(incomingMessage))
      {
         ; // just waiting for the first message
      }


      //while (true)
      //{
      if (subscription.poll(incomingMessage))  // poll for new messages
      {
         //System.out.println(incomingMessage);
         //i++;
      }
      else
      {
         // no available messages
      }
      //}

      TowrCartesianStates towrCartesianStatesToFill = new TowrCartesianStates(incomingMessage.getPoints().size());
      QuadrupedTOWRTrajectoryConverter quadrupedTowrTrajectoryConverter = new QuadrupedTOWRTrajectoryConverter();

      quadrupedTowrTrajectoryConverter.towrMessageToCartesianStateConverter(incomingMessage, towrCartesianStatesToFill);

      //node.spin(); // start the realtime node thread

      return towrCartesianStatesToFill;

   }

   @Override
   public List<QuadrupedTimedStepMessage> getSteps()
   {
      TowrCartesianStates towrCartesianStates = new TowrCartesianStates(200);
      try
      {
         towrCartesianStates = subscribeToTowrRobotStateCartesianTrajectory();
         PrintTools.info("Number of points: "+towrCartesianStates.getPointsNumber());
         PrintTools.info("Base trajectory: "+towrCartesianStates.getBaseLinearTrajectoryWorldFrame());
         PrintTools.info("FL foot trajectory WF: "+towrCartesianStates.getFrontLeftFootPositionWorldFrame());
         PrintTools.info("FR foot trajectory WF: "+towrCartesianStates.getFrontRightFootPositionWorldFrame());
         PrintTools.info("HL foot trajectory WF: "+towrCartesianStates.getHindLeftFootPositionWorldFrame());
         PrintTools.info("HR foot trajectory WF: "+towrCartesianStates.getHindRightFootPositionWorldFrame());

         PrintTools.info("FL foot trajectory BF: "+towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.FL));
         PrintTools.info("FR foot trajectory BF: "+towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.FR));
         PrintTools.info("HL foot trajectory BF: "+towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.HL));
         PrintTools.info("HR foot trajectory BF: "+towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.HR));

         PrintTools.info("Number of steps: "+towrCartesianStates.getStepsNumber());

         PrintTools.info("Touch down: "+towrCartesianStates.getTouchDown());
         PrintTools.info("Take off: "+towrCartesianStates.getTakeOff());
      }
      catch (Exception e)
      {
      }
      //PrintTools.info("initial base pos TOWR:"+initial_base_pos);

      ArrayList<QuadrupedTimedStepMessage> steps = new ArrayList<>();
      int stepsTot = 3;
      //for (LegIndex legIdx : LegIndex.values()){
         int legIdx = 0;
         DenseMatrix64F stepsTotal = towrCartesianStates.getStepsNumber();
         for(int stepCounter = 0; stepCounter< stepsTot; stepCounter++){
            double targetPositionX = towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.FL).get(stepCounter, 0);
            double targetPositionY = towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.FL).get(stepCounter, 1);
            double touchDown = towrCartesianStates.getTouchDown().get(stepCounter+1, legIdx);
            double takeOff = towrCartesianStates.getTakeOff().get(stepCounter+1, legIdx);
            steps.add(QuadrupedMessageTools.createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_LEFT, new Point3D(targetPositionX, targetPositionY, -0.012), 0.1, new TimeInterval(takeOff, touchDown)));
         }

      legIdx = 1;
      for(int stepCounter = 0; stepCounter< stepsTot; stepCounter++){
         double targetPositionX = towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.FR).get(stepCounter, 0);
         double targetPositionY = towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.FR).get(stepCounter, 1);
         double touchDown = towrCartesianStates.getTouchDown().get(stepCounter+1, legIdx);
         double takeOff = towrCartesianStates.getTakeOff().get(stepCounter+1, legIdx);
         steps.add(QuadrupedMessageTools.createQuadrupedTimedStepMessage(RobotQuadrant.FRONT_RIGHT, new Point3D(targetPositionX, targetPositionY, -0.012), 0.1, new TimeInterval(takeOff, touchDown)));
      }

      legIdx = 2;
      for(int stepCounter = 0; stepCounter< stepsTot; stepCounter++){
         double targetPositionX = towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.HL).get(stepCounter, 0);
         double targetPositionY = towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.HL).get(stepCounter, 1);
         double touchDown = towrCartesianStates.getTouchDown().get(stepCounter+1, legIdx);
         double takeOff = towrCartesianStates.getTakeOff().get(stepCounter+1, legIdx);
         steps.add(QuadrupedMessageTools.createQuadrupedTimedStepMessage(RobotQuadrant.HIND_LEFT, new Point3D(targetPositionX, targetPositionY, -0.012), 0.1, new TimeInterval(takeOff, touchDown)));
      }

      legIdx = 3;
      for(int stepCounter = 0; stepCounter< stepsTot; stepCounter++){
         double targetPositionX = towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.HR).get(stepCounter, 0);
         double targetPositionY = towrCartesianStates.getTargetFootholdBaseFrame(LegIndex.HR).get(stepCounter, 1);
         double touchDown = towrCartesianStates.getTouchDown().get(stepCounter+1, legIdx);
         double takeOff = towrCartesianStates.getTakeOff().get(stepCounter+1, legIdx);
         steps.add(QuadrupedMessageTools.createQuadrupedTimedStepMessage(RobotQuadrant.HIND_RIGHT, new Point3D(targetPositionX, targetPositionY, -0.012), 0.1, new TimeInterval(takeOff, touchDown)));
      }

      return steps;
   }
}
