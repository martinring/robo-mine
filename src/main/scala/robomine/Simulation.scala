package robomine

import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import org.jbox2d.common.Vec2
import org.jbox2d.testbed.framework.TestList
import org.jbox2d.testbed.framework.TestbedController
import org.jbox2d.testbed.framework.TestbedModel
import org.jbox2d.testbed.framework.TestbedSettings
import org.jbox2d.testbed.framework.TestbedTest
import org.jbox2d.testbed.framework.j2d.TestPanelJ2D
import javax.swing.JFrame
import java.awt.BorderLayout
import javax.swing.WindowConstants

trait Simulation {
  /**
   * can be passed a function which will be executed when the
   * simulation is available. The function will be supplied with
   * a list of the available robots.
   */
  def onReady(f: Traversable[RobotControls] => Unit)
  
  /**
   * used to register a callback to be executed when the amount of
   * collected gold has changed
   */
  def onGoldCollected(f: Double => Unit)
    
  /**
   * stops the simulation
   */
  def stop()
}

object Simulation {     
  /**
   * Creates a new simulation. 
   */
  def start(teams: Int = 1): Simulation = {
    println("[simulation] starting")
    require(teams == 1, "currently only one team is supported")
    
    object World extends TestbedTest with SimulationInit with SimulationStep {      
      override def getTestName() = "robo-mine"
      override def getDefaultCameraPos() = new Vec2(0,0)
      override def getDefaultCameraScale() = 3.2f            
    }
    
    
       
    val model = new TestbedModel()
    model.clearTestList()
    model.addCategory("robo-mine")
    model.addTest(World)
              
    val panel = new TestPanelJ2D(model)    
    TestList.populateModel(model)
    model.setDebugDraw(panel.getDebugDraw())    
           
    model.getSettings().getSetting(TestbedSettings.DrawHelp).enabled = false
    
    val controller = new TestbedController(model,panel,TestbedController.UpdateBehavior.UPDATE_CALLED)    
        
    val frame = new JFrame("robo-mine v1.0") {
      setLayout(new BorderLayout());
      
      add(panel, "Center")
      pack()
      
      controller.playTest(0)
      controller.start()       
    }
    
    frame.setVisible(true)
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    frame.addWindowListener(new WindowAdapter() {
      override def windowClosed(event: WindowEvent) {
        println("[simulation] shutting down")
        controller.stop()
      }
    })
    
    new Simulation {
      def stop() = {
        controller.stop()
        if (frame != null && frame.isVisible())
          frame.dispose()
      }
      
      def collectedGold = World.goldInBase
      
      def onGoldCollected(f: Double => Unit) = {
        World.goldListeners.append(f)
        if (World.initialized)
          f(World.goldInBase)
      }
      
      def onReady(f: Traversable[RobotControls] => Unit) = {
        if (World.initialized) {
          f(World._robots)
        } else {
          World.initCode.append(f)
        }
      }
    }
  }
}