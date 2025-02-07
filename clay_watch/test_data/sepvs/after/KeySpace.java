package cau.agse.sepvs.aestask.beckervoigt;

import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.K;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class KeySpace implements Iterator<char[]> {

    //private int[][] space = new int[3][2];
    private long[] flat = new long[8];
    public long progress = 0; //TODO private!

    KeySpace(long[] flat_limits) {
        //this.space = new int[3][2];
        this.flat = flat_limits;
        this.progress = 0;
    }

    public static KeySpace fromString(String source) {

        //System.out.println(source);
        String contents = source.substring(1,source.length()-1);
        String[] entries = contents.split(",|-");

        long[] flat = new long[8];
        for (int i = 0; i < 8; i++) {
            flat[i] = Long.parseLong(entries[i]);
        }
        //System.out.println(flat[7]);
        //System.out.println(new KeySpace(flat).toString());
        return new KeySpace(flat);
    }

    public long key_count() {
        long result = 1;
        for(int i=0; i < 4; i++) {
            //System.out.println(String.format("%d - %d = %d",this.flat[i*2+1],this.flat[i*2],this.flat[i*2+1]-this.flat[i*2] ));
            result *= (this.flat[i*2+1] + 1 - this.flat[i*2]);
        }
        return result;
    }

    public String toString() {
        String result = "[";
        for(int i=0; i < 8; i++) {
            result += this.flat[i] + ", ";
        }
        return result+"]";
    }


    @Override
    public boolean hasNext() {
        return this.progress < this.key_count();
    }

    @Override
    public char[] next() { //TODO inclusive or exclusive boundaries? now, if key goes from 4892 to 4956, 4956 is tried!
        char[] result = new char[4];
        long progress_div = this.progress;

        for (int i = 3; i >= 0; i--) {
            long dimension_size = (this.flat[i*2+1] +1 -this.flat[i*2]);
            result[i] = (char) (this.flat[i*2] + progress_div % dimension_size);
            progress_div = progress_div / dimension_size;
        }

        this.progress ++;
        return result;
    }


    public long flat_first() {
        return this.flat[0];
    }

    public boolean contains(char[] key) {
        return (this.flat[0] <= key[0] &&
                this.flat[1] >= key[0] &&
                this.flat[2] <= key[1] &&
                this.flat[3] >= key[1] &&
                this.flat[4] <= key[2] &&
                this.flat[5] >= key[2] &&
                this.flat[6] <= key[3] &&
                this.flat[7] >= key[3]);
    }


    public static KeySpace superset(KeySpace first, KeySpace second) {
        long[] flat_limits = new long[8];
        flat_limits[0] = Math.min(first.flat[0],second.flat[0]);
        flat_limits[1] = Math.max(first.flat[1],second.flat[1]);
        flat_limits[2] = Math.min(first.flat[2],second.flat[2]);
        flat_limits[3] = Math.max(first.flat[3],second.flat[3]);
        flat_limits[4] = Math.min(first.flat[4],second.flat[4]);
        flat_limits[5] = Math.max(first.flat[5],second.flat[5]);
        flat_limits[6] = Math.min(first.flat[6],second.flat[6]);
        flat_limits[7] = Math.max(first.flat[7],second.flat[7]);

        return new KeySpace(flat_limits);

    }




    public List<KeySpace> split_into_half() {
        List<KeySpace> result = new ArrayList<>();

        long middle = (this.flat[1] - this.flat[0])/2+this.flat[0];
        if (middle+1 > this.flat[1]) return null;

        result.add(new KeySpace(this.flat.clone()));
        result.get(0).flat[1] = middle;
        result.add(new KeySpace(this.flat.clone()));
        result.get(1).flat[0] = middle+1;

        return result;
    }

    /*
    public List<KeySpace> split_into_8ths() {

        List<KeySpace> result = new ArrayList<>();

        long[] middles = new long[] {
                (this.flat[1] - this.flat[0])/2+this.flat[0],
                (this.flat[3] - this.flat[2])/2+this.flat[2],
                (this.flat[5] - this.flat[4])/2+this.flat[4],
                (this.flat[7] - this.flat[6])/2+this.flat[6],
        };



        result.add(new KeySpace(new long[] {
                this.flat[0],middles[0],this.flat[2],middles[1],this.flat[4],middles[2],this.flat[6],middles[3]}));
        result.add(new KeySpace(new long[] {
                this.flat[0],middles[0],this.flat[2],middles[1],this.flat[4],middles[2],middles[3]+1,this.flat[7]}));
        result.add(new KeySpace(new long[] {
                this.flat[0],middles[0],this.flat[2],middles[1],middles[2]+1,this.flat[5],this.flat[6],middles[3]}));
        result.add(new KeySpace(new long[] {
                this.flat[0],middles[0],this.flat[2],middles[1],middles[2]+1,this.flat[5],middles[3]+1,this.flat[7]}));
        result.add(new KeySpace(new long[] {
                this.flat[0],middles[0],middles[1]+1,this.flat[3],this.flat[4],middles[2],this.flat[6],middles[3]}));
        result.add(new KeySpace(new long[] {
                this.flat[0],middles[0],middles[1]+1,this.flat[3],this.flat[4],middles[2],middles[3]+1,this.flat[7]}));
        result.add(new KeySpace(new long[] {
                this.flat[0],middles[0],middles[1]+1,this.flat[3],middles[2]+1,this.flat[5],this.flat[6],middles[3]}));
        result.add(new KeySpace(new long[] {
                this.flat[0],middles[0],middles[1]+1,this.flat[3],middles[2]+1,this.flat[5],middles[3]+1,this.flat[7]}));

        result.add(new KeySpace(new long[] {
                middles[0]+1,this.flat[1],this.flat[2],middles[1],this.flat[4],middles[2],this.flat[6],middles[3]}));
        result.add(new KeySpace(new long[] {
                middles[0]+1,this.flat[1],this.flat[2],middles[1],this.flat[4],middles[2],middles[3]+1,this.flat[7]}));
        result.add(new KeySpace(new long[] {
                middles[0]+1,this.flat[1],this.flat[2],middles[1],middles[2]+1,this.flat[5],this.flat[6],middles[3]}));
        result.add(new KeySpace(new long[] {
                middles[0]+1,this.flat[1],this.flat[2],middles[1],middles[2]+1,this.flat[5],middles[3]+1,this.flat[7]}));
        result.add(new KeySpace(new long[] {
                middles[0]+1,this.flat[1],middles[1]+1,this.flat[3],this.flat[4],middles[2],this.flat[6],middles[3]}));
        result.add(new KeySpace(new long[] {
                middles[0]+1,this.flat[1],middles[1]+1,this.flat[3],this.flat[4],middles[2],middles[3]+1,this.flat[7]}));
        result.add(new KeySpace(new long[] {
                middles[0]+1,this.flat[1],middles[1]+1,this.flat[3],middles[2]+1,this.flat[5],this.flat[6],middles[3]}));
        result.add(new KeySpace(new long[] {
                this.flat[0],middl
        for (int i = 0; i < 8;i++) {
            if (i%2==0) {
                //t f t f t f t f
            }
            if (i%4==0) {
                //
            }
            if (i%2==0) {

            }
        }

        return null;
    }*/
}