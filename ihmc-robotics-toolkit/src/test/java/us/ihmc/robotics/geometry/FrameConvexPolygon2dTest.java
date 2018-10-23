package us.ihmc.robotics.geometry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Disabled;
import us.ihmc.euclid.geometry.ConvexPolygon2D;
import us.ihmc.euclid.geometry.interfaces.Vertex2DSupplier;
import us.ihmc.euclid.referenceFrame.FrameConvexPolygon2D;
import us.ihmc.euclid.referenceFrame.ReferenceFrame;
import us.ihmc.euclid.referenceFrame.tools.ReferenceFrameTools;

public class FrameConvexPolygon2dTest
{

   private final ReferenceFrame worldFrame = ReferenceFrame.getWorldFrame();
   private ConvexPolygon2D convexPolygon2d;
   private FrameConvexPolygon2D frameConvexPolygon2d;

   @BeforeEach
   public void setUp()
   {
      convexPolygon2d = createSomeValidPolygon();
      frameConvexPolygon2d = new FrameConvexPolygon2D(worldFrame, convexPolygon2d);
   }

   @AfterEach
   public void tearDown()
   {
      ReferenceFrameTools.clearWorldFrameTree();
   }

	@Test // timeout = 30000
   public void thereIsNoTestHere()
   {
      
   }
   
   private ConvexPolygon2D createSomeValidPolygon()
   {
      double[][] polygonPoints = new double[][]
            {
               {-0.05107802536335158, 0.04155594197133163}, {-0.05052044462374434, 0.1431544119584275}, {0.12219695435431863, 0.14220652470109518},
               {0.12219695435431865, -0.041946248489056696}, {0.12163937361471142, -0.1435447184761526}, {-0.05107802536335154, -0.14259683121882027}
            };

      ConvexPolygon2D polygon = new ConvexPolygon2D(Vertex2DSupplier.asVertex2DSupplier(polygonPoints));
      return polygon;
   }

}
