package br.com.fiap.hacka.frameextractorservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

@Slf4j
public class FileProcessor {

    private final String fileName;
    private final BlockingQueue<FilePartDto> queue = new LinkedBlockingQueue<>();
    private final PipedOutputStream pos = new PipedOutputStream();
    private final PipedInputStream pis;
    private final FilePartConsumer parent;
    private final File zipFile;
    private volatile boolean finished = false;
    private final int frameInterval;

    public FileProcessor(String fileName, FilePartConsumer parent, int frameInterval) {
        this.fileName = fileName;
        this.parent = parent;
        this.frameInterval = frameInterval;
        try {
            this.pis = new PipedInputStream(pos, FilePartDto.CHUNK_SIZE); // 1MB buffer
        } catch (IOException e) {
            throw new RuntimeException("Pipe init failed for file " + fileName, e);
        }

        this.zipFile = new File("/tmp/frames/" + fileName + ".zip");
        this.zipFile.getParentFile().mkdirs();
    }

    public void submitPart(FilePartDto part) {
        queue.offer(part);
    }

    public void process() {
        // Step 1: Collect all chunks sequentially from the queue
        ByteArrayOutputStream videoBuffer = new ByteArrayOutputStream();
        try {
            while (true) {
                FilePartDto part = queue.take(); // blocks until chunk arrives
                if (part.getBytesRead() == -1) {
                    part.setFrameFilePath("caminho_no_S3"); //TODO incluir a url do arquivo na S3.!
                    break; // EOF
                }
                videoBuffer.write(part.getBytes(), 0, part.getBytesRead());
            }

            try (InputStream videoStream = new ByteArrayInputStream(videoBuffer.toByteArray());
                 FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zipOut = new ZipOutputStream(fos)) {

                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoStream);
                grabber.start();

                Java2DFrameConverter converter = new Java2DFrameConverter();
                int frameIndex = 0;
                Frame frame;
                long lastExtractedTime = 0;
                long interval = frameInterval * 1000000L; // intervale in milliseconds

                while ((frame = grabber.grabImage()) != null) {
                    long timestamp = grabber.getTimestamp(); // current frame timestamp
                    if (lastExtractedTime == 0 || timestamp >= lastExtractedTime + interval) {
                        lastExtractedTime = timestamp;

                        BufferedImage img = converter.getBufferedImage(frame);
                        String entryName = fileName + "_frame_" + (frameIndex++) + ".jpg";
                        zipOut.putNextEntry(new ZipEntry(entryName));

                        // --- Use ImageWriter + ImageWriteParam for optimized JPEG compression ---
                        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                        if (!writers.hasNext()) {
                            throw new IllegalStateException("No JPEG writer found");
                        }
                        ImageWriter writer = writers.next();

                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                             ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                            writer.setOutput(ios);
                            ImageWriteParam param = writer.getDefaultWriteParam();
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(0.75f); // Adjust quality (0.0 = low, 1.0 = best)
                            writer.write(null, new IIOImage(img, null, null), param);
                            zipOut.write(baos.toByteArray());
                        } finally {
                            writer.dispose();
                        }
                        zipOut.closeEntry();
                    }
                }
                grabber.stop();
                parent.removeProcessor(fileName);
                log.info("File {} processed and zipped successfully: {}", fileName, zipFile.getAbsolutePath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Process the file:
     * - Concatenate chunks in memory using SequenceInputStream
     * - Extract frames via Java2D
     * - Write frames as JPG directly into ZipOutputStream
     */
    public void processOk() {
        try {
            // Step 1: Collect all chunks sequentially from the queue
            ByteArrayOutputStream videoBuffer = new ByteArrayOutputStream();
            while (true) {
                FilePartDto part = queue.take(); // blocks until chunk arrives
                if (part.getBytesRead() == -1) break; // EOF
                videoBuffer.write(part.getBytes(), 0, part.getBytesRead());
            }

            try (InputStream videoStream = new ByteArrayInputStream(videoBuffer.toByteArray());
                 FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zipOut = new ZipOutputStream(fos)) {

                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoStream);
                grabber.start();

                Java2DFrameConverter converter = new Java2DFrameConverter();
                int frameIndex = 0;
                Frame frame;

                while ((frame = grabber.grabImage()) != null) {
                    BufferedImage img = converter.getBufferedImage(frame);
                    String entryName = fileName + "_frame_" + (frameIndex++) + ".jpg";
                    zipOut.putNextEntry(new ZipEntry(entryName));

                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        ImageIO.write(img, "jpg", baos);
                        zipOut.write(baos.toByteArray());
                    }

                    zipOut.closeEntry();
                }

                grabber.stop();
            }

            parent.removeProcessor(fileName);
            log.info("File {} processed and zipped successfully: {}", fileName, zipFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Error processing file {}", fileName, e);
        }
    }

    public void processNok() {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            // Thread to read frames from FFmpegFrameGrabber
            Thread grabberThread = new Thread(() -> {
                try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(pis)) {
                    grabber.start();
                    Java2DFrameConverter converter = new Java2DFrameConverter();
                    int frameIndex = 0;
                    Frame frame;

                    while ((frame = grabber.grabImage()) != null) {
                        BufferedImage img = converter.getBufferedImage(frame);
                        String entryName = fileName + "_frame_" + (frameIndex++) + ".jpg";
                        zipOut.putNextEntry(new ZipEntry(entryName));

                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            ImageIO.write(img, "jpg", baos);
                            zipOut.write(baos.toByteArray());
                        }

                        zipOut.closeEntry();
                    }

                    grabber.stop();
                } catch (Exception e) {
                    log.error("Grabber thread error for file {}", fileName, e);
                }
            });

