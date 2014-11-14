package us.ihmc.simulationconstructionset.robotController;

import us.ihmc.yoUtilities.dataStructure.registry.YoVariableRegistry;

class SingleThreadedRobotControllerExecutor implements RobotControllerExecutor
{
   private final long ticksPerSimulationTick;
   private final MultiThreadedRobotControlElement robotControlElement;
   private final boolean skipFirstControlCycle;

   SingleThreadedRobotControllerExecutor(MultiThreadedRobotControlElement robotControlElement, int ticksPerSimulationTick, boolean skipFirstControlCycle, YoVariableRegistry parentRegistry)
   {
      this.ticksPerSimulationTick = ticksPerSimulationTick;
      this.robotControlElement = robotControlElement;
      this.skipFirstControlCycle = skipFirstControlCycle;

      parentRegistry.addChild(robotControlElement.getYoVariableRegistry());
   }

   public void waitAndWriteData(long tick)
   {
      if (tick % ticksPerSimulationTick == 0 && !(tick == 0 && skipFirstControlCycle))
      {
         robotControlElement.write(System.nanoTime());
      }
   }

   public void readData(long tick)
   {
      if (tick % ticksPerSimulationTick == 0 && !(tick == 0 && skipFirstControlCycle))
      {
         robotControlElement.read(System.nanoTime());
      }
   }

   public void executeForSimulationTick(long tick)
   {
      if (tick % ticksPerSimulationTick == 0 && !(tick == 0 && skipFirstControlCycle))
      {
         robotControlElement.run();
      }
   }

   public void initialize()
   {
      robotControlElement.initialize();
   }
   
   public long getTicksPerSimulationTick()
   {
      return ticksPerSimulationTick;
   }

   public void stop()
   {
      // Nothing to do here.
   }

   public void updateDynamicGraphicObjectListRegistry()
   {
   }
   

}
