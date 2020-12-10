package trace.generator;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import trace.generator.Counters;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.SECONDS;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DDAgentV05 {

    private String agentUrl;
    private List<Path> requests;

    private HttpClient client;
    private HttpRequest.Builder requestBuilder;

    @Setup(Level.Trial)
    public void unpackBuffers() throws IOException {
        this.agentUrl = System.getProperty("dd.agent.url", "http://localhost:8126");
        Path payloads = Paths.get(System.getProperty("user.dir")).resolve("payloads");
        try (Stream<Path> files = Files.list(payloads)) {
            requests = files.collect(Collectors.toList());
        }
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(5, SECONDS))
                .executor(ForkJoinPool.commonPool())
                .build();
        this.requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(agentUrl + "/v0.5/traces"))
                .header("Datadog-Meta-Lang", "java")
                .header("Datadog-Meta-Lang-Version", System.getProperty("java.version"))
                .header("Datadog-Meta-Lang-Interpreter", System.getProperty("java.vm.name"))
                .header("Datadog-Meta-Lang-Interpreter-Vendor", System.getProperty("java.vm.vendor"))
                .header("Datadog-Meta-Tracer-Version", "benchmark");
    }

    @Benchmark
    public void sendTraces(Counters counters, Blackhole bh) {
        for (Path request : requests) {
            try {
                HttpResponse<Void> response = client.send(requestBuilder.copy()
                                .PUT(HttpRequest.BodyPublishers.ofFile(request)).build(),
                        HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() < 400) {
                    counters.success++;
                } else {
                    counters.failed++;
                }
                bh.consume(response);
            } catch (IOException | InterruptedException e) {
                counters.failed++;
            }
        }
    }



}
