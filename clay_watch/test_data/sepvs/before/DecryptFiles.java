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

        int which_demo = 4;


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
            workers.add(new DecryptorWorker(solved_tasks));
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
            //TODO assert exactly one entry
            List<String> keypatterns_lines =  FileTools.stringsFromFile(keypatterns_path.get(0).toString());


            List<Path> checksums_path = ciphertextFiles.stream()
                    .filter(p -> p.getFileName().toString().equals("shasums")).collect(Collectors.toList());
            //TODO assert exactly one entry
            List<String> checksums_lines =  FileTools.stringsFromFile(checksums_path.get(0).toString());

            //generate tasks
            for (int i=0; i < keypatterns_lines.size(); i++) {
                String keypatternline = keypatterns_lines.get(i);
                String checksums_line = checksums_lines.get(i);

                String[] split = keypatternline.split("\\s+");
                long filesize = Util.getFileSize(ciphertextDirectoryPath.toString() + "/"+split[1] +".enc");
                String checksum = checksums_line.split("\\s+")[0]; //TODO hex -> long
                Task task = new Task(ciphertextDirectoryPath, cleartextDirectory, split[1],filesize, KeySpace.fromString(split[0]),checksum);
                tasks.add(task);
            }
        }



        //find keyspace overlap
        //TODO List<Pair<KeySpace,List<Task>>> total_keyspace = DecryptFiles.find_total_keyspace(tasks);
        //DecryptFiles.find_keyspace_overlap(tasks);


        // if there are too few tasks to split between workers, split up keyspaces!
        //TODO implement!

        //TODO fill workers on startup with 10 simple tasks, immediately. Then, apply proper scheduling.

        //TODO implement smarter scheduling!
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

        // create directories for decrypted files
        for (String decryptionDirectory : decryptionDirectories) {
            FileTools.createDirectory(Paths.get(decryptionDirectory));
        }

        // 4. assign 50% of the work on startup!
        long assigned_task_count = 0;
        for (int i=0; i < tasks.size() / 2; i++) {
            workers.get(i%worker_count).task_queue.add(tasks.get(i));
            assigned_task_count += 1;
        }
        tasks = tasks.subList(tasks.size() / 2, tasks.size());


        //TODO schedule rest dynamically




        // write solved tasks to files! schedule further work!
        long solved_task_count = 0;
        while (solved_task_count < assigned_task_count) {
            System.out.println(String.format("solved %d / %d tasks", solved_task_count, assigned_task_count));
            if (solved_tasks.peek() != null) {
                Task solved_task = solved_tasks.poll();
                System.out.println(String.format("Solved: %s", solved_task.file_name));

                Util.writeBytesToFile(solved_task.target_filepath(),solved_task.cleartext);
                solved_task_count++;
            } else {
                Misc.sleep(1234);
            }
        }

        System.out.println("done");
        done_all_tasks = true;
        return;
        /*for (DecryptorWorker worker : workers) {
            worker.join();
        }*/

    }



    private static List<Pair<KeySpace, List<Task>>> find_keyspace_overlap(List<Task> tasks) {
        //List<Pair<Integer, KeySpace>> disjunct_keyspaces = new ArrayList<>();

        /*KeySpace all_keys_superset = new KeySpace(tasks.get(0).search_space.flat);
        for (Task task : tasks) {
            all_keys_superset = KeySpace.superset(all_keys_superset, task.search_space);
        }
        System.out.println(all_keys_superset.key_count());
        */


        Map<char[],Integer> key_occurences = new HashMap<>();
        for (Task task : tasks) {
            while (task.search_space.hasNext()) {
                char[] key = task.search_space.next();
                if (!key_occurences.containsKey(key)) {
                    key_occurences.put(key,1);
                } else {
                    key_occurences.replace(key, key_occurences.get(key)+1);
                }
            }
        }

        List<Integer> occucence_distribution = new ArrayList<>();
        for (char[] key : key_occurences.keySet()) {
            int amount = key_occurences.get(key);
            while (occucence_distribution.size() <= amount) {
                occucence_distribution.add(0);
            }
            occucence_distribution.set(amount,occucence_distribution.get(amount)+1);
        }
        Misc.printArray("occurence distr",occucence_distribution.toArray(),0,occucence_distribution.size()-1);

        return  null;
    }

    private static List<Pair<KeySpace, List<Task>>> find_total_keyspace(List<Task> tasks) {
        List<Pair<Integer, KeySpace>> disjunct_keyspaces = new ArrayList<>();
        List<KeySpace> keyspaces = new ArrayList<>();

        for (Task t : tasks) {
            keyspaces.add(t.search_space);
        }

        keyspaces.sort(Comparator.comparingLong(KeySpace::flat_first));

        for (KeySpace k : keyspaces) {
            System.out.println(k);
        }


        //limits =
        return null;
    }

}
