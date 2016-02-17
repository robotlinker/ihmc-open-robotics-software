package us.ihmc.aware.planning;

import us.ihmc.aware.util.PreallocatedQueue;
import us.ihmc.aware.util.QuadrupedTimedStep;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.quadrupedRobotics.referenceFrames.QuadrupedReferenceFrames;
import us.ihmc.quadrupedRobotics.supportPolygon.QuadrupedSupportPolygon;
import us.ihmc.quadrupedRobotics.util.HeterogeneousMemoryPool;
import us.ihmc.robotics.dataStructures.registry.YoVariableRegistry;
import us.ihmc.robotics.geometry.FramePoint;
import us.ihmc.robotics.geometry.FrameVector;
import us.ihmc.robotics.geometry.RigidBodyTransform;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotEnd;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.robotSide.RobotSide;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.BagOfBalls;
import us.ihmc.simulationconstructionset.yoUtilities.graphics.YoGraphicsListRegistry;

/**
 * A footstep planner capable of planning pace, amble, and trot gaits.
 */
public class QuadrupedXGaitFootstepPlanner
{
   private static final int FOOTSTEP_QUEUE_SIZE = 15;

   private static final RobotQuadrant[] FOOTSTEP_ORDER = new RobotQuadrant[] { RobotQuadrant.HIND_LEFT,
         RobotQuadrant.FRONT_LEFT, RobotQuadrant.HIND_RIGHT, RobotQuadrant.FRONT_RIGHT };

   /**
    * The time a foot should spend in the air during its swing state in s.
    */
   private double swingDuration = 0.8;

   /**
    * The time during which both feet of a front or back pair are in support in s.
    */
   private double endPairSupportDuration = 0.8;

   /**
    * The phase offset of the front and back feet pairs in deg.
    */
   private double phaseShift = 90.0;

   private double stanceLength = 1.1;
   private double stanceWidth = 0.5;

   private double strideLength = 0.3;
   private double strideWidth = 0.0;

   // TODO: Compute conversion of arbitrary yaw rate units to rad/s.
   private double yawRate = 0.1;

   private final QuadrupedReferenceFrames referenceFrames;
   private final BagOfBalls footstepVisualization;

   /**
    * Maintain four footstep queues -- one for each quadrant. This way, they can be merge-sorted in order of start time
    * into a full plan. This is necessary because some combinations of parameters generate the steps out of order.
    */
   private final QuadrantDependentList<PreallocatedQueue<QuadrupedTimedStep>> queues = new QuadrantDependentList<>();

   private final HeterogeneousMemoryPool pool = new HeterogeneousMemoryPool();

   public QuadrupedXGaitFootstepPlanner(YoVariableRegistry registry, YoGraphicsListRegistry graphicsListRegistry,
         QuadrupedReferenceFrames referenceFrames)
   {
      this.referenceFrames = referenceFrames;

      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         queues.set(quadrant, new PreallocatedQueue<>(QuadrupedTimedStep.class, FOOTSTEP_QUEUE_SIZE));
      }

