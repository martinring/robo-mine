package robomine

import org.jbox2d.common.Vec2

object Util {
  implicit def loosePrecision(double: Double) = double.toFloat      
  implicit def vec2FromTuple1(tup: (Double,Double)) = new Vec2(tup._1.toFloat, tup._2.toFloat)
  implicit def vec2FromTuple2(tup: (Int,Int)) = new Vec2(tup._1.toFloat, tup._2.toFloat)
  implicit def vec2FromTuple3(tup: (Double,Int)) = new Vec2(tup._1.toFloat, tup._2.toFloat)
  implicit def vec2FromTuple4(tup: (Int,Double)) = new Vec2(tup._1.toFloat, tup._2.toFloat)
  implicit class RotatableVec2(val vec: Vec2) extends AnyVal {
    def rotate(beta: Float) = new Vec2(
      vec.x * Math.cos(beta) - vec.y * Math.sin(beta),
      vec.x * Math.sin(beta) + vec.y * Math.cos(beta))
  }
}