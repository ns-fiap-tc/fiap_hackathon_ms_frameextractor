package br.com.fiap.hacka.frameextractorservice.app.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class FileMetrics {
    private final Counter chunksProcessed;
    private final Counter filesUploaded;
    //private final Gauge activeThreads;

    private final ExecutorService executor;

    public FileMetrics(MeterRegistry registry, ExecutorService executor) {
        this.executor = executor;

        this.chunksProcessed = Counter.builder("file_chunks_processed_total")
                .description("Number of file chunks processed")
                .register(registry);

        this.filesUploaded = Counter.builder("files_uploaded_total")
                .description("Number of completed file uploads")
                .register(registry);
/*
        Gauge.builder("executor_active_threads", executor,
                        ex -> ((ThreadPoolExecutor) ex).getActiveCount())
                .description("Active threads in file processing executor")
                .tag("executor", "file.processing.executor")
                .register(registry);
 */
    }

    public void incrementChunks() {
        chunksProcessed.increment();
    }

    public void incrementFiles() {
        filesUploaded.increment();
    }
}