package sensordata;

import akka.NotUsed;
import akka.kafka.ConsumerMessage;
import akka.stream.javadsl.FlowWithContext;
import akka.stream.javadsl.RunnableGraph;

import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.proto.javadsl.ProtoInlet;
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes;

import sensordata.InvalidProto.InvalidMetric;

public class InvalidMetricLogger extends AkkaStreamlet {

    private final ProtoInlet<InvalidMetric> inlet = new ProtoInlet<>(
            "in",
            InvalidMetric.class,
            true,
            (inBytes, throwable) -> {
                context().system().log().error(String.format("an exception occurred on inlet: %s -> (hex) %h", throwable.getMessage(), inBytes));
                return null; // skip the element
            });

    @Override
    public StreamletShape shape() {
        return StreamletShape.createWithInlets(inlet);
    }

    @Override
    public AkkaStreamletLogic createLogic() {
        return new RunnableGraphStreamletLogic(getContext()) {

            FlowWithContext<InvalidMetric, ConsumerMessage.Committable, InvalidMetric, ConsumerMessage.Committable, NotUsed> createFlow() {
                return FlowWithContext.<InvalidMetric, ConsumerMessage.Committable>create()
                        .map(invalidMetric -> {
                            system().log().warning(String.format("%s = %s", "Invalid metric detected!!!", invalidMetric.toString()));
                            return invalidMetric;
                        })
                        /*
                        Note: if you don't currently have a Lightbend subscription you can optionally comment
                        out the following line referencing CinnamonAttributes and associated import above.
                         */
                        .withAttributes(CinnamonAttributes.instrumentedByName("InvalidMetricLogger"));
            }

            @Override
            public RunnableGraph<?> createRunnableGraph() {
                return getSourceWithCommittableContext(inlet)
                        .via(createFlow())
                        .to(getCommittableSink());
            }
        };
    }

}
