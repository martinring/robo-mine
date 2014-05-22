package robomine

import org.jbox2d.testbed.framework.TestbedSettings
import org.jbox2d.dynamics._
import scala.collection.mutable.Buffer
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import scala.util.Random
import org.jbox2d.testbed.framework.TestbedTest
import Util._

private [robomine] trait SimulationInit { self: TestbedTest =>

  implicit val names = NameGenerator()
  val _robots = Buffer.empty[RobotControls]
  val initCode = Buffer.empty[Traversable[RobotControls] => Unit]
  val goldListeners = Buffer.empty[Double => Unit]
  var initialized = false
  var goldInBase = 0f
	      
  var robots = Buffer.empty[Body]
  var base: Body = null   
  
  override def initTest(argDeserialized: Boolean) = {
    robots.clear()
    setTitle("")
    
    getWorld().setGravity(new Vec2())
    
    val wallShapes = List(
      List((-95,95),(-95,-95),(-90,-90),(-90,90)),
      List((-95,-95),(95,-95),(90,-90),(-90,-90)),
      List((95,-95),(95,95),(90,90),(90,-90)),
      List((95,95),(-95,95),(-90,90),(90,90))             
    )
            
    for (shape <- wallShapes) {
      val wall = new BodyDef()
      wall.`type` = BodyType.STATIC
      val wallBody = getWorld().createBody(wall)
      val theShape = new PolygonShape()
      theShape.set(shape.map(x => new Vec2(x._1,x._2)).toArray, shape.length)
      wallBody.createFixture(theShape, 10000f)
              .setRestitution(0.5f)                  
    }
                   
    val base = new BodyDef()
    base.`type` = BodyType.STATIC      
    val baseBody = getWorld().createBody(base)        
    val baseShape = new PolygonShape()
    baseShape.set(Array[Vec2]((-80,80),(-80,50),(-50,50),(-50,80)),4)        
    val baseFixture = new FixtureDef()
    baseFixture.shape = baseShape
    baseFixture.isSensor = true
    baseBody.createFixture(baseFixture)
    this.base = baseBody
    
    for (i <- 0 to 24) {
      val polygonShape = new PolygonShape()
      //val ps = Array[Vec2]((-1,1),(-1,-0.3),(0,-1),(1,-0.3),(1,1),(0.7,1),(0.7,0.7),(-0.7,0.7))
      //polygonShape.set(ps,ps.length)
      polygonShape.setAsBox(1, 1)
      
      val bodyDef = new BodyDef()
      bodyDef.linearDamping = 2.5f
      bodyDef.angularDamping = 3          
      bodyDef.angle = Random.nextFloat() * 2 * Math.PI.toFloat
      bodyDef.`type` = BodyType.DYNAMIC
      bodyDef.position.set(-75 + 5 * (i % 5), 75 - 5 * (i / 5))
      val userData = new RobotInfo(baseBody)
      bodyDef.userData = userData
                
      val body = getWorld().createBody(bodyDef)
      body.createFixture(polygonShape, 2.5f)
      
      robots += body
      _robots += userData.controls
    }
    
    for (i <- 0 to 100) {
      val polygonShape = new PolygonShape()
      val n = 12
      val u = 2 * Math.PI / n
      val ps = Array.tabulate(n){ i =>
        val scale = Math.max(0.1f, Random.nextGaussian().toFloat)
        new Vec2(Math.sin(u * i) * scale, Math.cos(u * i) * scale) 
      }          
      polygonShape.set(ps, ps.length / 2)

      
      val bodyDef = new BodyDef()
      bodyDef.linearDamping = 2.5f
      bodyDef.angularDamping = 1f
      bodyDef.`type` = BodyType.DYNAMIC
      bodyDef.position.set(Random.nextGaussian.toFloat * 30, Random.nextGaussian.toFloat * 30)
                
      val body = getWorld().createBody(bodyDef)      
      body.createFixture(polygonShape, 5f)
      body.setUserData(new GoldInfo(body.getMass()))
    }
  }

}