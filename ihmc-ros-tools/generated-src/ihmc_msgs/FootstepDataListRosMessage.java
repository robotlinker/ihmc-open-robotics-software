package ihmc_msgs;

public interface FootstepDataListRosMessage extends org.ros.internal.message.Message {
  static final java.lang.String _TYPE = "ihmc_msgs/FootstepDataListRosMessage";
  static final java.lang.String _DEFINITION = "## FootstepDataListRosMessage\n# This message commands the controller to execute a list of footsteps. See FootstepDataMessage for\n# information about defining a footstep. A message with a unique id equals to 0 will be interpreted as\n# invalid and will not be processed by the controller. This rule does not apply to the fields of this\n# message.\n\n# Defines the list of footstep to perform.\nihmc_msgs/FootstepDataRosMessage[] footstep_data_list\n\n# When CONTROL_DURATIONS is chosen:  The controller will try to achieve the swingDuration and the\n# transferDuration specified in the message. If a  footstep touches down early, the next step will not\n# be affected by this and the whole trajectory might finish  earlier then expected. When\n# CONTROL_ABSOLUTE_TIMINGS is chosen:  The controller will compute the expected times for swing start\n# and touchdown and attempt to start a footstep  at that time. If a footstep touches down early, the\n# following transfer will be extended to make up for this  time difference and the footstep plan will\n# finish at the expected time.\nuint8 execution_timing\n\n# The swingDuration is the time a foot is not in ground contact during a step. Each step in a list of\n# footsteps might have a different swing duration. The value specified here is a default value, used\n# if a footstep in this list was created without a swingDuration.\nfloat64 default_swing_duration\n\n# The transferDuration is the time spent with the feet in ground contact before a step. Each step in a\n# list of footsteps might have a different transfer duration. The value specified here is a default\n# value, used if a footstep in this list was created without a transferDuration.\nfloat64 default_transfer_duration\n\n# Specifies the time used to return to a stable standing stance after the execution of the footstep\n# list is finished. If the value is negative the defaultTransferDuration will be used.\nfloat64 final_transfer_duration\n\n# When OVERRIDE is chosen:  - The time of the first trajectory point can be zero, in which case the\n# controller will start directly at the first trajectory point. Otherwise the controller will prepend\n# a first trajectory point at the current desired position.  When QUEUE is chosen:  - The message must\n# carry the ID of the message it should be queued to.  - The very first message of a list of queued\n# messages has to be an OVERRIDE message.  - The trajectory point times are relative to the the last\n# trajectory point time of the previous message.  - The controller will queue the joint trajectory\n# messages as a per joint basis. The first trajectory point has to be greater than zero.\nuint8 execution_mode\n\n# Only needed when using QUEUE mode, it refers to the message Id to which this message should be\n# queued to. It is used by the controller to ensure that no message has been lost on the way. If a\n# message appears to be missing (previousMessageId different from the last message ID received by the\n# controller), the motion is aborted. If previousMessageId == 0, the controller will not check for the\n# ID of the last received message.\nint64 previous_message_id\n\n# A unique id for the current message. This can be a timestamp or sequence number. Only the unique id\n# in the top level message is used, the unique id in nested messages is ignored. Use\n# /output/last_received_message for feedback about when the last message was received. A message with\n# a unique id equals to 0 will be interpreted as invalid and will not be processed by the controller.\nint64 unique_id\n\n\n# This message utilizes \"enums\". Enum value information for this message follows.\n\n# \"execution_mode\" enum values:\nuint8 OVERRIDE=0 # This message will override the previous.\nuint8 QUEUE=1 # The previous message will first be executed before executing this message. When sending a series of queued messages, the very first has to be declared as OVERRIDE.\n\n# \"execution_timing\" enum values:\nuint8 CONTROL_DURATIONS=0 # During the execution of this message the controller will attempt to achieve the given durations for segments of the whole trajectory.\nuint8 CONTROL_ABSOLUTE_TIMINGS=1 # During the execution of this message the controller will attempt to achieve the absolute timings at the knot points relative to the start of execution.\n\n";
  static final byte OVERRIDE = 0;
  static final byte QUEUE = 1;
  static final byte CONTROL_DURATIONS = 0;
  static final byte CONTROL_ABSOLUTE_TIMINGS = 1;
  java.util.List<ihmc_msgs.FootstepDataRosMessage> getFootstepDataList();
  void setFootstepDataList(java.util.List<ihmc_msgs.FootstepDataRosMessage> value);
  byte getExecutionTiming();
  void setExecutionTiming(byte value);
  double getDefaultSwingDuration();
  void setDefaultSwingDuration(double value);
  double getDefaultTransferDuration();
  void setDefaultTransferDuration(double value);
  double getFinalTransferDuration();
  void setFinalTransferDuration(double value);
  byte getExecutionMode();
  void setExecutionMode(byte value);
  long getPreviousMessageId();
  void setPreviousMessageId(long value);
  long getUniqueId();
  void setUniqueId(long value);
}