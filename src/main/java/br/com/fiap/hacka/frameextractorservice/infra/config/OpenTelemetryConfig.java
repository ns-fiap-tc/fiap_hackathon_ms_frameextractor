package br.com.fiap.hacka.frameextractorservice.infra.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {
/*
    //configurar a url do pometheus "na mao"
    @Bean
    public OtlpMeterRegistry otlpMeterRegistry() {
        OtlpConfig config = new OtlpConfig() {
            @Override
            public String get(String key) {
                return null; // use defaults
            }
            @Override
            public String url() {
                return "http://localhost:4318/v1/metrics";
            }
        };
        return new OtlpMeterRegistry(config, Clock.SYSTEM);
    }
    */
/*
    @Bean
    public OpenTelemetry openTelemetry() {
        // Create a resource with application metadata
        Resource resource = Resource.getDefault()
                .merge(Resource.create(
                        Attributes.of(
                                io.opentelemetry.semconv.resource.attributes.ResourceAttributes.ResourceAttributes.SERVICE_NAME, "demo-app",
                                ResourceAttributes.SERVICE_VERSION, "1.0.0"
                )));

        // Set up the Prometheus exporter
        PrometheusHttpServer prometheusExporter = PrometheusHttpServer.builder()
                .setPort(9464)
                .build();

        // Create a meter provider
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(prometheusExporter)
                .build();

        // Create and return the OpenTelemetry instance
        return OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .buildAndRegisterGlobal();
    }

    @Bean
    public MeterProvider meterProvider(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeterProvider();
    }
*/
/*
    @Bean
    public OpenTelemetry openTelemetry() {
        return AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk();
    }
*/

/*
    @Bean
    public Tracer tracer() {
        //return openTelemetry.getTracer("br.com.fiap.hacka.frameextractorservice");
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint("http://localhost:4317")
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        return openTelemetry.getTracer("br.com.fiap.hacka.frameextractorservice");
    }
*/
/*
    @Bean(name = "prometheusServer")
    public PrometheusHttpServer prometheusServer(@Value("${prometheus.server.port}") int serverPort) {
        // This will expose metrics at http://localhost:9464/metrics
        PrometheusHttpServer server = PrometheusHttpServer.builder()
                .setPort(serverPort)
                .build();
        return server;
    }
 */
}