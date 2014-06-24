package robomine

import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.callbacks.RayCastCallback
import org.jbox2d.dynamics.Fixture
import scala.util.control.NonFatal
import Util._
import java.util.Locale
import org.jbox2d.callbacks.QueryCallback
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import org.jbox2d.dynamics.joints.JointDef
import org.jbox2d.dynamics.joints.JointType
import org.jbox2d.dynamics.joints.RopeJointDef
import org.jbox2d.dynamics.joints.Joint
import scala.collection.mutable.Buffer

private [robomine] class RobotInfo(base: Body)(implicit names: NameGenerator) {
  @volatile var charging = false
  @volatile var batteryLevel = 1.0f
  @volatile var leftPower = 0.0f
  @volatile var rightPower = 0.0f
  @volatile var laserPowerL = 0.0f
  @volatile var laserPowerC = 0.0f
  @volatile var laserPowerR = 0.0f
  @volatile var laserPointL = Option.empty[Vec2]
  @volatile var laserPointC = Option.empty[Vec2]
  @volatile var laserPointR = Option.empty[Vec2]
  @volatile var load = 0f
  @volatile var _prevAcc = new Vec2(0,0)  
  @volatile var _laserL = 0f
  @volatile var _laserC = 0f
  @volatile var _laserR = 0f
  @volatile var _gold = 0f      
  @volatile var doLoad = false
  @volatile var doUnload = false
  @volatile var ropes = Buffer.empty[Joint]
  
  val controls = new RobotControls with RobotControlsInternal {
    import collection.mutable.{Map,Set}
    
    val name = names.nextName()
        
    val reliability = Random.nextInt(75000)
    var screwdriver = if (Random.nextBoolean) Some(0.0) else None    
    
    private var sensors = Map[String, (Double,Body => String)](
      "gps" -> ( 6.0,  b =>  "%2.4fN %2.4fW".formatLocal(Locale.US, b.getPosition().y, -b.getPosition().x)),
      "batteryLevel" -> ( 0.1, _ => "%.1f%%".formatLocal(Locale.US, batteryLevel * 100) ),
      "compass" -> ( 2.0, b => "%.1f°".formatLocal(Locale.US, b.getAngle() * 57.295) ),
      "gyroscope" -> ( 0.5, { b =>
        "%.4f".formatLocal(Locale.US, b.getAngularVelocity())
      }),
      "accelerometer" -> ( 0.5, b => {
        val acc = b.getLinearVelocity().sub(_prevAcc).rotate(-b.getAngle())
        "%.4f %.4f".formatLocal(Locale.US, acc.x, acc.y)           
      }),
      "laser" -> (( 1.0, _ => {
        val raw = "%.4f %.4f %.4f".formatLocal(Locale.US, _laserL, _laserC, _laserR)
        screwdriver.filter(_ > 0).fold(raw){ p => if (p > Random.nextDouble) Random.shuffle((raw + "XX").toList).mkString else raw }        
      })),
      "load" -> (( 0.1, _ => "%.4f".formatLocal(Locale.US,load))),
      "goldDetector" -> ((2.0, _ => {
        val raw = "%.4f".formatLocal(Locale.US, _gold)
        screwdriver.map(_ * -1).filter(_ > 0).fold(raw){ p => if (p > Random.nextDouble) Random.shuffle((raw ++ "XX").toList).mkString else raw }
      }))
    )
    
    def cut(f: Float, negative: Boolean = false) = 
      if (negative) Math.min(1f,Math.max(-1f,f))
      else Math.min(1f,Math.max(0f,f))
    
    private var outputs = Map[String, Float => Unit](
      "leftWheel" -> ( value => leftPower = cut(value, negative = true)),
      "rightWheel" -> ( value => rightPower = cut(value, negative = true)),
      "laserL" -> (value => laserPowerL = cut(value)),
      "laserC" -> (value => laserPowerC = cut(value)),
      "laserR" -> (value => laserPowerR = cut(value)))
    
    private var listeners = Map.empty[String,Set[String => Unit]] 
    				  
	  def availableSensors: Traversable[String] = sensors.keys
	  				  
	  def addEventListener(sensorId: String, handler: String => Unit) = {
      if (!sensors.contains(sensorId))
        throw new NoSuchElementException("sensor " + sensorId + " is unavailable")
      listeners.get(sensorId) match {
        case None => listeners(sensorId) = Set(handler)
        case Some(x) => x += handler
      }
    }
	  
	  def removeEventListener(sensorId: String, handler: String => Unit) = {
	    listeners.get(sensorId) match {
	      case Some(x) => x -= handler
	      case None => //
	    }
	  }
	  
	  def availableOutputs: Traversable[String] = outputs.keys
	  
	  def setPowerLevel(outputId: String, level: Double) = { 
	    outputs.get(outputId) match {
	      case Some(x) => x(level.toFloat)
	      case None => throw new NoSuchElementException("output " + outputId + " is unavailable")
	    }
	  }
	  
	  
	  def loadGold() {
	    assert(!doUnload, "can't load and unload at the same time!")
	    assert(batteryLevel >= 0.25, "not enough battery")	    
	    doLoad = true
	  }
	  
	  def unloadGold() {
	    assert(!doLoad, "can't load and unload at the same time!")
	    assert(batteryLevel >= 0.05, "not enough battery")	    
	    doUnload = true
	  }
	  
	  def step(body: Body) = {
	    if (Random.nextInt(100) == 1)
	      screwdriver = screwdriver.map( _ + Random.nextGaussian * 0.001 )
	    if (Random.nextInt(reliability) == 1)
	      screwdriver = screwdriver.map( x => x * 10 )
	    listeners.foreach { case (name,listeners) =>
	      if (batteryLevel > 0 && listeners.size > 0) {	        
		      sensors.get(name) match {
		        case None =>
		          listeners.foreach { listener => Future ( listener("0.0") ) }
		          batteryLevel -= 0.00003f
		        case Some((cost,sensor)) =>
		          val output = sensor(body)
		          listeners.foreach { listener => Future ( listener(output) ) }
		          batteryLevel -= 0.00003f * cost
		      }
	      }
	    }
	    _prevAcc = body.getLinearVelocity().clone()	    
	  }
  }
  
  def goldCallback(robot: Body, pos: Vec2, orientation: Float) = new QueryCallback() {
    def reportFixture(fixture: Fixture): Boolean = {
      fixture.getBody().getUserData() match {
        case g: GoldInfo =>
          val relative = fixture.getBody().getPosition().sub(pos).rotate(-orientation)          
          val factor = Math.max(5 - relative.length(),0) / 5          
          _gold += g.amount * factor
          if (doLoad && relative.y > -1.5 && relative.y < 1.5 && relative.x > -1 && relative.x < 3.5) {
            val joint = new RopeJointDef
            joint.bodyA = robot
            joint.localAnchorA.set(-1,0)
            joint.bodyB = fixture.getBody()
            joint.maxLength = 4
            joint.`type` = JointType.ROPE
            ropes += robot.getWorld().createJoint(joint)
            load += fixture.getBody().getMass()
          }
        case _ =>
      }
      true
    }
  }
  
  val laserCallbackL = new RayCastCallback() {
    def reportFixture(fixture: Fixture, point: Vec2, point2: Vec2, float: Float): Float = {
      if (fixture.getBody() != base) {
        laserPointL = Some(point)
        _laserL = float
      }
      float
    }
  }
  val laserCallbackC = new RayCastCallback() {
    def reportFixture(fixture: Fixture, point: Vec2, point2: Vec2, float: Float): Float = {
      if (fixture.getBody() != base) {
        laserPointC = Some(point)
        _laserC = float
      }	          
      float
    }
  }
  val laserCallbackR = new RayCastCallback() {
    def reportFixture(fixture: Fixture, point: Vec2, point2: Vec2, float: Float): Float = {
      if (fixture.getBody() != base) {
        laserPointR = Some(point)
        _laserR = float
      }
      float
    }
  }
}