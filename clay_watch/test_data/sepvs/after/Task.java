package cau.agse.sepvs.aestask.beckervoigt;

import cau.agse.sepvs.tools.FileTools;

import java.io.IOException;
import java.nio.file.Path;

public class Task {

    private Path folder_path_cipher;
    private String folder_path_cleartext;

    public final String file_name;
    private long size_bytes;
    public KeySpace search_space;
    private byte[] content;
    public final String checksum; //TODO long or something?
    public byte[] cleartext;

    public int worker_id;
    //public boolean solved = false;



    Task(Path folder_path_cipher, String folder_path_cleartext, String file_name, long size_bytes, KeySpace search_space, String checksum) {
        this.folder_path_cipher = folder_path_cipher;
        this.folder_path_cleartext = folder_path_cleartext;
        this.file_name = file_name;
        this.size_bytes = size_bytes;
        this.search_space = search_space; //load keypatterns
        this.content = null;
        this.checksum = checksum;
        this.cleartext = null;
        // load checksum shasums
        this.worker_id = worker_id;
    }

    public Path target_filepath() {
        return Path.of(this.folder_path_cleartext, this.file_name);
    }

    public String toString() {
        return String.format("Task: %s  work: %d",
                this.file_name,
                this.estimate_work(),
                this.size_bytes,
                this.search_space.key_count());
    }

    public long estimate_work() {
        return this.search_space.key_count();
    }

    //returns success
    public boolean loadContent() {
        if (this.content != null) {
            return false;
        }
        String filename = this.folder_path_cipher.toString() + "/" + this.file_name +".enc";
        try {
            this.content = FileTools.bytesFromFile(Path.of(filename));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public byte[] get_content() {
        return this.content;
    }
}
