package us.ihmc.quadrupedRobotics.estimator.stateEstimator;

import us.ihmc.commonWalkingControlModules.sensors.footSwitch.SettableFootSwitch;
import us.ihmc.commonWalkingControlModules.touchdownDetector.ForceBasedTouchDownDetection;
import us.ihmc.commonWalkingControlModules.touchdownDetector.JointTorqueBasedTouchdownDetector;
import us.ihmc.commons.PrintTools;
import us.ihmc.robotModels.FullQuadrupedRobotModel;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.robotics.contactable.ContactablePlaneBody;
import us.ihmc.robotics.partNames.LegJointName;
import us.ihmc.robotics.robotSide.QuadrantDependentList;
import us.ihmc.robotics.robotSide.RobotQuadrant;
import us.ihmc.robotics.screwTheory.TotalMassCalculator;
import us.ihmc.robotics.sensors.FootSwitchInterface;
import us.ihmc.sensorProcessing.stateEstimation.FootSwitchType;
import us.ihmc.tools.factories.FactoryTools;
import us.ihmc.tools.factories.RequiredFactoryField;

public class QuadrupedFootSwitchFactory
{
   // Factory fields
   private final RequiredFactoryField<Double> gravity = new RequiredFactoryField<>("gravity");
   private final RequiredFactoryField<YoVariableRegistry> yoVariableRegistry = new RequiredFactoryField<>("yoVariableRegistry");
   protected final RequiredFactoryField<QuadrantDependentList<ContactablePlaneBody>> footContactableBodies = new RequiredFactoryField<>(
         "footContactableBodies");
   private final RequiredFactoryField<FullQuadrupedRobotModel> fullRobotModel = new RequiredFactoryField<>("fullRobotModel");
   private final RequiredFactoryField<FootSwitchType> footSwitchType = new RequiredFactoryField<>("footSwitchType");
   private final RequiredFactoryField<QuadrantDependentList<Double>> kneeTorqueTouchdownThreshold = new RequiredFactoryField<>("kneeTorqueTouchdownThreshold");

   // Private fields
   protected final YoVariableRegistry registry = new YoVariableRegistry("QuadrupedFootSwitchManagerRegistry");

   protected void setupTouchdownBasedFootSwitches(QuadrantDependentList<FootSwitchInterface> footSwitches, double totalRobotWeight)
   {
      FactoryTools.checkAllFactoryFieldsAreSet(this);

      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         QuadrupedTouchdownDetectorBasedFootSwitch touchdownDetectorBasedFootSwitch = new QuadrupedTouchdownDetectorBasedFootSwitch(robotQuadrant,
                                                                                                                                    footContactableBodies.get()
                                                                                                                                                         .get(robotQuadrant),
                                                                                                                                    totalRobotWeight, registry);
         JointTorqueBasedTouchdownDetector jointTorqueBasedTouchdownDetector;
         boolean dontDetectTouchdownIfAtJointLimit = true;
         jointTorqueBasedTouchdownDetector = new JointTorqueBasedTouchdownDetector(fullRobotModel.get().getLegJoint(robotQuadrant, LegJointName.KNEE_PITCH), dontDetectTouchdownIfAtJointLimit, registry);
         jointTorqueBasedTouchdownDetector.setTorqueThreshold(kneeTorqueTouchdownThreshold.get().get(robotQuadrant));
         touchdownDetectorBasedFootSwitch.addTouchdownDetector(jointTorqueBasedTouchdownDetector);

         ForceBasedTouchDownDetection forceBasedTouchDownDetection = new ForceBasedTouchDownDetection(fullRobotModel.get(), robotQuadrant,
                                                                                                      footContactableBodies.get().get(robotQuadrant)
                                                                                                                           .getSoleFrame(), registry);
         touchdownDetectorBasedFootSwitch.addTouchdownDetector(forceBasedTouchDownDetection);

         footSwitches.set(robotQuadrant, touchdownDetectorBasedFootSwitch);
      }
   }

   protected void setupGroundContactPointFootSwitches(QuadrantDependentList<FootSwitchInterface> footSwitches, double totalRobotWeight)
   {
      PrintTools.warn(this, "simulatedRobot is not set, creating touchdown based foot switches.");
      setupTouchdownBasedFootSwitches(footSwitches, totalRobotWeight);
   }

   private void setupSettableFootSwitches(QuadrantDependentList<FootSwitchInterface> footSwitches, double totalRobotWeight)
   {
      for (RobotQuadrant robotQuadrant : RobotQuadrant.values)
      {
         SettableFootSwitch footSwitch = new SettableFootSwitch(footContactableBodies.get().get(robotQuadrant), robotQuadrant, totalRobotWeight, registry);
         footSwitches.set(robotQuadrant, footSwitch);
      }
   }

   public QuadrantDependentList<FootSwitchInterface> createFootSwitches()
   {
      FactoryTools.checkAllFactoryFieldsAreSet(this);

      yoVariableRegistry.get().addChild(registry);

      QuadrantDependentList<FootSwitchInterface> footSwitches = new QuadrantDependentList<FootSwitchInterface>();
      double gravityMagnitude = Math.abs(gravity.get());
      double totalRobotWeight = TotalMassCalculator.computeSubTreeMass(fullRobotModel.get().getElevator()) * gravityMagnitude;

      switch (footSwitchType.get())
      {
      case TouchdownBased:
         setupTouchdownBasedFootSwitches(footSwitches, totalRobotWeight);
         break;
      case WrenchBased:
         setupGroundContactPointFootSwitches(footSwitches, totalRobotWeight);
         break;
      default:
         setupSettableFootSwitches(footSwitches, totalRobotWeight);
      }

      FactoryTools.disposeFactory(this);

      return footSwitches;
   }

   // Factory setters

   public void setGravity(double gravity)
   {
      this.gravity.set(gravity);
   }

   public void setYoVariableRegistry(YoVariableRegistry yoVariableRegistry)
   {
      this.yoVariableRegistry.set(yoVariableRegistry);
   }

   public void setFootContactableBodies(QuadrantDependentList<ContactablePlaneBody> footContactableBodies)
   {
      this.footContactableBodies.set(footContactableBodies);
   }

   public void setFullRobotModel(FullQuadrupedRobotModel fullRobotModel)
   {
      this.fullRobotModel.set(fullRobotModel);
   }

   public void setFootSwitchType(FootSwitchType footSwitchType)
   {
      this.footSwitchType.set(footSwitchType);
   }

   public void setKneeTouchdownThresholds(QuadrantDependentList<Double> kneeTouchdownThresholds)
   {
      this.kneeTorqueTouchdownThreshold.set(kneeTouchdownThresholds);
   }
}
