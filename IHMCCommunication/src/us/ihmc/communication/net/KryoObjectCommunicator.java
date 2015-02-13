package us.ihmc.communication.net;

import java.awt.Container;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import us.ihmc.utilities.FormattingTools;
import us.ihmc.utilities.ThreadTools;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.Listener;

public abstract class KryoObjectCommunicator implements ObjectCommunicator
{

   private final LinkedHashMap<Class<?>, ExecutorService> listenerExecutors = new LinkedHashMap<Class<?>, ExecutorService>();
   private final LinkedHashMap<Class<?>, ArrayList<ObjectConsumer<?>>> listeners = new LinkedHashMap<Class<?>, ArrayList<ObjectConsumer<?>>>();
   

   private final ArrayList<NetStateListener> stateListeners = new ArrayList<NetStateListener>();
   
   private final ArrayList<GlobalObjectConsumer> globalListeners = new ArrayList<GlobalObjectConsumer>();
   
   // Stuff for data count table 
   private DefaultTableModel dataRateTable;
   private LinkedHashMap<Class<?>, TableData> dataCounts;
   private long startTime = 0;
   
   
   public void showBandwidthDialog(String name)
   {
      if(dataRateTable == null)
      {
         dataCounts = new LinkedHashMap<Class<?>, TableData>();
         JFrame jFrame = new JFrame(name);
         Container contentPane = jFrame.getContentPane();
         
         dataRateTable = new DefaultTableModel(new Object[]{"Object", "Total bits", "bps"}, 0);
         
         int rowNumber = 0;
         for(Class<?> clazz : listeners.keySet())
         {
            dataCounts.put(clazz, new TableData(rowNumber));
            dataRateTable.addRow(new Object[] { clazz.getSimpleName(), 0, 0 });
            rowNumber++;
         }
         
         JTable table = new JTable(dataRateTable);
         JScrollPane scrollPane = new JScrollPane(table);
         contentPane.add(scrollPane);
         
         
         jFrame.pack();
         jFrame.setSize(400, 500);
         jFrame.setLocationByPlatform(true);
         jFrame.setVisible(true);
      }
   }
   
   protected void registerClassList(NetClassList classList, Kryo kryo)
   {
	   if(!listeners.containsKey(Object.class))
	   {
		   listeners.put(Object.class, new ArrayList<ObjectConsumer<?>>());
	   }
	   
	   for(Class<?> clazz : classList.getPacketClassList())
	   {
	      kryo.register(clazz);
		   listeners.put(clazz, new ArrayList<ObjectConsumer<?>>());
		   listenerExecutors.put(clazz, Executors.newFixedThreadPool(1, ThreadTools.getNamedThreadFactory("Kryo" + clazz.getSimpleName() + "Listener")));
	   }
	   
	   for(Class<?> type : classList.getPacketFieldList())
	   {
		   kryo.register(type);
	   }
   }
   
   @Override
   public void attachStateListener(NetStateListener stateListener)
   {
      stateListeners.add(stateListener);
   }
   
   @Override
   public <T> void attachListener(Class<T> clazz, ObjectConsumer<T> listener)
   {
      if(listeners.containsKey(clazz))
      {
         listeners.get(clazz).add(listener);
      }
      else
      {
         throw new RuntimeException("Class " + clazz.getSimpleName() + " is not registered with ObjectCommunicator");
      }
   }
   
   @Override
   public <T> void detachListener(Class<T> clazz, ObjectConsumer<T> listener)
   {
      if(listeners.containsKey(clazz))
      {
         ArrayList<ObjectConsumer<?>> listenerList = listeners.get(clazz);
         if (listenerList.contains(listener))
         {
            listenerList.remove(listener);
         }
      }
   }
   
   @Override
   public void attachGlobalListener(GlobalObjectConsumer listener)
   {
	   globalListeners.add(listener);
   }
   
   @Override
   public void detachGlobalListener(GlobalObjectConsumer listener)
   {
      if (globalListeners.contains(listener))
      {
         globalListeners.remove(listener);
      }
   }
   
   @Override
   public synchronized void consumeObject(Object object)
   {
      consumeObject(object, true);
   }
   
   @Override
   public synchronized void consumeObject(Object object, boolean consumeGlobal)
   {
      if (!consumeGlobal && !listeners.containsKey(object.getClass()))
      {
         throw new RuntimeException(object.getClass().getSimpleName() + " not registered with ObjectCommunicator");
      }
      int bytesSend = sendTCP(object);
      updateDataRateTable(object, bytesSend);
      
   }

   private void updateDataRateTable(Object object, int bytesSend)
   {
      if(dataRateTable != null)
      {
         if(startTime == 0)
         {
            startTime = System.nanoTime();
         }
         
         TableData tableData = dataCounts.get(object.getClass());
         double wallTime = (System.nanoTime() - startTime) / 1e9;
         tableData.addData(bytesSend * 8, wallTime);
         dataRateTable.setValueAt(FormattingTools.toHumanReadable(tableData.getTotalBits()), tableData.getRow(), 1);
         dataRateTable.setValueAt(FormattingTools.toHumanReadable(tableData.getBitsPerSecond()), tableData.getRow(), 2);         
      }
   }
   
   protected final void createConnectionListener(EndPoint endPoint)
   {
      Listener listener = new Listener()
      {
         @Override
         @SuppressWarnings("unchecked")
         public void received(Connection connection, final Object object)
         {
            final Class<? extends Object> classType = object.getClass();
            ExecutorService executorService = listenerExecutors.get(classType);
            if(executorService != null)
            {
               executorService.execute(new Runnable()
               {
                  
                  @Override
                  public void run()
                  {
                     for(GlobalObjectConsumer listener : globalListeners)
                     {
                        listener.consumeObject(object, false);
                     }
                     
                     ArrayList<ObjectConsumer<?>> objectListeners = listeners.get(classType);
                     if(objectListeners != null)
                     {
                        for(@SuppressWarnings("rawtypes") ObjectConsumer listener : objectListeners)
                        {
                           listener.consumeObject(object);
                        }
                     }
                  }
               });
            }
            
            else if (!(object instanceof KeepAlive))
            {
               System.err.println("Received unkown object of class " + classType);
            }
         }
         
         
         @Override
         public void connected(Connection connection)
         {
            for(NetStateListener stateListener : stateListeners)
            {
               stateListener.connected();
            }
         }
         
         @Override
         public void disconnected(Connection connection)
         {
            for(NetStateListener stateListener : stateListeners)
            {
               stateListener.disconnected();
            }
         }

      };
      endPoint.addListener(listener);
   }

   @Override
   public final void connect() throws IOException
   {
      openConnection();
   }
   
   @Override
   public final void close()
   {
      closeConnection();
      for(ExecutorService executor : listenerExecutors.values())
      {
         executor.shutdownNow();
      }
   }
   
   protected abstract void openConnection() throws IOException;
   
   protected abstract int sendUDP(Object object);

   protected abstract int sendTCP(Object object);
   
   @Override
   public abstract boolean isConnected();

   protected abstract void closeConnection();

   private class TableData
   {
      private final int row;
      private long totalBits = 0;
      private double bitsPerSecond = 0;
      
      public TableData(int row)
      {
         this.row = row;
      }
      
      public void addData(int bits, double wallTime)
      {
         totalBits += bits;
         bitsPerSecond = totalBits/wallTime;
      }
      
      public long getTotalBits()
      {
         return totalBits;
      }
      
      public double getBitsPerSecond()
      {
         return bitsPerSecond;
      }
      
      public int getRow()
      {
         return row;
      }
   
   }

}
