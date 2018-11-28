package us.ihmc.quadrupedRobotics.estimator.stateEstimator;

import us.ihmc.commonWalkingControlModules.sensors.footSwitch.TouchdownDetectorBasedFootswitch;
import us.ihmc.commonWalkingControlModules.touchdownDetector.TouchdownDetector;
import us.ihmc.euclid.referenceFrame.FramePoint2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.mecano.spatial.Wrench;
import us.ihmc.robotics.contactable.ContactablePlaneBody;
import us.ihmc.robotics.math.filters.GlitchFilteredYoBoolean;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoBoolean;
import us.ihmc.yoVariables.variable.YoFramePoint2D;

public class QuadrupedTouchdownDetectorBasedFootSwitch extends TouchdownDetectorBasedFootswitch
{
   private static final int defaultGlitchWindow = 3;

   private final ContactablePlaneBody foot;
   private final double totalRobotWeight;
   private final YoFramePoint2D yoResolvedCoP;
   private final GlitchFilteredYoBoolean touchdownDetected;
   private final YoBoolean trustTouchdownDetectors;

   public QuadrupedTouchdownDetectorBasedFootSwitch(RobotQuadrant robotQuadrant, ContactablePlaneBody foot, double totalRobotWeight, YoVariableRegistry parentRegistry)
   {
      this(robotQuadrant, foot, defaultGlitchWindow, totalRobotWeight, parentRegistry);
   }

   public QuadrupedTouchdownDetectorBasedFootSwitch(RobotQuadrant robotQuadrant, ContactablePlaneBody foot, int glitchWindow, double totalRobotWeight, YoVariableRegistry parentRegistry)
   {
      super(robotQuadrant.getCamelCaseName() + "QuadrupedTouchdownFootSwitch", parentRegistry);

      this.foot = foot;
      this.totalRobotWeight = totalRobotWeight;
      yoResolvedCoP = new YoFramePoint2D(foot.getName() + "ResolvedCoP", "", foot.getSoleFrame(), registry);
      touchdownDetected = new GlitchFilteredYoBoolean(robotQuadrant.getCamelCaseName() + "TouchdownDetected", registry, glitchWindow);
      trustTouchdownDetectors = new YoBoolean(robotQuadrant.getCamelCaseName() + "TouchdownDetectorsTrusted", registry);
   }

   public YoBoolean getControllerSetFootSwitch()
   {
      return controllerThinksHasTouchedDown;
   }

   public void addTouchdownDetector(TouchdownDetector touchdownDetector)
   {
      touchdownDetectors.add(touchdownDetector);
   }

   @Override
   public boolean hasFootHitGround()
   {
      boolean touchdown = true;
      for (int i = 0; i < touchdownDetectors.size(); i++)
      {
         TouchdownDetector touchdownDetector = touchdownDetectors.get(i);
         touchdownDetector.update();
         touchdown &= touchdownDetector.hasTouchedDown();
      }
      touchdownDetected.update(touchdown);

      if(trustTouchdownDetectors.getBooleanValue())
         return touchdownDetected.getBooleanValue();
      else
         return controllerThinksHasTouchedDown.getBooleanValue();
   }

   @Override
   public double computeFootLoadPercentage()
   {
      return Double.NaN;
   }

   @Override
   public void computeAndPackCoP(FramePoint2D copToPack)
   {
      copToPack.setToNaN(getMeasurementFrame());
   }

   @Override
   public void updateCoP()
   {
      yoResolvedCoP.setToZero();
   }

   @Override
   public void computeAndPackFootWrench(Wrench footWrenchToPack)
   {
      footWrenchToPack.setToZero();
      if (hasFootHitGround())
         footWrenchToPack.setLinearPartZ(totalRobotWeight / 4.0);
   }

   @Override
   public ReferenceFrame getMeasurementFrame()
   {
      return foot.getSoleFrame();
   }

   @Override
   public void trustFootSwitch(boolean trustFootSwitch)
   {
      this.trustTouchdownDetectors.set(trustFootSwitch);
   }

   @Override
   public void reset()
   {
      for (int i = 0; i < touchdownDetectors.size(); i++)
      {
         touchdownDetectors.get(i).reset();
      }

      touchdownDetected.set(false);
   }
}
