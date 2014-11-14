package us.ihmc.simulationconstructionset.gui.dialogConstructors;


import java.awt.Container;

import javax.swing.JFrame;

import us.ihmc.simulationconstructionset.SimulationConstructionSet;
import us.ihmc.simulationconstructionset.gui.dialogs.PlaybackPropertiesDialog;

public class PlaybackPropertiesDialogGenerator implements PlaybackPropertiesDialogConstructor
{
   private SimulationConstructionSet sim;
   private JFrame frame;
   private Container parentContainer;

   public PlaybackPropertiesDialogGenerator(SimulationConstructionSet sim, Container parentContainer, JFrame frame)
   {
      this.sim = sim;
      this.frame = frame;
      this.parentContainer = parentContainer;
   }

   public void constructPlaybackPropertiesDialog()
   {
      new PlaybackPropertiesDialog(parentContainer, frame, sim);
   }
}

