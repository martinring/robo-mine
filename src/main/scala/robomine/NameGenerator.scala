package robomine

import scala.util.Random
import scala.collection.mutable.Buffer

private [robomine] trait NameGenerator {
  def nextName(): String
}

private [robomine] object NameGenerator {
  private val prefixes = List("intelligent","perceptive","analytic",
                              "smart","autonomous","cognitive",
                              "heuristic","biological","artificial",
                              "virtual","algorithmic","sovereign",
                              "universal","industrial","civil","spacial",
                              "formal","cyber-physical","nuclear",
                              "functional","reactive","resilient")
  private val infixes  = List("task","path","life","gold","money",
                              "vacuum","cave","wealth","work","duty",
                              "load","assignment","obstacle","serial",
                              "rock","ground","cash","gem", "jewel",
                              "fortune","capital","fund","finance",
                              "property","gem","target","object")
  private val suffixes = List("terminator","cleaner","finder","anihilator",
                              "destoyer","detector","vaporizer","maximizer",
                              "minimizer","executor","bomber","eradicator",
                              "vandal","slaughterer","examinator","acquirer",
                              "appropriator","discoverer","spotter","killer",
                              "harvester","processor","agent","defender",
                              "patron","proponent","hunter","claimant",
                              "discoverer","booster","expander","widener",
                              "augmenter","transformer")
  
  private val robotNames = Buffer.empty[String]
    
  for {
    prefix <- prefixes
    infix <- infixes
    suffix <- suffixes
  } robotNames += s"$prefix-$infix-$suffix"
     
  def apply() = {
    val localNames = Buffer(robotNames)    
    new NameGenerator {
      def nextName() = {
        if (localNames.isEmpty)
          sys.error("no more names available")
		    var i = Random.nextInt(robotNames.length)
		    val res = robotNames.apply(i)
		    robotNames.remove(i)
		    res        
      }
    }
  }
}