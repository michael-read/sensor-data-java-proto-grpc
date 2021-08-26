package sensordata;

import akka.grpc.javadsl.ServerReflection;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.japi.Function;

import cloudflow.akkastream.AkkaServerStreamlet;
import cloudflow.akkastream.AkkaStreamletLogic;
import cloudflow.akkastream.util.javadsl.GrpcServerLogic;
import cloudflow.streamlets.RoundRobinPartitioner;
import cloudflow.streamlets.StreamletShape;
import cloudflow.streamlets.proto.javadsl.ProtoOutlet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;

// tag::ingress[]
public class SensorDataGrpcIngress extends AkkaServerStreamlet {
    public final ProtoOutlet<SensorData> out =
            new ProtoOutlet<>("out", RoundRobinPartitioner.getInstance(), SensorData.class);

    public StreamletShape shape() {
        return StreamletShape.createWithOutlets(out);
    }

    public AkkaStreamletLogic createLogic() {
        return new GrpcServerLogic(this, getContext()) {
            public List<Function<HttpRequest, CompletionStage<HttpResponse>>> handlers() {
                return Arrays.asList(
                        SensorDataServiceHandlerFactory.partial(new SensorDataIngressImpl(sinkRef(out)), SensorDataService.name, system()),
                        ServerReflection.create(Collections.singletonList(SensorDataService.description), system()));
            }
        };
    }

}
// end::ingress[]