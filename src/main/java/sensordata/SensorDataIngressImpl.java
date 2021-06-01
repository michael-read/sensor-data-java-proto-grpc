package sensordata;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import cloudflow.akkastream.WritableSinkRef;

import java.util.concurrent.CompletionStage;

public class SensorDataIngressImpl implements SensorDataService {
    private final WritableSinkRef<SensorData> sinkRef;

    public SensorDataIngressImpl(WritableSinkRef<SensorData> sinkRef) {
        this.sinkRef = sinkRef;
    }

    @Override
    public CompletionStage<SensorDataReply> provide(SensorData in) {
        return sinkRef.writeJava(in)
                .thenApply(i -> SensorDataReply.newBuilder()
                        .setDeviceId(in.getDeviceId())
                        .setSuccess(true)
                        .build());
        // TODO: Look into recovery or failure
    }

    @Override
    public Source<SensorDataReply, NotUsed> provideStreamed(Source<SensorData, NotUsed> in) {
        // TODO: build streaming implementation
        return null;
    }
}
