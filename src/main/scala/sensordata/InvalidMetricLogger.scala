package sensordata

import cloudflow.akkastream._
import cloudflow.akkastream.scaladsl._
import cloudflow.streamlets._
import cloudflow.streamlets.proto._
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes

class InvalidMetricLogger extends AkkaStreamlet {
  val inlet = ProtoInlet[InvalidMetric]("in")
  val shape = StreamletShape.withInlets(inlet)

  override def createLogic = new RunnableGraphStreamletLogic() {
    val flow = FlowWithCommittableContext[InvalidMetric]
      .map { invalidMetric ⇒
        system.log.warning(s"Invalid metric detected!!! $invalidMetric")
        invalidMetric
      }
      .withAttributes(CinnamonAttributes.instrumented(name = "InvalidMetricLogger"))

    def runnableGraph =
      sourceWithCommittableContext(inlet)
        .via(flow)
        .to(committableSink)
  }
}