      this.footstepVisualization = BagOfBalls
            .createRainbowBag(FOOTSTEP_QUEUE_SIZE * 4, 0.03, "footsteps", registry, graphicsListRegistry);
   }

   public void plan(PreallocatedQueue<QuadrupedTimedStep> steps, double startTime)
   {
      pool.evict();

      // Add the initialization steps.
      double initializationPeriod = addInitializationSteps(startTime);

      // Fill the queue with steps.
      final double gaitPeriod = 2.0 * swingDuration + 2.0 * endPairSupportDuration;
      for (int i = 0; i < FOOTSTEP_QUEUE_SIZE - 4; i++)
      {
         addFourSteps(initializationPeriod + startTime + i * gaitPeriod);
      }

      // Draw the footstep visualization.
      footstepVisualization.setVisible(true);
      for (RobotQuadrant quadrant : RobotQuadrant.values)
      {
         for (int i = 0; i < queues.get(quadrant).size(); i++)
         {
            QuadrupedTimedStep step = queues.get(quadrant).get(i);

            step.getGoalPosition().changeFrame(referenceFrames.getWorldFrame());
            footstepVisualization
                  .setBallLoop(step.getGoalPosition(), YoAppearance.getStandardRoyGBivRainbow()[quadrant.ordinal()]);
         }
      }

      // Clear the queue to make room for our plan.
      while (steps.dequeue())
         ;

      // Dequeue footsteps from each of the quadrant queues and add them to the plan ordered by their start time.
      mergeFootstepQueues(steps);
   }

   /**
    * Add some steps to bring the feet to their initial positions, from which the gait can begin.
    *
    * @param startTime the time at which the first initialization step should begin
    * @return the initialization period
    */
   private double addInitializationSteps(double startTime)
   {
      int stepIndex = 0;
      for (RobotQuadrant quadrant : FOOTSTEP_ORDER)
      {
         QuadrupedTimedStep step = addZeroStep(startTime + stepIndex * (swingDuration + endPairSupportDuration),
               quadrant);

         // Set initial stance position based on stanceWidth/stanceHeight in the body frame
         FramePoint stepGoalPosition = step.getGoalPosition();
         stepGoalPosition.setToZero(referenceFrames.getBodyZUpFrame());

         double initialX = quadrant.getEnd().negateIfHindEnd(stanceLength / 2.0);
         double initialY = quadrant.getSide().negateIfRightSide(stanceWidth / 2.0);

         stepGoalPosition.set(initialX, initialY, 0.0);

         // Project onto the ground.
         stepGoalPosition.changeFrame(referenceFrames.getWorldFrame());
         stepGoalPosition.setZ(0.0);

         // Shift right feet forward half a stride.
         if (quadrant.isQuadrantOnRightSide())
         {
            step.getGoalPosition().add(strideLength / 2.0, strideWidth / 2.0, 0.0);
         }

         stepIndex++;
      }

      return 4 * (swingDuration + endPairSupportDuration);
   }

   private void addFourSteps(double startTime)
   {
      // Compute the period of this end pair, or the time between steps of the same foot.
      double endPairPeriod = 2.0 * swingDuration + 2.0 * endPairSupportDuration;

      // Compute the time these steps should be delayed to meet the desired phase shift.
      double phaseShiftDt = endPairPeriod * phaseShift / 360.0;

      addStepsForEndPair(startTime, RobotEnd.HIND);
      addStepsForEndPair(startTime + phaseShiftDt, RobotEnd.FRONT);
   }

   private void addStepsForEndPair(double startTime, RobotEnd end)
   {
      addStepFromPrevious(startTime, RobotQuadrant.getQuadrant(end, RobotSide.LEFT));
      addStepFromPrevious(startTime + swingDuration + endPairSupportDuration,
            RobotQuadrant.getQuadrant(end, RobotSide.RIGHT));
   }

   /**
    * Add a step based on the position of the previous step of the same quadrant.
    *
    * @see #addZeroStep(double, RobotQuadrant)
    */
   private QuadrupedTimedStep addStepFromPrevious(double startTime, RobotQuadrant quadrant)
   {
      QuadrupedTimedStep previous = queues.get(quadrant).getTail();

      // Start with the new step positioned at the previous step for this quadrant.
      QuadrupedTimedStep step = addZeroStep(startTime, quadrant);
      step.getGoalPosition().setIncludingFrame(previous.getGoalPosition());

      // Compute the support polygon for the previous group of four footsteps.
      QuadrupedSupportPolygon polygon = new QuadrupedSupportPolygon();
      for (RobotQuadrant q : RobotQuadrant.values)
      {
         polygon.setFootstep(q, queues.get(q).getTail().getGoalPosition());
      }
      polygon.changeFrame(referenceFrames.getWorldFrame());

      // From the previous support polygon, yaw the next support polygon by the desired turn rate.
      double nominalYaw = polygon.getNominalYaw();
      polygon.yawAboutCentroid(yawRate);

      // Compute the stride vector and rotate it to the new yaw angle.
      FrameVector stride = pool.lease(FrameVector.class);
      stride.setToZero(referenceFrames.getWorldFrame());
      stride.add(strideLength, strideWidth, 0.0);

      RigidBodyTransform tf = pool.lease(RigidBodyTransform.class);
      tf.rotZ(nominalYaw);
      stride.applyTransform(tf);

      step.setGoalPosition(polygon.getFootstep(quadrant));
      step.getGoalPosition().add(stride);

      return step;
   }

   /**
    * Add a step with a goal position at the current foot position.
    *
    * @param startTime the swing phase start time for this step
    * @param quadrant  the quadrant for this step
    * @return the new step
    */
   private QuadrupedTimedStep addZeroStep(double startTime, RobotQuadrant quadrant)
   {
      queues.get(quadrant).enqueue();
      QuadrupedTimedStep step = queues.get(quadrant).getTail();

      step.setRobotQuadrant(quadrant);
      step.getTimeInterval().setInterval(0.0, swingDuration);
      step.getTimeInterval().shiftInterval(startTime);

      step.getGoalPosition().setToZero(referenceFrames.getFootFrame(quadrant));

      return step;
   }

   private void mergeFootstepQueues(PreallocatedQueue<QuadrupedTimedStep> steps)
   {
      while (true)
      {
         RobotQuadrant smallestQuadrant = null;
         for (RobotQuadrant quadrant : RobotQuadrant.values)
         {
            if (queues.get(quadrant).size() == 0)
            {
               continue;
            }

            if (smallestQuadrant == null)
            {
               smallestQuadrant = quadrant;
            }

            double smallestTime = queues.get(smallestQuadrant).getHead().getTimeInterval().getStartTime();
            double thisTime = queues.get(quadrant).getHead().getTimeInterval().getStartTime();
            if (thisTime < smallestTime)
            {

               smallestQuadrant = quadrant;
            }
         }

         if (smallestQuadrant == null)
         {
            break;
         }

         steps.enqueue();
         steps.getTail().set(queues.get(smallestQuadrant).getHead());

         queues.get(smallestQuadrant).dequeue();
      }
   }

   public void setVisible(boolean visible)
   {
      footstepVisualization.setVisible(visible);
   }

   public double getSwingDuration()
   {
      return swingDuration;
   }

   public void setSwingDuration(double swingDuration)
   {
      this.swingDuration = swingDuration;
   }

   public double getEndPairSupportDuration()
   {
      return endPairSupportDuration;
   }

   public void setEndPairSupportDuration(double endPairSupportDuration)
   {
      this.endPairSupportDuration = endPairSupportDuration;
   }

   public double getPhaseShift()
   {
      return phaseShift;
   }

   public void setPhaseShift(double phaseShift)
   {
      this.phaseShift = phaseShift;
   }

   public double getStanceLength()
   {
      return stanceLength;
   }

   public void setStanceLength(double stanceLength)
   {
      this.stanceLength = stanceLength;
   }

   public double getStanceWidth()
   {
      return stanceWidth;
   }

   public void setStanceWidth(double stanceWidth)
   {
      this.stanceWidth = stanceWidth;
   }

   public double getStrideLength()
   {
      return strideLength;
   }

   public void setStrideLength(double strideLength)
   {
      this.strideLength = strideLength;
   }

   public double getStrideWidth()
   {
      return strideWidth;
   }

   public double getYawRate()
   {
      return yawRate;
   }

   public void setYawRate(double yawRate)
   {
      this.yawRate = yawRate;
   }

   public void setStrideWidth(double strideWidth)
   {
      this.strideWidth = strideWidth;
   }
}