            grabberThread.start();

            // Write chunks from queue to PipedOutputStream
            while (true) {
                FilePartDto part = queue.take();
                if (part.getBytesRead() == -1) break; // EOF
                pos.write(part.getBytes(), 0, part.getBytesRead());
                pos.flush();
            }

            pos.close();
            grabberThread.join();
            log.info("File {} processed and zipped: {}", fileName, zipFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Processing error for file {}", fileName, e);
        }
    }

    public void processNoJava2D() {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            // FFmpeg grabber thread
            Thread grabberThread = new Thread(() -> {
                try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(pis)) {
                    grabber.start();
                    int frameIndex = 0;
                    Frame frame;

                    while ((frame = grabber.grabImage()) != null) {
                        String entryName = fileName + "_frame_" + (frameIndex++) + ".jpg";
                        zipOut.putNextEntry(new ZipEntry(entryName));

                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                             FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
                                     baos, frame.imageWidth, frame.imageHeight, 0)) {

                            recorder.setFormat("jpeg");
                            recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MJPEG);
                            recorder.setPixelFormat(grabber.getPixelFormat());
                            recorder.setVideoQuality(2);
                            recorder.start();
                            recorder.record(frame);
                            recorder.stop();

                            zipOut.write(baos.toByteArray());
                        }

                        zipOut.closeEntry();
                    }

                    grabber.stop();
                } catch (Exception e) {
                    log.error("Grabber error for file {}", fileName, e);
                }
            });

            grabberThread.start();

            // Feed chunks to PipedOutputStream
            while (true) {
                FilePartDto part = queue.take();
                if (part.getBytesRead() == -1) break; // EOF
                pos.write(part.getBytes(), 0, part.getBytesRead());
                pos.flush();
            }

            pos.close();
            grabberThread.join();
            log.info("File {} processed and zipped: {}", fileName, zipFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Processing error for file {}", fileName, e);
        }
    }

    public File getZipFile() {
        return zipFile;
    }

    public void processWithJava2D() {
        Future<?> grabberFuture = Executors.newSingleThreadExecutor().submit(() -> {
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(pis)) {
                grabber.start();
                Frame frame;
                int index = 0;

                Java2DFrameConverter converter = new Java2DFrameConverter();

                while ((frame = grabber.grabImage()) != null) {
                    BufferedImage img = converter.convert(frame);
                    File output = new File(zipFile, String.format("frame_%05d.png", index++));
                    ImageIO.write(img, "png", output);
                }

                grabber.stop();
                finished = true;
                log.info("File [{}] - Finished extracting frames", fileName);

            } catch (Exception e) {
                log.error("File [{}] - Grabber failed", fileName, e);
            }
        });

        try {
            while (true) {
                FilePartDto part = queue.take();
                if (part.getBytesRead() == -1) {
                    break;
                }
                pos.write(part.getBytes(), 0, part.getBytesRead());
                pos.flush();
            }
            pos.close();
            grabberFuture.get();
        } catch (Exception e) {
            log.error("File [{}] - Writer error", fileName, e);
        } finally {
            //parent.removeProcessor(fileName);
        }
    }

    public boolean isFinished() {
        return finished;
    }

    public File zipFrames() throws IOException {
        File zipFile = new File("/tmp/zips/" + fileName + ".zip");
        zipFile.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            File[] frames = zipFile.listFiles((dir, name) -> name.endsWith(".png"));
            if (frames != null) {
                for (File frame : frames) {
                    try (FileInputStream fis = new FileInputStream(frame)) {
                        ZipEntry entry = new ZipEntry(frame.getName());
                        zos.putNextEntry(entry);

                        byte[] buffer = new byte[FilePartDto.CHUNK_SIZE];
                        int len;
                        while ((len = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }

                        zos.closeEntry();
                    }
                }
            }
        }

        log.info("File [{}] - Frames zipped successfully: {}", fileName, zipFile.getAbsolutePath());
        return zipFile;
    }

    public void process_old() {
        FilePartDto lastPart = null;
        Future<?> grabberFuture = Executors.newSingleThreadExecutor().submit(() -> {
            try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(pis)) {
                grabber.start();
                Frame frame;
                while ((frame = grabber.grabImage()) != null) {
                    log.info("File [{}] - Got frame {}x{}", fileName, frame.imageWidth, frame.imageHeight);
                }
                grabber.stop();
            } catch (Exception e) {
                log.error("File [{}] - Grabber error", fileName, e);
            }
        });

        try {
            while (true) {
                FilePartDto part = queue.take();
                if (part.getBytesRead() == -1) {
                    lastPart = part;
                    break; // EOF
                }
                pos.write(part.getBytes(), 0, part.getBytesRead());
                pos.flush();

                //postar na fila uploadS3 a parte.
            }
            pos.close();
            grabberFuture.get();
        } catch (Exception e) {
            log.error("File [{}] - Writer error", fileName, e);
        } finally {
            //parent.removeProcessor(fileName); // cleanup in your existing class
        }

        //armazenar a url do arquivo armazenado.
        //lastPart.setFrameFilePath();
        //postar ultima parte na fila.
    }
}