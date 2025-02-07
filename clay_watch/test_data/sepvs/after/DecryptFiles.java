// 12s for runDecryption
//exact copy of DecryptFiles from demo

// This is just a demo application that can decrypt files only if they are the *exact*
// result of the encryption operation, since the order in which the round keys are
// generated must be the same as in the encryption process.

package cau.agse.sepvs.aestask.beckervoigt;

import static cau.agse.sepvs.aestask.demo.EncryptFiles.defaultCiphertextDirectories;
import static cau.agse.sepvs.aestask.demo.EncryptFiles.defaultDecryptionDirectories;
import static cau.agse.sepvs.aestask.demo.EncryptFiles.keyGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import cau.agse.sepvs.aestask.insecurecrypto.InsecureAESAccess;
import cau.agse.sepvs.tools.FileTools;
import cau.agse.sepvs.tools.Misc;
import org.apache.commons.math3.util.Pair;

public class DecryptFiles {

    public static int worker_count = -1; //if <= 0: take processorcount +2!

    public static Path defaultDecryptDirectory = Paths.get("output/plaintexts/demo-02");
    static volatile boolean done_all_tasks = false;

    public static void main(String[] args) throws IOException {
        int which_demo = -1;

        final List<String> ciphertextDirectories;
        final List<String> decryptionDirectories;

        // Encrypt directories given on command-line, if present. Treat these as
        // alternating input (ciphertext) and output (plaintext) directories, so
        // the number of arguments must be even.
        if (args.length != 0) {
            ciphertextDirectories = new ArrayList<>();
            decryptionDirectories = new ArrayList<>();
            if (args.length % 2 != 0) {
                System.out.println("Expected even number of arguments.");
                System.exit(0);
            }
            for (int index = 0; index < args.length / 2; index++) {
                ciphertextDirectories.set(index, args[2 * index]);
                decryptionDirectories.set(index, args[2 * index + 1]);
            }
        }
        else {
            ciphertextDirectories = defaultCiphertextDirectories;
            decryptionDirectories = defaultDecryptionDirectories;
        }

        // 1. startup workers
        int cores = Runtime.getRuntime().availableProcessors();
        int worker_count = DecryptFiles.worker_count;
        if (worker_count <= 0)
            worker_count = cores + 2; //(int) (cores * 1.4);

        List<DecryptorWorker> workers = new ArrayList<>();
        ConcurrentLinkedQueue<Task> solved_tasks = new ConcurrentLinkedQueue<>();
        for (int i=0; i < worker_count; i++) {
            workers.add(new DecryptorWorker(solved_tasks,i));
            workers.get(i).start();
        }

        // 1. discover directories.
        List<Task> tasks = new ArrayList<>();
        for (int dir_i = 0; dir_i < ciphertextDirectories.size(); dir_i++ ) {
            if (which_demo >= 0 && which_demo != dir_i +1) {
                continue;
            }
            String ciphertextDirectory = ciphertextDirectories.get(dir_i);
            String cleartextDirectory = decryptionDirectories.get(dir_i);
            //
            Path ciphertextDirectoryPath = Paths.get(ciphertextDirectory);
            List<Path> ciphertextFiles = FileTools.filesInDirectory(ciphertextDirectoryPath);

            //find keypatterns as a first step!
            List<Path> keypatterns_path = ciphertextFiles.stream()
                    .filter(p -> p.getFileName().toString().equals("keypatterns")).collect(Collectors.toList());
            //assert exactly one entry
            List<String> keypatterns_lines =  FileTools.stringsFromFile(keypatterns_path.get(0).toString());


            List<Path> checksums_path = ciphertextFiles.stream()
                    .filter(p -> p.getFileName().toString().equals("shasums")).collect(Collectors.toList());
            //assert exactly one entry
            List<String> checksums_lines =  FileTools.stringsFromFile(checksums_path.get(0).toString());

            //generate tasks
            for (int i=0; i < keypatterns_lines.size(); i++) {
                String keypatternline = keypatterns_lines.get(i);
                String checksums_line = checksums_lines.get(i);

                String[] split = keypatternline.split("\\s+");
                long filesize = Util.getFileSize(ciphertextDirectoryPath.toString() + "/"+split[1] +".enc");
                String checksum = checksums_line.split("\\s+")[0];
                Task task = new Task(ciphertextDirectoryPath, cleartextDirectory, split[1],filesize, KeySpace.fromString(split[0]),checksum);
                tasks.add(task);
            }
        }

        // if there are too few tasks to split between workers, split up keyspaces!
        //TODO implement!

        tasks.sort(Comparator.comparingLong(Task::estimate_work).reversed());
        //they are all of equal size!


        for (int i_task=0; i_task < tasks.size(); i_task++) {
            boolean success = tasks.get(i_task).loadContent();
            if (!success) {
                throw new IOException(String.format("couldnt load file contents of file: %s",tasks.get(i_task).file_name));
            }
        }


        // 2. split work
        // TODO maybe start work intensive early and do small and large 50-50 or something
        /*if (tasks.size() < 2*worker_count) {
            //split all tasks into halves and assign those!
            //TODO
        }*/



        // 4. assign 50% of the work on startup!
        long assigned_task_count = 0;
        List<Integer> assigned_taskcount_by_worker = new ArrayList<>();
        for (int i = 0; i < workers.size(); i++) {
            assigned_taskcount_by_worker.add(0);
        }
        for (int i=0; i < tasks.size() / 2; i++) {
            workers.get(i%worker_count).task_queue.add(tasks.get(i));
            assigned_task_count += 1;
            assigned_taskcount_by_worker.set(i%worker_count,
                    assigned_taskcount_by_worker.get(i%worker_count)+1);
        }
        tasks = tasks.subList(tasks.size() / 2, tasks.size());

        // create directories for decrypted files
        for (String decryptionDirectory : decryptionDirectories) {
            FileTools.createDirectory(Paths.get(decryptionDirectory));
        }


        // write solved tasks to files! schedule further work!
        long solved_task_count = 0;
        List<Integer> solved_taskcount_by_worker = new ArrayList<>();
        for (int i = 0; i < workers.size(); i++) {
            solved_taskcount_by_worker.add(0);
        }

        while (solved_task_count < assigned_task_count) {
            System.out.println(String.format("solved %d / %d tasks", solved_task_count, assigned_task_count));
            if (solved_tasks.peek() != null) {
                Task solved_task = solved_tasks.poll();
                System.out.println(String.format("Solved: %s", solved_task.file_name));

                Util.writeBytesToFile(solved_task.target_filepath(),solved_task.cleartext);
                solved_task_count++;
                solved_taskcount_by_worker.set(solved_task.worker_id,solved_taskcount_by_worker.get(solved_task.worker_id) +1);
            } else {
                Misc.sleep(234);
            }

            //dynamically assign remaining tasks!
            //if many tasks are remaining, schedule early.
            // if only few remain, only reassign when empty!;
            if (tasks.size() > 0) { //there are tasks to be assigned
                for(int i =0; i < worker_count;i++) {
                    int task_amount = assigned_taskcount_by_worker.get(i) - solved_taskcount_by_worker.get(i);

                    int limit_for_assigning = 0;
                    if (tasks.size() > worker_count) {
                        limit_for_assigning = 1;
                    }
                    if (tasks.size() > 2*worker_count) {
                        limit_for_assigning = 2;
                    }
                    if (tasks.size() > 3*worker_count) {
                        limit_for_assigning = 3;
                    }

                    if (task_amount <= limit_for_assigning) {
                        if (!tasks.isEmpty()) {
                            workers.get(i).task_queue.add(tasks.removeFirst());
                        }
                    }
                }


                if (tasks.size() == 0) { //tasks ran empty this iteration!
                    for (DecryptorWorker worker : workers) {
                        worker.terminate_when_queue_empty = true;
                    }
                }
            }
        }
        System.out.println("done, terminating");
        for (DecryptorWorker worker : workers) {
            boolean retry = true;
            while(retry) {
                try {
                    worker.join();
                    retry = false;
                } catch (InterruptedException e) {
                    //try again
                }
            }

        }
    }
}
