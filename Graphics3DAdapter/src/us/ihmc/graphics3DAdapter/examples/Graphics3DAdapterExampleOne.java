package us.ihmc.graphics3DAdapter.examples;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Random;

import javax.media.j3d.Transform3D;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;

import us.ihmc.graphics3DAdapter.Graphics3DAdapter;
import us.ihmc.graphics3DAdapter.NodeType;
import us.ihmc.graphics3DAdapter.SelectedListener;
import us.ihmc.graphics3DAdapter.graphics.LinkGraphics;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearance;
import us.ihmc.graphics3DAdapter.graphics.appearances.YoAppearanceRGBColor;
import us.ihmc.graphics3DAdapter.graphics.instructions.LinkGraphicsInstruction;
import us.ihmc.graphics3DAdapter.structure.Graphics3DNode;
import us.ihmc.utilities.ThreadTools;

public class Graphics3DAdapterExampleOne
{
   public void doExampleOne(Graphics3DAdapter adapter)
   {
      Graphics3DNode teapotAndSphereNode = new Graphics3DNode("teaPot", NodeType.JOINT);
      LinkGraphics teapotObject = createTeapotObject();
      LinkGraphicsInstruction sphereAppearanceHolder = teapotObject.addSphere(2.0, YoAppearance.Red());

      teapotAndSphereNode.setGraphicsObject(teapotObject);
      adapter.addRootNode(teapotAndSphereNode);

      Canvas canvas = adapter.getDefaultCamera().getCanvas();
      JPanel panel = new JPanel(new BorderLayout());
      panel.add("Center", canvas);
      
      JFrame jFrame = new JFrame("Example One");
      Container contentPane = jFrame.getContentPane();
      contentPane.setLayout(new BorderLayout());
      contentPane.add("Center", panel);
      
      jFrame.pack();
      jFrame.setVisible(true);
      jFrame.setSize(800, 600);
      
      
      SelectedListener selectedListener = new SelectedListener()
      {
         
         public void selected(Graphics3DNode graphics3dNode, String modifierKey)
         {
            System.out.println("Selected " + graphics3dNode.getName());
         }
      };
      
      
      adapter.addSelectedListener(selectedListener);
      teapotAndSphereNode.addSelectedListener(selectedListener);
      
      RotateAndScaleNodeRunnable rotator = new RotateAndScaleNodeRunnable(teapotAndSphereNode);
      BlinkRunnable blinker = new BlinkRunnable(sphereAppearanceHolder);
      AddAndRemoveObjectRunnable addAndRemoveObjectsRunnable = new AddAndRemoveObjectRunnable(teapotAndSphereNode);
     

      ArrayList<Runnable> runnables = new ArrayList<Runnable>();
//      runnables.add(rotator);
//      runnables.add(blinker);

      while (true)
      {
         for (Runnable runnable : runnables)
         {
            runnable.run();
         }

         try
         {
            Thread.sleep(10L);
         }
         catch (InterruptedException e)
         {
            e.printStackTrace();
         }
      }
   }

   
   public void doExampleTwo(Graphics3DAdapter adapter)
   {
      Random random = new Random(1776L);
      
      Graphics3DNode node1 = new Graphics3DNode("node1", NodeType.JOINT);
      
      Transform3D transform1 = new Transform3D();
      transform1.setTranslation(new Vector3d(2.0, 0.0, 0.0));
      node1.setTransform(transform1);
      
      Graphics3DNode node2 = new Graphics3DNode("node2", NodeType.JOINT);
      Graphics3DNode rootNode = new Graphics3DNode("rootNode", NodeType.JOINT);
     
      LinkGraphics object2 = createCubeObject(0.6);
      LinkGraphics object1 = createRandomObject(random);
      
      node1.setGraphicsObject(object1);
      node2.setGraphicsObject(object2);
      
      rootNode.addChild(node1);
      rootNode.addChild(node2);
      
      adapter.addRootNode(rootNode);

      Canvas canvas = adapter.getDefaultCamera().getCanvas();
      JPanel panel = new JPanel(new BorderLayout());
      panel.add("Center", canvas);
      
      JFrame jFrame = new JFrame("Example Two");
      Container contentPane = jFrame.getContentPane();
      contentPane.setLayout(new BorderLayout());
      contentPane.add("Center", panel);
      
      jFrame.pack();
      jFrame.setVisible(true);
      jFrame.setSize(800, 600);
      
      double rotation = 0.0;
      
      int count = 0;
      
      while (true)
      {
         rotation = rotation + 0.01;
         node2.getTransform().rotZ(rotation);
         
         count++;
         if (count > 200)
         {
            LinkGraphics randomObject = createRandomObject(random);
            node1.setGraphicsObject(randomObject);
            count = 0;
         }
         
         ThreadTools.sleep(1L);
      }
   }
   
   
   private LinkGraphics createTeapotObject()
   {
	      //teapot = assetManager.loadModel("Models/Teapot/Teapot.mesh.xml");
      LinkGraphics teapotObject = new LinkGraphics();
//      teapotObject.addModelFile("Models/Teapot/Teapot.mesh.xml");
      teapotObject.translate(0.0, 1.0, 1.0);
      teapotObject.rotate(Math.PI / 4.0, LinkGraphics.X);
      teapotObject.addEllipsoid(2.0, 2.0, 1.5);
      teapotObject.translate(0.0, 2.0, 1.0);
      teapotObject.rotate(Math.PI / 4.0, LinkGraphics.X);

      return teapotObject;
   }
   
