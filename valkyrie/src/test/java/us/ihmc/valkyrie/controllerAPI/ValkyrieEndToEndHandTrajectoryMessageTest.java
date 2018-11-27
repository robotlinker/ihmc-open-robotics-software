package us.ihmc.valkyrie.controllerAPI;

import static org.junit.Assert.*;

import org.junit.Test;

import controller_msgs.msg.dds.HandTrajectoryMessage;
import controller_msgs.msg.dds.SE3TrajectoryMessage;
import controller_msgs.msg.dds.WrenchTrajectoryMessage;
import us.ihmc.avatar.DRCObstacleCourseStartingLocation;
import us.ihmc.avatar.controllerAPI.EndToEndHandTrajectoryMessageTest;
import us.ihmc.avatar.drcRobot.DRCRobotModel;
import us.ihmc.avatar.drcRobot.RobotTarget;
import us.ihmc.avatar.testTools.DRCSimulationTestHelper;
import us.ihmc.commons.lists.RecyclingArrayList;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.communication.packets.MessageTools;
import us.ihmc.continuousIntegration.ContinuousIntegrationAnnotations.ContinuousIntegrationTest;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.tuple3D.Point3D;
import us.ihmc.euclid.tuple3D.Vector3D;
import us.ihmc.euclid.tuple3D.interfaces.Point3DReadOnly;
import us.ihmc.euclid.tuple3D.interfaces.Vector3DReadOnly;
import us.ihmc.euclid.tuple4D.Quaternion;
import us.ihmc.humanoidRobotics.communication.packets.HumanoidMessageTools;
import us.ihmc.robotics.math.trajectories.waypoints.EuclideanTrajectoryPointCalculator;
import us.ihmc.robotics.math.trajectories.waypoints.FrameEuclideanTrajectoryPoint;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.robotics.screwTheory.SelectionMatrix3D;
import us.ihmc.simulationConstructionSetTools.bambooTools.BambooTools;
import us.ihmc.simulationConstructionSetTools.util.environments.HeavyBallOnTableEnvironment;
import us.ihmc.simulationconstructionset.util.simulationRunner.BlockingSimulationRunner.SimulationExceededMaximumTimeException;
import us.ihmc.valkyrie.ValkyrieRobotModel;
import us.ihmc.valkyrie.parameters.ValkyrieContactPointParameters;
import us.ihmc.valkyrie.parameters.ValkyriePhysicalProperties;
import us.ihmc.wholeBodyController.RobotContactPointParameters;

