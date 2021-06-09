package sensordata;

import akka.NotUsed;
import akka.kafka.ConsumerMessage.Committable;
import akka.stream.javadsl.FlowWithContext;
import akka.stream.javadsl.RunnableGraph;

import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.akkastream.javadsl.util.Either;
import cloudflow.akkastream.util.javadsl.Splitter;
import cloudflow.streamlets.RoundRobinPartitioner;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.proto.javadsl.ProtoInlet;
import cloudflow.streamlets.proto.javadsl.ProtoOutlet;

import com.lightbend.cinnamon.akka.stream.CinnamonAttributes;
import sensordata.MetricProto.Metric;
import sensordata.InvalidProto.InvalidMetric;

import java.util.UUID;

public class MetricsValidation extends AkkaStreamlet {
    private final ProtoInlet<Metric> inlet = new ProtoInlet<Metric>(
            "in",
            Metric.class,
            true,
            (inBytes, throwable) -> null
    );

    public final ProtoOutlet<InvalidMetric> invalid =
            new ProtoOutlet<>("invalid",
                    (invalidMetric) -> {
                        if (invalidMetric.hasMetric()) {
                            return invalidMetric.getMetric().getDeviceId();
                        }
                        else {
                            // this shouldn't happen, but just in case create a UUID
                            return UUID.randomUUID().toString();
                        }
                    },
                    InvalidMetric.class);

    public final ProtoOutlet<Metric> valid =
            new ProtoOutlet<>("valid", RoundRobinPartitioner.getInstance(), Metric.class);

    @Override
    public StreamletShape shape() {
        return StreamletShape.createWithInlets(inlet).withOutlets(invalid, valid);
    }

    @Override
    public AkkaStreamletLogic createLogic() {
        return new RunnableGraphStreamletLogic(context()) {

            FlowWithContext<Metric, Committable, Either<InvalidMetric, Metric>, Committable, NotUsed> createFlow() {
                return FlowWithContext.<Metric, Committable>create()
                        .map(metric -> {
                            if (metric.getValue() < 0) {
//                                system().log().warning(String.format("%s %s = %f All metrics must be positive numbers", metric.getDeviceId(), metric.getName(), metric.getValue()));
                                return Either.left(InvalidMetric.newBuilder()
                                        .setMetric(metric)
                                        .setError("All metrics must be positive numbers!")
                                        .build()
                                );
                            }
                            else {
                                return Either.right(metric);
                            }
                        });
//                        .withAttributes(CinnamonAttributes.instrumented("MetricsValidation"));
            }

            public RunnableGraph createRunnableGraph() {
                return getSourceWithCommittableContext(inlet).to(Splitter.sink(createFlow(), invalid, valid, context()));
            }
        };
    }

}
