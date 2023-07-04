package simpledb;

import simpledb.common.Database;
import simpledb.common.Type;
import simpledb.execution.SeqScan;
import simpledb.storage.HeapFile;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionId;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName test
 * @Description test about seqscan
 * @Author jtao
 * @Date 2023/7/4 2:23 下午
 **/

public class test {
    public static void main(String[] args) {
        Type[] types = new Type[]{Type.INT_TYPE, Type.INT_TYPE};
        String[] names = new String[]{"field0", "field1"};
        TupleDesc descriptor = new TupleDesc(types, names);

        HeapFile table1 = new HeapFile(new File("some_data_file.dat"), descriptor);
        Database.getCatalog().addTable(table1, "test");

        TransactionId tid = new TransactionId();
        SeqScan f = new SeqScan(tid, table1.getId());

        try {
            f.open();
            while(f.hasNext()){
                System.out.println(f.next());
            }
            f.close();
            Database.getBufferPool().transactionComplete(tid);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
}
