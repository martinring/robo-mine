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

private [robomine] class RobotInfo(base: Body)(implicit names: NameGenerator) {
  var charging = false
  var batteryLevel = 1.0f
  var leftPower = 0.0f
  var rightPower = 0.0f
  var laserPowerL = 0.0f
  var laserPowerC = 0.0f
  var laserPowerR = 0.0f
  var laserPointL = Option.empty[Vec2]
  var laserPointC = Option.empty[Vec2]
  var laserPointR = Option.empty[Vec2]
  var _prevAcc = new Vec2(0,0)
  var _laserL = 0f
  var _laserC = 0f
  var _laserR = 0f
  var _gold = 0f
  
  val controls = new RobotControls with RobotControlsInternal {
    import collection.mutable.{Map,Set}
    
    val name = names.nextName()
    
    private var sensors = Map[String, Body => String](
      "gps" -> ( b =>  "%2.4fN %2.4fW".formatLocal(Locale.US, b.getPosition().y, -b.getPosition().x)),
      "batteryLevel" -> ( _ => "%.1f%%".formatLocal(Locale.US, batteryLevel * 100) ),
      "gyroscope" -> { b =>        
        "%.4f".formatLocal(Locale.US, b.getAngularVelocity())
      },      
      "accelerometer" -> ( b => {
        val acc = b.getLinearVelocity().sub(_prevAcc).rotate(-b.getAngle())
        "%.4f %.4f".formatLocal(Locale.US, acc.x, acc.y)           
      }),
      "laser" -> ( b => "%.4f %.4f %.4f".formatLocal(Locale.US, _laserL, _laserC, _laserR) ),
      "goldDetector" -> ( _ => "%.4f".formatLocal(Locale.US, _gold) )
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
	  
	  def step(body: Body) = {
	    listeners.foreach { case (name,listeners) =>
	      if (batteryLevel > 0 && listeners.size > 0) {
	        batteryLevel -= 0.00005f				         
		      sensors.get(name) match {
		        case None =>
		          listeners.foreach { listener => Future { listener("0.0") } }
		        case Some(sensor) =>
		          listeners.foreach { listener => Future { listener(sensor(body)) } }
		      }
	      }
	    }
	    _prevAcc = body.getLinearVelocity().clone()	    
	  }
  }
  
  def goldCallback(pos: Vec2) = new QueryCallback() {
    def reportFixture(fixture: Fixture): Boolean = {
      fixture.getBody().getUserData() match {
        case g: GoldInfo =>
          val factor = Math.max(5 - fixture.getBody().getPosition().sub(pos).length(),0) / 5
          _gold += g.amount * factor
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