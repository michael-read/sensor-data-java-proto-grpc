package sensordata;

import akka.NotUsed;
import akka.kafka.ConsumerMessage;
import akka.stream.javadsl.FlowWithContext;
import akka.stream.javadsl.RunnableGraph;
import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.streamlets.RoundRobinPartitioner;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.proto.javadsl.ProtoInlet;
import cloudflow.streamlets.proto.javadsl.ProtoOutlet;
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes;
import scala.Option;
import sensordata.MetricProto.Metric;

import java.util.Arrays;
import java.util.Optional;

// tag::toMetrics[]
public class SensorDataToMetrics extends AkkaStreamlet {

    private final ProtoInlet<SensorData> inlet = ProtoInlet.create(
            "in",
            SensorData.class,
            true,
            (inBytes, throwable) -> {
                context().system().log().error(String.format("an exception occurred on inlet: %s -> (hex string) %h", throwable.getMessage(), Arrays.toString(inBytes)));
                return Optional.empty(); // skip the element
            });

    public final ProtoOutlet<Metric> outlet =
            new ProtoOutlet<>("out", RoundRobinPartitioner.getInstance(), Metric.class);

    @Override
    public StreamletShape shape() {
        return StreamletShape.createWithInlets(inlet).withOutlets(outlet);
    }

    @Override
    public AkkaStreamletLogic createLogic() {
        return new RunnableGraphStreamletLogic(getContext()) {

            FlowWithContext<SensorData, ConsumerMessage.Committable, Metric, ConsumerMessage.Committable, NotUsed> createFlow() {
                return FlowWithContext.<SensorData, ConsumerMessage.Committable>create()
                    .mapConcat(data -> {
                        return Arrays.asList(
                               Metric.newBuilder()
                                        .setDeviceId(data.getDeviceId())
                                        .setTimestamp(data.getTimestamp())
                                        .setName("power")
                                        .setValue(data.getMeasurements().getPower()).build(),
                                Metric.newBuilder()
                                        .setDeviceId(data.getDeviceId())
                                        .setTimestamp(data.getTimestamp())
                                        .setName("rotorSpeed")
                                        .setValue(data.getMeasurements().getRotorSpeed()).build(),
                                Metric.newBuilder()
                                        .setDeviceId(data.getDeviceId())
                                        .setTimestamp(data.getTimestamp())
                                        .setName("windSpeed")
                                        .setValue(data.getMeasurements().getWindSpeed()).build()
                        );
                    })
                    /*
                    Note: if you don't currently have a Lightbend subscription you can optionally comment
                    out the following line referencing CinnamonAttributes and associated import above.
                     */
                    .withAttributes(CinnamonAttributes.instrumentedByName("SensorDataToMetrics"));
            }

            @Override
            public RunnableGraph<?> createRunnableGraph() {
                return getSourceWithCommittableContext(inlet)
                        .via(createFlow())
                        .to(getCommittableSink(outlet));
            }
        };
    }

}
// end::toMetrics[]