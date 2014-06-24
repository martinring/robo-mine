package robomine

import org.jbox2d.dynamics.Body

trait RobotControls {
  /**
   * The unique name of this robot
   */
  val name: String
  
  /**
   * Lists the available sensors for this robot.
   */
  def availableSensors: Traversable[String]
  
  /*
   * Add an event listener that gets fed with sensor data
   * of a specific sensor port.
   */
  def addEventListener(sensorId: String, handler: String => Unit)
  
  /*
   * Remove event listener from a specific sensor port. The sensor
   * will consume power as long as any event listener is connected.
   */
  def removeEventListener(sensorId: String, handler: String => Unit)
  
  /**
   * Lists the available outputs for this robot.
   */
  def availableOutputs: Traversable[String]
  
  /*
   * Set the power level of an output to a value between 0 and 1.
   */
  def setPowerLevel(outputId: String, level: Double)
  
  /**
   * Loads all Gold nuggets that are directly in front of the robot
   * loading consumes 25% of the battery.
   */
  def loadGold()
  
  /**
   * Drops the loaded gold directly in front of the robot. 
   * Consumes 5% of the battery.
   */
  def unloadGold()
}

private [robomine] trait RobotControlsInternal {
  def step(state: Body)
}