   private LinkGraphics createSphereObject(double radius)
   {
      LinkGraphics sphere = new LinkGraphics();
      sphere.addSphere(radius, YoAppearance.Green());

      return sphere;
   }
   
   private LinkGraphics createCylinderObject(double radius)
   {
      LinkGraphics cylinder = new LinkGraphics();
      double height = 1.0;
      cylinder.addCylinder(height, radius, YoAppearance.Pink());

      return cylinder;
   }
   
   private LinkGraphics createCubeObject(double lengthWidthHeight)
   {
      LinkGraphics cube = new LinkGraphics();
      cube.addCube(lengthWidthHeight, lengthWidthHeight, lengthWidthHeight, YoAppearance.Red());

      return cube;
   }
   
   private LinkGraphics createRandomObject(Random random)
   {
      int selection = random.nextInt(3);
      
      switch (selection)
      {
      case 0:
      {
         return createCubeObject(random.nextDouble());
      }
      case 1:
      {
         return createSphereObject(random.nextDouble() * 0.5);
      }
      case 2:
      {
         return createCylinderObject(random.nextDouble() * 0.5);
      }
      default:
      {
         throw new RuntimeException("Should not get here");
      }
      }
   }

   
   private class BlinkRunnable implements Runnable
   {
      private final LinkGraphicsInstruction instruction;
      private double transparency = 0.0;
      
      public BlinkRunnable(LinkGraphicsInstruction instruction)
      {
         this.instruction = instruction;
      }

      public void run()
      {
         transparency += 0.01;
         if (transparency > 1.0) transparency = 0.0;
         
         Color3f color = new Color3f((float) Math.random(), (float) Math.random(), (float) Math.random());
         YoAppearanceRGBColor appearance = new YoAppearanceRGBColor(color);
         appearance.setTransparancy(transparency);
         instruction.setAppearance(appearance);
      }
   
   }
   
   private class RotateAndScaleNodeRunnable implements Runnable
   {
      private final Graphics3DNode node;
      private double rotation = 0.0;
      private double scale = 1.0;
      private boolean scalingDown = true;
      
      public RotateAndScaleNodeRunnable(Graphics3DNode node)
      {
         this.node = node;
      }

      public void run()
      {
         rotation += 0.01;
         
         if (scalingDown)
         {
            scale = scale * 0.99;
            if (scale < 0.1) scalingDown = false;
         }
         else
         {
            scale = scale * 1.01;
            if (scale > 2.0) scalingDown = true;
         }
         
         Transform3D transform = new Transform3D();
         transform.rotZ(rotation);
         transform.setScale(scale);
         node.setTransform(transform);
      }
   }


   private class AddAndRemoveObjectRunnable
   {
      private Graphics3DNode graphics3DNode;
      
      public AddAndRemoveObjectRunnable(Graphics3DNode rootNode)
      {
         // TODO Auto-generated constructor stub
      }
      
   }

}
