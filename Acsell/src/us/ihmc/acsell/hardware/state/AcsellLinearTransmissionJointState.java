package us.ihmc.acsell.hardware.state;

import us.ihmc.acsell.hardware.state.slowSensors.StrainSensor;
import us.ihmc.yoVariables.registry.YoVariableRegistry;
import us.ihmc.yoVariables.variable.YoDouble;
import us.ihmc.robotics.geometry.AngleTools;

public class AcsellLinearTransmissionJointState implements AcsellJointState
{
   private final YoVariableRegistry registry;

   private final AcsellActuatorState actuator;

   private final StrainSensor strainSensor;
   private final double ratio;
   private final boolean hasOutputEncoder;

   private double motorAngle;
   
   private final YoDouble q;
   private final YoDouble qd;
   private final YoDouble tau_strain;
   private final YoDouble tau_current;
   private final YoDouble tau_error;

   public AcsellLinearTransmissionJointState(String name, double ratio, boolean hasOutputEncoder, AcsellActuatorState actuator, StrainSensor strainSensor, YoVariableRegistry parentRegistry)
   {
      this.registry = new YoVariableRegistry(name);
      this.ratio = ratio;
      this.actuator = actuator;
      this.hasOutputEncoder = hasOutputEncoder;
      this.strainSensor = strainSensor;

      this.q = new YoDouble(name + "_q", registry);
      this.qd = new YoDouble(name + "_qd", registry);
      this.tau_current = new YoDouble(name + "_tauPredictedCurrent", registry);
      this.tau_strain = new YoDouble(name + "_tauMeasuredStrain", registry);
      this.tau_error = new YoDouble(name + "_tauCurrentStrainError", registry);
      
      parentRegistry.addChild(registry);
   }

   @Override
   public double getQ()
   {
      return q.getDoubleValue();
   }

   @Override
   public double getQd()
   {
      return qd.getDoubleValue();
   }

   @Override
   public double getTau()
   {
      return tau_current.getDoubleValue();
   }

   @Override
   public void update()
   {
      
      motorAngle = actuator.getMotorPosition();
      if(hasOutputEncoder)
      {
         q.set(AngleTools.trimAngleMinusPiToPi(actuator.getJointPosition()));
         qd.set(actuator.getJointVelocity());
      }
      else
      {
         q.set(AngleTools.trimAngleMinusPiToPi(actuator.getMotorPosition() / ratio)); 
         qd.set(actuator.getMotorVelocity() / ratio); 
      }
      tau_current.set(actuator.getMotorTorque() * ratio);
      if(strainSensor!=null)
      {
         tau_strain.set(strainSensor.getCalibratedValue());
         tau_error.set(tau_strain.getDoubleValue()-tau_current.getDoubleValue());
      }
   }

   @Override
   public int getNumberOfActuators()
   {
      return 1;
   }

   @Override
   public double getMotorAngle(int actuator)
   {
      return motorAngle;
   }

   @Override
   public void updateOffsets()
   {
   }

}
