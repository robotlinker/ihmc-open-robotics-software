package us.ihmc.tools.inputDevices.keyboard.linux;

import org.junit.Test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
@Tag("fast")
public class RepeatingReleasedEventsFixerTest
{
   @Test(timeout = 30000)
   public void testInstallAndRemove()
   {
      RepeatingReleasedEventsFixer repeatingReleasedEventsFixer = new RepeatingReleasedEventsFixer();
      repeatingReleasedEventsFixer.install();
      
      // TODO test some dispatched events here
      
      repeatingReleasedEventsFixer.remove();
   }
}
