package sensordata;

import akka.stream.javadsl.RunnableGraph;

import cloudflow.akkastream.AkkaStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.javadsl.RunnableGraphStreamletLogic;
import cloudflow.streamlets.RoundRobinPartitioner;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.proto.javadsl.ProtoInlet;
import cloudflow.streamlets.proto.javadsl.ProtoOutlet;

import sensordata.MetricProto.Metric;

import java.util.Arrays;

public class SensorDataToMetrics extends AkkaStreamlet {
    private final ProtoInlet<SensorData> inlet = new ProtoInlet<SensorData>(
            "in",
            SensorData.class,
            true,
            (inBytes, throwable) -> null
    );
    public final ProtoOutlet<Metric> outlet =
            new ProtoOutlet<>("out", RoundRobinPartitioner.getInstance(), Metric.class);

    @Override
    public StreamletShape shape() {
        return StreamletShape.createWithInlets(inlet).withOutlets(outlet);
    }

    @Override
    public AkkaStreamletLogic createLogic() {
        return new RunnableGraphStreamletLogic(getContext()) {
            @Override
            public RunnableGraph<?> createRunnableGraph() {
                return getSourceWithCommittableContext(inlet)
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
                        .to(getCommittableSink());
            }
        };
    }


}
