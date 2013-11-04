package us.ihmc.robotDataCommunication.logger;

import gnu.trove.list.array.TLongArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import us.ihmc.robotDataCommunication.logger.util.FFMpeg;
import us.ihmc.robotDataCommunication.logger.util.PipedCommandExecutor;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaViewer;
import com.xuggle.mediatool.IMediaViewer.Mode;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec.Type;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IStream;
import com.yobotics.simulationconstructionset.LongYoVariable;
import com.yobotics.simulationconstructionset.PlaybackListener;
import com.yobotics.simulationconstructionset.SimulationRewoundListener;

public class VideoDataPlayer implements PlaybackListener, SimulationRewoundListener
{

   private final boolean interlaced;
   
   private final TLongArrayList robotTimestamps = new TLongArrayList();
   private final TLongArrayList videoTimestamps = new TLongArrayList();

   
   private final LongYoVariable timestamp;
   private final int videoStream;
   private final IMediaReader reader;
   
   
   private int currentlyShowingIndex = 0;
   private long currentlyShowingRobottimestamp = 0;
   private long upcomingRobottimestamp = 0;
   
   private volatile boolean updateVideo = true;
   private final IMediaViewer viewer;

   private final File videoFile;
   
   public VideoDataPlayer(File dataDirectory, LogProperties logProperties, LongYoVariable timestamp)
   {
      this.interlaced = logProperties.getInterlaced();
      videoFile = new File(dataDirectory, logProperties.getVideoFile());
      File timestampFile = new File(dataDirectory, logProperties.getTimestampFile());

      parseTimestampData(timestampFile);
      
      this.timestamp = timestamp;
      reader = ToolFactory.makeReader(videoFile.getAbsolutePath());
      reader.open();
      int videoStream = -1;
      for(int i = 0; i < reader.getContainer().getNumStreams(); i++)
      {
         IStream stream = reader.getContainer().getStream(i);
         if(stream.getStreamCoder().getCodecType() == Type.CODEC_TYPE_VIDEO)
         {
            videoStream = i;
            break;
         }
      }
      if(videoStream == -1)
      {
         throw new RuntimeException("Cannot find video stream in " + videoFile.getAbsolutePath());
      }
      this.videoStream = videoStream;
      
      
      viewer = ToolFactory.makeViewer(Mode.FAST_VIDEO_ONLY, false);
      reader.addListener(viewer);
      reader.readPacket();
   }

   public void showVideoFrame(long timestamp)
   {
      if(timestamp >= currentlyShowingRobottimestamp && timestamp < upcomingRobottimestamp)
      {
         return;
      }
      
      long videoTimestamp = getVideoTimestamp(timestamp);
      
      if(currentlyShowingIndex + 1 < robotTimestamps.size())
      {
         upcomingRobottimestamp = robotTimestamps.get(currentlyShowingIndex + 1);
      }
      else
      {
         upcomingRobottimestamp = currentlyShowingRobottimestamp;
      }
      
      int retval = reader.getContainer().seekKeyFrame(videoStream, videoTimestamp, 0);
      if(retval != 0)
      {
         System.err.println(IError.make(retval).getDescription());
      }
      reader.readPacket();
   }

   private long getVideoTimestamp(long timestamp)
   {
      for(int i = robotTimestamps.size() - 1; i >= 0; i--)
      {
         long nextTimestamp = robotTimestamps.get(i);
         if(timestamp >= nextTimestamp)
         {
            currentlyShowingRobottimestamp = nextTimestamp;
            currentlyShowingIndex = i;
            break;
         }
      }
      
      long videoTimestamp = videoTimestamps.get(currentlyShowingIndex);
      return videoTimestamp;
   }
   
   private void parseTimestampData(File timestampFile)
   {
      try
      {
         BufferedReader reader = new BufferedReader(new FileReader(timestampFile));
         String line;
         while ((line = reader.readLine()) != null)
         {
            String[] stamps = line.split("\\s");
            long robotStamp = Long.valueOf(stamps[0]);
            long videoStamp = Long.valueOf(stamps[1]);
            
            if(interlaced)
            {
               videoStamp /= 2;
            }

            robotTimestamps.add(robotStamp);
            videoTimestamps.add(videoStamp);

         }

         reader.close();
      }
      catch (FileNotFoundException e)
      {
         throw new RuntimeException(e);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }
   }

   
   @Override
   public void indexChanged(int newIndex, double newTime)
   {
      if(updateVideo)
      {
         showVideoFrame(timestamp.getLongValue());
      }
   }

   @Override
   public void play(double realTimeRate)
   {
      
   }

   @Override
   public void stop()
   {
      
   }

   @Override
   public void simulationWasRewound()
   {
      showVideoFrame(timestamp.getLongValue());
   }

   public void updateVideo(boolean update)
   {
      this.updateVideo = update;
   }

   public long getCurrentTimestamp()
   {
      return timestamp.getLongValue();
   }

   public void exportVideo(File selectedFile, long startTimestamp, long endTimestamp)
   {
      
      long startVideoTimestamp = getVideoTimestamp(startTimestamp);
      long endVideoTimestamp = getVideoTimestamp(endTimestamp);
      
      double timebase = reader.getContainer().getStream(videoStream).getTimeBase().getDouble();
      
      double startTime = startVideoTimestamp * timebase;
      double endTime = endVideoTimestamp * timebase;
      
      FFMpeg ffMpeg  = new FFMpeg(true);
      
      ffMpeg.setStarttime(startTime);
      ffMpeg.setEndtime(endTime);
      ffMpeg.setInputFile(videoFile.getAbsolutePath());
      ffMpeg.setVideoCodec("h264");
      ffMpeg.setAudioCodec("aac");
      ffMpeg.enableExperimentalCodecs(true);
      ffMpeg.setOutputFile(selectedFile.getAbsolutePath());
      
      PipedCommandExecutor executor = new PipedCommandExecutor(ffMpeg);
      try
      {
         executor.execute();
      }
      catch (IOException e)
      {
         e.printStackTrace();
      }
      
   }

}
