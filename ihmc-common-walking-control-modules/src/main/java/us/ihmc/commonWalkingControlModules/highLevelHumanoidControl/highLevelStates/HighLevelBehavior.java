package us.ihmc.commonWalkingControlModules.highLevelHumanoidControl.highLevelStates;

import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreCommand;
import us.ihmc.commonWalkingControlModules.controllerCore.command.ControllerCoreOutputReadOnly;
import us.ihmc.humanoidRobotics.communication.packets.dataobjects.HighLevelController;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.robotics.stateMachines.conditionBasedStateMachine.FinishableState;

public abstract class HighLevelBehavior extends FinishableState<HighLevelController>
{

   public HighLevelBehavior(HighLevelController stateEnum)
   {
      super(stateEnum);
   }

   @Override
   public abstract void doAction();

   @Override
   public abstract void doTransitionIntoAction();

   @Override
   public abstract void doTransitionOutOfAction();

   public abstract YoVariableRegistry getYoVariableRegistry();

   public abstract void setControllerCoreOutput(ControllerCoreOutputReadOnly controllerCoreOutput);

   public abstract ControllerCoreCommand getControllerCoreCommand();

   @Override
   public boolean isDone()
   {
      return true;
   }
}
