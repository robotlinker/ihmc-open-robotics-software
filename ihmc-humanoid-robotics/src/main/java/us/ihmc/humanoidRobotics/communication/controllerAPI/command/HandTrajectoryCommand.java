package us.ihmc.humanoidRobotics.communication.controllerAPI.command;

import java.util.Random;

import controller_msgs.msg.dds.HandTrajectoryMessage;
import us.ihmc.communication.controllerAPI.command.Command;
import us.ihmc.euclid.interfaces.EpsilonComparable;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.humanoidRobotics.communication.controllerAPI.converter.FrameBasedCommand;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.sensorProcessing.frames.ReferenceFrameHashCodeResolver;

public class HandTrajectoryCommand
      implements Command<HandTrajectoryCommand, HandTrajectoryMessage>, FrameBasedCommand<HandTrajectoryMessage>, EpsilonComparable<HandTrajectoryCommand>
{
   private RobotSide robotSide;
   private final SE3TrajectoryControllerCommand se3Trajectory;
   private final WrenchTrajectoryControllerCommand wrenchTrajectory;

   public HandTrajectoryCommand()
   {
      se3Trajectory = new SE3TrajectoryControllerCommand();
      wrenchTrajectory = new WrenchTrajectoryControllerCommand();
   }

   public HandTrajectoryCommand(RobotSide robotSide, ReferenceFrame dataFrame, ReferenceFrame trajectoryFrame)
   {
      se3Trajectory = new SE3TrajectoryControllerCommand(dataFrame, trajectoryFrame);
      wrenchTrajectory = new WrenchTrajectoryControllerCommand(dataFrame, trajectoryFrame);
      this.robotSide = robotSide;
   }

   public HandTrajectoryCommand(Random random)
   {
      se3Trajectory = new SE3TrajectoryControllerCommand(random);
      wrenchTrajectory = new WrenchTrajectoryControllerCommand(random);
      robotSide = RobotSide.generateRandomRobotSide(random);
   }

   @Override
   public void clear()
   {
      se3Trajectory.clear();
      wrenchTrajectory.clear();
      robotSide = null;
   }

   public void clear(ReferenceFrame referenceFrame)
   {
      se3Trajectory.clear(referenceFrame);
      wrenchTrajectory.clear(referenceFrame);
      robotSide = null;
   }

   @Override
   public void set(HandTrajectoryCommand other)
   {
      se3Trajectory.set(other.se3Trajectory);
      wrenchTrajectory.set(other.wrenchTrajectory);
      robotSide = other.robotSide;
   }

   /**
    * Same as {@link #set(HandTrajectoryCommand)} but does not change the trajectory points.
    *
    * @param other
    */
   public void setPropertiesOnly(HandTrajectoryCommand other)
   {
      se3Trajectory.setPropertiesOnly(other.se3Trajectory);
      wrenchTrajectory.setPropertiesOnly(other.wrenchTrajectory);
      robotSide = other.robotSide;
   }

   @Override
   public void setFromMessage(HandTrajectoryMessage message)
   {
      se3Trajectory.setFromMessage(message.getSe3Trajectory());
      wrenchTrajectory.setFromMessage(message.getWrenchTrajectory());
      robotSide = RobotSide.fromByte(message.getRobotSide());
   }

   @Override
   public void set(ReferenceFrameHashCodeResolver resolver, HandTrajectoryMessage message)
   {
      se3Trajectory.set(resolver, message.getSe3Trajectory());
      wrenchTrajectory.set(resolver, message.getWrenchTrajectory());
      robotSide = RobotSide.fromByte(message.getRobotSide());
   }

   public void setRobotSide(RobotSide robotSide)
   {
      this.robotSide = robotSide;
   }

   public RobotSide getRobotSide()
   {
      return robotSide;
   }

   public SE3TrajectoryControllerCommand getSE3Trajectory()
   {
      return se3Trajectory;
   }

   public WrenchTrajectoryControllerCommand getWrenchTrajectory()
   {
      return wrenchTrajectory;
   }

   @Override
   public Class<HandTrajectoryMessage> getMessageClass()
   {
      return HandTrajectoryMessage.class;
   }

   @Override
   public boolean isCommandValid()
   {
      return robotSide != null && se3Trajectory.isCommandValid();
   }

   @Override
   public boolean epsilonEquals(HandTrajectoryCommand other, double epsilon)
   {
      return robotSide == other.robotSide && se3Trajectory.epsilonEquals(other.se3Trajectory, epsilon) && wrenchTrajectory.epsilonEquals(other.wrenchTrajectory, epsilon);
   }
   
   @Override
   public boolean isDelayedExecutionSupported()
   {
      return true;
   }

   @Override
   public void setExecutionDelayTime(double delayTime)
   {
      se3Trajectory.setExecutionDelayTime(delayTime);
      wrenchTrajectory.setExecutionDelayTime(delayTime);
   }

   @Override
   public void setExecutionTime(double adjustedExecutionTime)
   {
      se3Trajectory.setExecutionTime(adjustedExecutionTime);
      wrenchTrajectory.setExecutionTime(adjustedExecutionTime);
   }

   @Override
   public double getExecutionDelayTime()
   {
      return se3Trajectory.getExecutionDelayTime();
   }

   @Override
   public double getExecutionTime()
   {
      return se3Trajectory.getExecutionTime();
   }
}
