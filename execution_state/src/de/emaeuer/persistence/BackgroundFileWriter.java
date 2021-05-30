package de.emaeuer.persistence;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BackgroundFileWriter implements AutoCloseable {

    private final BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();

    private final FileWriter writer;

    private final Thread thread;

    public BackgroundFileWriter(String fileName) throws IOException {
        writer = new FileWriter(fileName, false);

        this.thread = new Thread(this::writeTask);
        this.thread.setDaemon(true);
        this.thread.start();
    }

    private void writeTask() {
        try {
            while (true) {
                String lineToWrite = this.writeQueue.take();
                if (!lineToWrite.endsWith("\n")) {
                    lineToWrite = lineToWrite + "\n";
                }
                this.writer.write(lineToWrite);
                this.writer.flush();

                synchronized (writeQueue) {
                    if (writeQueue.isEmpty()) {
                        this.writeQueue.notify();
                    }
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }

    public void writeLine(String line) {
        this.writeQueue.add(line);
    }

    @Override
    public void close() throws Exception {
        // wait until queue is empty
        synchronized (writeQueue) {
            while (!writeQueue.isEmpty()) {
                writeQueue.wait();
            }
        }
        this.thread.interrupt();
        this.writer.close();
    }

}
