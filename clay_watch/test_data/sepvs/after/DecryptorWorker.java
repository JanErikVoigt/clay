package cau.agse.sepvs.aestask.beckervoigt;

import cau.agse.sepvs.tools.Misc;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;

import static cau.agse.sepvs.aestask.beckervoigt.DecryptFiles.done_all_tasks;

public class DecryptorWorker extends Thread {

    public ConcurrentLinkedQueue<Task> task_queue;
    private Task current_task = null;
    private ConcurrentLinkedQueue<Task> solved_tasks;
    public boolean terminate_when_queue_empty = false;
    public int id;

    public DecryptorWorker(ConcurrentLinkedQueue<Task> solved_tasks, int worker_id) {
        this.task_queue = new ConcurrentLinkedQueue<>();
        this.solved_tasks = solved_tasks;
        this.id = worker_id;
    }

    //try to decrypt using next key
    //return true, if this key worked
    private byte[] try_next_key() {

        if (!current_task.search_space.hasNext()) return null;

        char[] keyArray = current_task.search_space.next();
        byte[] inputBytes = current_task.get_content(); //assert loaded

        Cipher cipher;

        byte[] decrypted_message = null;
        boolean failed = false;
        try {
            cipher = Util.getAESCipher(keyArray, Cipher.DECRYPT_MODE);
            decrypted_message = cipher.doFinal(inputBytes);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) { //TODO dont use Exceptions, as they are slow (apparently)
            return null;
        } catch (IllegalBlockSizeException e) {
            System.out.println("illegal blocksize");
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        //verify decrypted message:

        if (decrypted_message == null) {
            return null;
        }

        boolean success = Util.checksum_correct(decrypted_message,current_task.checksum);
        if (success) {
            return decrypted_message;
        } else {
            return null;
        }
        //TODO write/return result?
        //TODO cancel other workers on same task!
    }

    public void run() {

        while(true) {

            if (current_task == null) {
                current_task = this.task_queue.poll();
                if (current_task == null) { //what to do when empty?
                    if (this.terminate_when_queue_empty) {
                        return;
                    }
                    Misc.sleep(200); //TODO busy waiting :/
                    continue;
                }
                System.out.println(String.format("Current Task: %s keycount:%d",current_task.file_name, current_task.search_space.key_count()));
                //TODO watch out when empty
            }

            byte[] successful_decryption = null;
            for (int i = 0; i < 20 && successful_decryption != null; i++) { //do 20 trys at a time
                successful_decryption = this.try_next_key();
            }

            if (successful_decryption != null) {
                //System.out.println(String.format("Successfully decrypted %s.enc",current_task.file_name));
                this.current_task.cleartext = successful_decryption;
                this.current_task.worker_id = this.id;
                this.solved_tasks.add(this.current_task);
                current_task = null;

                //TODO save result?
                //TODO notify tasks with same file
            } else {
                if (!current_task.search_space.hasNext()) {
                    System.out.println(String.format("failed task %s!",current_task.file_name));
                    current_task = null;
                    continue;
                }
            }
        }
    }

}
