package br.com.fiap.hacka.frameextractorservice.app.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FileMetricsTest {

    @Mock
    private ExecutorService executor;

    private MeterRegistry meterRegistry;
    private FileMetrics fileMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        fileMetrics = new FileMetrics(meterRegistry, executor);
    }

    @Test
    void shouldIncrementChunks() {
        fileMetrics.incrementChunks();
        
        Counter chunksCounter = meterRegistry.find("file_chunks_processed_total").counter();
        assertNotNull(chunksCounter);
        assertEquals(1.0, chunksCounter.count());
    }

    @Test
    void shouldIncrementFiles() {
        fileMetrics.incrementFiles();
        
        Counter filesCounter = meterRegistry.find("files_uploaded_total").counter();
        assertNotNull(filesCounter);
        assertEquals(1.0, filesCounter.count());
    }

    @Test
    void shouldIncrementMultipleTimes() {
        fileMetrics.incrementChunks();
        fileMetrics.incrementChunks();
        fileMetrics.incrementFiles();
        
        Counter chunksCounter = meterRegistry.find("file_chunks_processed_total").counter();
        Counter filesCounter = meterRegistry.find("files_uploaded_total").counter();
        
        assertEquals(2.0, chunksCounter.count());
        assertEquals(1.0, filesCounter.count());
    }
}