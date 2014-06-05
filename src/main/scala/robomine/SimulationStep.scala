package robomine

import org.jbox2d.dynamics.Body
import Util._
import org.jbox2d.dynamics.contacts.Contact
import scala.util.control.NonFatal
import org.jbox2d.testbed.framework.TestbedTest
import org.jbox2d.testbed.framework.TestbedSettings
import org.jbox2d.common.Vec2
import org.jbox2d.common.Color3f
import scala.util.Random
import org.jbox2d.collision.AABB

private [robomine] trait SimulationStep extends TestbedTest with SimulationInit {
  override def step(settings: TestbedSettings) {
    if (!initialized) {
      initialized = true
      initCode.foreach{ f => 
        try f(_robots) catch {
          case NonFatal(e) => e.printStackTrace()
        }
      }
    }
    super.step(settings)                
    val model = getModel()
    var i = 0
    robots.foreach { robot =>
      
      i += 1
      val info = robot.getUserData().asInstanceOf[RobotInfo]          
      
      info.controls.step(robot)
            
      info._gold = 0f
      getWorld().queryAABB(info.goldCallback(robot.getPosition()), new AABB(robot.getPosition().sub(new Vec2(5,5)), robot.getPosition().add(new Vec2(5,5))))
      
      model.getDebugDraw().drawPoint(robot.getWorldPoint(new Vec2(1f,0)), 2f, new Color3f(0,1,0))
      model.getDebugDraw().drawSolidCircle(robot.getPosition(), Math.sqrt(info.batteryLevel), new Vec2(), new Color3f(0,1,0))
      model.getDebugDraw().drawString(model.getDebugDraw().getWorldToScreen(robot.getPosition().x, robot.getPosition().y + 1.5f).addLocal(-5,0), i.toString, new Color3f(1,1,1))
                
      val leftMotorPos = robot.getWorldPoint(new Vec2(0,0.8f))
      val rigthMotorPos = robot.getWorldPoint(new Vec2(0,-0.8f))
      val leftPower = robot.getWorldVector(new Vec2(200f * info.leftPower,0))
      val rightPower = robot.getWorldVector(new Vec2(200f * info.rightPower,0))
                         
      if (info.batteryLevel > 0f) {
        info.laserPointC = None
        info.laserPointL = None
        info.laserPointR = None		        
        
        info._laserL = 1f
        info._laserC = 1f
        info._laserR = 1f
        
        if (info.laserPowerL > 0)
          getWorld().raycast(info.laserCallbackL, robot.getPosition(), robot.getWorldPoint(new Vec2(info.laserPowerL * 29.5f,info.laserPowerL * 5.21f)))
        if (info.laserPowerC > 0) {
          getWorld().raycast(info.laserCallbackC, robot.getPosition(), robot.getWorldPoint(new Vec2(info.laserPowerC * 30f,0)))
        }
        if (info.laserPowerR > 0)
          getWorld().raycast(info.laserCallbackR, robot.getPosition(), robot.getWorldPoint(new Vec2(info.laserPowerR * 29.5f,info.laserPowerR * -5.21f)))
        
        info.laserPointL match {
          case Some(point) =>          
            model.getDebugDraw().drawSegment(robot.getPosition(), point, new Color3f(1,0,0))
            model.getDebugDraw().drawPoint(point, 3f, new Color3f(1,0,0))              
          case None =>
            model.getDebugDraw().drawSegment(robot.getPosition(), robot.getWorldPoint(new Vec2(info.laserPowerL * 29.5f,info.laserPowerL * 5.21f)), new Color3f(0.7,0,0))
        }
        info.laserPointC match {
          case Some(point) =>          
            model.getDebugDraw().drawSegment(robot.getPosition(), point, new Color3f(1,0,0))
            model.getDebugDraw().drawPoint(point, 3f, new Color3f(1,0,0))              
          case None =>
            model.getDebugDraw().drawSegment(robot.getPosition(), robot.getWorldPoint(new Vec2(info.laserPowerC * 30f,0)), new Color3f(0.7,0,0))
        } 
        info.laserPointR match {
          case Some(point) =>          
            model.getDebugDraw().drawSegment(robot.getPosition(), point, new Color3f(1,0,0))
            model.getDebugDraw().drawPoint(point, 3f, new Color3f(1,0,0))              
          case None =>
            model.getDebugDraw().drawSegment(robot.getPosition(), robot.getWorldPoint(new Vec2(info.laserPowerR * 29.5f, info.laserPowerR * -5.21f)), new Color3f(0.7,0,0))
        }            
        
        info.batteryLevel -= info.laserPowerL * 0.00005f
        info.batteryLevel -= info.laserPowerC * 0.00005f
        info.batteryLevel -= info.laserPowerR * 0.00005f		        
        info.batteryLevel -= Math.abs(info.leftPower) * 0.0001f
        info.batteryLevel -= Math.abs(info.rightPower) * 0.0001f            
        robot.applyForce(leftPower,leftMotorPos)
        robot.applyForce(rightPower,rigthMotorPos)
      }
      
      if (info.charging && info.batteryLevel < 1.0f)
        info.batteryLevel += 0.005f
        
      info.batteryLevel = Math.max(0, Math.min(info.batteryLevel, 1))
        
      /*if (robot.isAwake())
        robot.applyForce(
          robot.getWorldVector(new Vec2(Random.nextGaussian.toFloat * robot.getLinearVelocity().length() * robot.getLinearVelocity().length() * 0.5, Random.nextGaussian.toFloat * robot.getLinearVelocity().length() * robot.getLinearVelocity().length() * 0.5)),
          robot.getWorldPoint(new Vec2(Random.nextGaussian.toFloat * 0.5, Random.nextGaussian.toFloat * 0.5))
        )*/          
    }        
  }
  
  def bodyInBase(body: Body) = {
    body.getUserData() match {
      case robot: RobotInfo =>
        robot.charging = true
      case gold: GoldInfo =>
        goldInBase += gold.amount
        goldListeners.foreach { f =>
          try {
            f(goldInBase)
          } catch {
            case NonFatal(e) => e.printStackTrace()
          }
        }
      case _ =>          
    }
  }
  
  def bodyOffBase(body: Body) = {
    body.getUserData() match {
      case robot: RobotInfo =>
        robot.charging = false
      case gold: GoldInfo =>
        goldInBase -= gold.amount
        goldListeners.foreach { f =>
          try {
            f(goldInBase)
          } catch {
            case NonFatal(e) => e.printStackTrace()
          }
        }
      case _ =>
    }
  }
  
  override def beginContact(contact: Contact) {
    if (contact.getFixtureA().getBody() == base)
      bodyInBase(contact.getFixtureB().getBody())        
    else if (contact.getFixtureB().getBody() == base)    
      bodyInBase(contact.getFixtureA().getBody())
  }
  
  override def endContact(contact: Contact) {
    if (contact.getFixtureA().getBody() == base)
      bodyOffBase(contact.getFixtureB().getBody())                  
    else if (contact.getFixtureB().getBody() == base)
      bodyOffBase(contact.getFixtureA().getBody())
  }  
}