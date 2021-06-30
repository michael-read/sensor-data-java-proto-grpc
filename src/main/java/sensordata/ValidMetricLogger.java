package sensordata;

import akka.NotUsed;
import akka.kafka.ConsumerMessage;
import akka.stream.javadsl.FlowWithContext;
import akka.stream.javadsl.RunnableGraph;

import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.streamlets.ConfigParameter;
import cloudflow.streamlets.RegExpConfigParameter;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.StringConfigParameter;
import cloudflow.streamlets.proto.javadsl.ProtoInlet;
import com.lightbend.cinnamon.akka.stream.CinnamonAttributes;

import scala.Option;
import scala.collection.immutable.IndexedSeq;
import scala.collection.immutable.VectorBuilder;

import sensordata.MetricProto.Metric;

public class ValidMetricLogger extends AkkaStreamlet {

    private final ProtoInlet<Metric> inlet = (ProtoInlet<Metric>) ProtoInlet.create("in", Metric.class)
            .withErrorHandler((inBytes, throwable) -> {
                        context().system().log().error(String.format("an exception occurred on inlet: %s -> (hex string) %h", throwable.getMessage(), inBytes));
                        return Option.apply(null); // skip the element
                    }
            );

/*    private final ProtoInlet<Metric> inlet = new ProtoInlet<>(
            "in",
            Metric.class,
            true,
            (inBytes, throwable) -> {
                context().system().log().error(String.format("an exception occurred on inlet: %s -> (hex string) %h", throwable.getMessage(), inBytes));
                return null; // skip the element
            });*/

    private final RegExpConfigParameter logLevel = new RegExpConfigParameter(
            "log-level",
            "Provide one of the following log levels, debug, info, warning or error",
            "^debug|info|warning|error$",
            Option.apply("info")
    );

    private final StringConfigParameter msgPrefix = new StringConfigParameter(
            "msg-prefix",
            "Provide a prefix for the log lines",
            Option.apply("valid-logger")
    );

    @Override
    public IndexedSeq<ConfigParameter> configParameters() {
        VectorBuilder<ConfigParameter> vb = new VectorBuilder<>();
        vb.$plus$eq(logLevel);
        vb.$plus$eq(msgPrefix);
        return vb.result();
    }

    @Override
    public StreamletShape shape() {
        return StreamletShape.createWithInlets(inlet);
    }

    private String mPrefix() { return msgPrefix.getValue(context()); }

    private void logF(String msg) {
        if (logLevel.getValue(context()).equalsIgnoreCase("debug")) {
            context().system().log().debug(msg);
        }
        else if (logLevel.getValue(context()).equalsIgnoreCase("info")) {
            context().system().log().info(msg);
        }
        else if (logLevel.getValue(context()).equalsIgnoreCase("warning")) {
            context().system().log().warning(msg);
        }
        else if (logLevel.getValue(context()).equalsIgnoreCase("error")) {
            context().system().log().error(msg);
        }
    }

    private void log(Metric metric) {
        logF(String.format("%s = %s", mPrefix(), metric.toString()));
    }


    @Override
    public AkkaStreamletLogic createLogic() {
        return new RunnableGraphStreamletLogic(getContext()) {

            FlowWithContext<Metric, ConsumerMessage.Committable, Metric, ConsumerMessage.Committable, NotUsed> createFlow() {
                return FlowWithContext.<Metric, ConsumerMessage.Committable>create()
                        .map(validMetric -> {
                            log(validMetric);
                            return validMetric;
                        })
                        /*
                        Note: if you don't currently have a Lightbend subscription you can optionally comment
                        out the following line referencing CinnamonAttributes and associated import above.
                         */
                        .withAttributes(CinnamonAttributes.instrumentedByName("ValidMetricLogger"));
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
