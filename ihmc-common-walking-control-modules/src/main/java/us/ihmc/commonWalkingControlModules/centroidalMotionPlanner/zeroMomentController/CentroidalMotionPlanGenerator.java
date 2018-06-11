package us.ihmc.commonWalkingControlModules.centroidalMotionPlanner.zeroMomentController;

import java.util.List;

import us.ihmc.commonWalkingControlModules.centroidalMotionPlanner.zeroMomentSQPPlanner.MotionPlannerParameters;
import us.ihmc.commonWalkingControlModules.controlModules.flight.ContactState;
import us.ihmc.euclid.referenceFrame.interfaces.FramePoint3DReadOnly;
import us.ihmc.euclid.referenceFrame.interfaces.FrameVector3DReadOnly;

public interface CentroidalMotionPlanGenerator
{
   boolean compute();

   void clearContactStateList();
   
   void submitContactStateList(List<ContactState> contactStates);

   void update();

   void prepareTransitionToNextContactState();
   
   CentroidalMotionPlan getMotionPlanReference();

   void setInitialState(FramePoint3DReadOnly comPosition, FrameVector3DReadOnly comVelocity, FramePoint3DReadOnly copPosition);

   void setFinalState(FramePoint3DReadOnly comPosition, FrameVector3DReadOnly comVelocity, FramePoint3DReadOnly copPosition);

   void initialize(MotionPlannerParameters plannerParameters, FrameVector3DReadOnly gravity);

   int getMaximumNumberOfContactStatesToPlan();
}