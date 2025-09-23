package br.com.fiap.hacka.frameextractorservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import br.com.fiap.hacka.core.commons.dto.NotificacaoDto;
import br.com.fiap.hacka.frameextractorservice.app.queue.MessageProducer;
import br.com.fiap.hacka.frameextractorservice.app.rest.client.NotificacaoServiceClient;
//import io.opentelemetry.api.trace.Span;
//import io.opentelemetry.api.trace.Tracer;
//import io.opentelemetry.context.Scope;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Slf4j
@RequiredArgsConstructor
@Service
public class FilePartConsumer {
    private static final Map<String, FileProcessor> fileProcessors = new ConcurrentHashMap<>();
    private static final Set<String> failedFiles = ConcurrentHashMap.newKeySet();
    private final ExecutorService fileProcessingExecutor;
    //private final Tracer tracer;
    private final MessageProducer messageProducer;
    private final NotificacaoServiceClient notificacaoServiceClient;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${rabbitmq.queue.producer.messageQueue}")
    private String queueName;

    @Value("${file.processing.frame.interval}")
    private int frameInterval;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @RabbitListener(queues = "${rabbitmq.queue.consumer.messageQueue}")
    public void receive(@Payload FilePartDto filePartDto) {
        String fileName = filePartDto.getFileName();
        String userName = filePartDto.getUserName();

        if (filePartDto.isFirstChunk()) {
            failedFiles.remove(fileName + "|" + userName);
        } else if (failedFiles.contains(fileName + "|" + userName)) {
            // skip files that already failed
            log.warn("Skipping chunk for failed file [{}]", fileName);
            return;
        }

        log.info("Received chunk for file [{}], bytesRead={}", fileName, filePartDto.getBytesRead());

        //Span span = tracer.spanBuilder("process-file-chunk").startSpan();
        try /*(Scope scope = span.makeCurrent())*/ {
            fileProcessors
                    .computeIfAbsent(fileName, name -> {
                        FileProcessor processor = new FileProcessor(name, this, frameInterval, s3Client, s3Presigner, bucketName);

                        fileProcessingExecutor.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String urlDownload = processor.process();
                                    notificacaoServiceClient.sendWebhook(
                                            new NotificacaoDto(
                                                    "Arquivo " + fileName + " processado com sucesso. Link download: "+urlDownload,
                                                    filePartDto.getWebhookUrl()));
                                } catch (Exception e) {
                                    log.error("Error processing file [{}]: {}", fileName, e.getMessage(), e);

                                    // mark the file as failed
                                    failedFiles.add(fileName + "|" + userName);

                                    // stop processing further chunks
                                    fileProcessors.remove(fileName);

                                    // mark the DTO with error
                                    filePartDto.setBytesRead(-2);

                                    // notify user just once
                                    notificacaoServiceClient.sendWebhook(
                                            new NotificacaoDto(
                                                    "Ocorreu um erro durante o processamento do arquivo " + fileName + ".",
                                                    filePartDto.getWebhookUrl()));
                                }
                            }
                        });
                        return processor;
                    })
                    .submitPart(filePartDto);

            // send chunk to queue
            this.messageProducer.send(queueName, filePartDto);
        } finally {
            //span.end();
        }
    }

    /*public void receiveOld(FilePartDto filePartDto) {
        String fileName = filePartDto.getFileName();
        log.info("Received chunk for file [{}], bytesRead={}", fileName, filePartDto.getBytesRead());

        Span span = tracer.spanBuilder("process-file-chunk").startSpan();
        try (Scope scope = span.makeCurrent()) {
            fileProcessors
                    .computeIfAbsent(fileName, name -> {
                        FileProcessor processor = new FileProcessor(name, this, frameInterval);
                        fileProcessingExecutor.submit(processor::process);
                        return processor;
                    })
                    .submitPart(filePartDto);

            //posta os chunks na fila para subir o arquivo completo na S3.
            this.messageProducer.send(queueName, filePartDto);
        } finally {
            span.end();
        }
    }*/

    void removeProcessor(String fileName) {
        fileProcessors.remove(fileName);
        log.info("File [{}] - Processor removed", fileName);
    }

    public Optional<FileProcessor> getProcessor(String fileName) {
        return Optional.ofNullable(fileProcessors.get(fileName));
    }
}