public class ValkyrieEndToEndHandTrajectoryMessageTest extends EndToEndHandTrajectoryMessageTest
{
   private final ValkyrieRobotModel robotModel = new ValkyrieRobotModel(RobotTarget.SCS, false);

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 46.3)
   @Test(timeout = 230000)
   public void testCustomControlFrame() throws SimulationExceededMaximumTimeException
   {
      super.testCustomControlFrame();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 15.7)
   @Test(timeout = 79000)
   public void testMessageWithTooManyTrajectoryPoints() throws Exception
   {
      super.testMessageWithTooManyTrajectoryPoints();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 36.8)
   @Test(timeout = 180000)
   public void testMultipleTrajectoryPoints() throws Exception
   {
      super.testMultipleTrajectoryPoints();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 65.8)
   @Test(timeout = 330000)
   public void testQueuedMessages() throws Exception
   {
      super.testQueuedMessages();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 31.2)
   @Test(timeout = 160000)
   public void testQueueStoppedWithOverrideMessage() throws Exception
   {
      super.testQueueStoppedWithOverrideMessage();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 17.1)
   @Test(timeout = 86000)
   public void testQueueWithWrongPreviousId() throws Exception
   {
      super.testQueueWithWrongPreviousId();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 42.0)
   @Test(timeout = 210000)
   public void testSingleTrajectoryPoint() throws Exception
   {
      super.testSingleTrajectoryPoint();
   }

   @Override
   @ContinuousIntegrationTest(estimatedDuration = 43.9)
   @Test(timeout = 220000)
   public void testStopAllTrajectory() throws Exception
   {
      super.testStopAllTrajectory();
   }

   @ContinuousIntegrationTest(estimatedDuration = 40.0)
   @Test(timeout = 300000)
   public void testWrenchTrajectoryMessage() throws Exception
   {
      BambooTools.reportTestStartedMessage(simulationTestingParameters.getShowWindows());

      DRCObstacleCourseStartingLocation selectedLocation = DRCObstacleCourseStartingLocation.DEFAULT;

      HeavyBallOnTableEnvironment testEnvironment = new HeavyBallOnTableEnvironment();
      drcSimulationTestHelper = new DRCSimulationTestHelper(simulationTestingParameters, getRobotModelWithHandContacts(), testEnvironment);
      drcSimulationTestHelper.setStartingLocation(selectedLocation);
      drcSimulationTestHelper.createSimulation(getClass().getSimpleName());

      ThreadTools.sleep(1000);
      boolean success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(2.0);
      assertTrue(success);

      EuclideanTrajectoryPointCalculator calculator = new EuclideanTrajectoryPointCalculator();
      calculator.appendTrajectoryPoint(1.0, new Point3D(0.25, 0.0, 1.05));
      calculator.appendTrajectoryPoint(1.5, new Point3D(0.4, 0.0, 0.95));
      calculator.appendTrajectoryPoint(1.75, new Point3D(0.5, 0.0, 0.85));
      calculator.appendTrajectoryPoint(2.0, new Point3D(0.6, 0.0, 0.95));
      calculator.appendTrajectoryPoint(2.5, new Point3D(0.6, 0.0, 1.25));
      calculator.appendTrajectoryPoint(3.5, new Point3D(0.25, 0.0, 1.05));
      calculator.computeTrajectoryPointVelocities(true);
      RecyclingArrayList<FrameEuclideanTrajectoryPoint> trajectoryPoints = calculator.getTrajectoryPoints();
      SE3TrajectoryMessage se3TrajectoryMessage = new SE3TrajectoryMessage();
      for (FrameEuclideanTrajectoryPoint trajectoryPoint : trajectoryPoints)
      {
         double time = trajectoryPoint.getTime();
         Point3DReadOnly position = trajectoryPoint.getPositionCopy();
         Vector3DReadOnly linearVelocity = trajectoryPoint.getLinearVelocityCopy();
         se3TrajectoryMessage.getTaskspaceTrajectoryPoints().add()
                             .set(HumanoidMessageTools.createSE3TrajectoryPointMessage(time, position, new Quaternion(), linearVelocity, new Vector3D()));
      }
      se3TrajectoryMessage.getAngularSelectionMatrix().set(MessageTools.createSelectionMatrix3DMessage(new SelectionMatrix3D(null, false, false, false)));

      WrenchTrajectoryMessage wrenchTrajectoryMessage = new WrenchTrajectoryMessage();
      wrenchTrajectoryMessage.getFrameInformation().setTrajectoryReferenceFrameId(ReferenceFrame.getWorldFrame().hashCode());
      wrenchTrajectoryMessage.getWrenchTrajectoryPoints().add().set(HumanoidMessageTools.createWrenchTrajectoryPointMessage(1.8, null, null));
      wrenchTrajectoryMessage.getWrenchTrajectoryPoints().add()
                             .set(HumanoidMessageTools.createWrenchTrajectoryPointMessage(1.9, null, new Vector3D(150.0, 0.0, 75.0)));
      wrenchTrajectoryMessage.getWrenchTrajectoryPoints().add()
                             .set(HumanoidMessageTools.createWrenchTrajectoryPointMessage(2.2, null, new Vector3D(150.0, 0.0, 75.0)));
      wrenchTrajectoryMessage.getWrenchTrajectoryPoints().add().set(HumanoidMessageTools.createWrenchTrajectoryPointMessage(2.7, null, null));
      wrenchTrajectoryMessage.setUseCustomControlFrame(true);
      wrenchTrajectoryMessage.getControlFramePose().setPosition(-0.15, -0.11, 0.0);

      HandTrajectoryMessage rightHandTrajectoryMessage = HumanoidMessageTools.createHandTrajectoryMessage(RobotSide.RIGHT, se3TrajectoryMessage);
      rightHandTrajectoryMessage.getWrenchTrajectory().set(wrenchTrajectoryMessage);
      drcSimulationTestHelper.publishToController(rightHandTrajectoryMessage);

      success = drcSimulationTestHelper.simulateAndBlockAndCatchExceptions(5.0);
      assertTrue(success);

      double ballHeight = testEnvironment.getBallRobot().getFloatingJoint().getQz().getValue();
      assertEquals(testEnvironment.getBallRadius(), ballHeight, 0.01);
   }

   @Override
   public DRCRobotModel getRobotModel()
   {
      return robotModel;
   }

   public DRCRobotModel getRobotModelWithHandContacts()
   {
      return new ValkyrieRobotModel(RobotTarget.SCS, false)
      {
         @Override
         public RobotContactPointParameters<RobotSide> getContactPointParameters()
         {
            ValkyrieContactPointParameters contactPointParameters = new ValkyrieContactPointParameters(getJointMap(), null);
            contactPointParameters.createAdditionalHandContactPoints();
            return contactPointParameters;
         }

         @Override
         public double getSimulateDT()
         {
            return super.getSimulateDT();
         }
      };
   }

   @Override
   public String getSimpleRobotName()
   {
      return BambooTools.getSimpleRobotNameFor(BambooTools.SimpleRobotNameKeys.VALKYRIE);
   }

   @Override
   public double getLegLength()
   {
      return ValkyriePhysicalProperties.getLegLength();
   }
}
