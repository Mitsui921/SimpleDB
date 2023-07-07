package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Debug;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;

import javax.xml.crypto.Data;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @author Sam Madden
 * @see HeapPage#HeapPage
 */
public class HeapFile implements DbFile {

    private File file;

    private TupleDesc tupleDesc;

    /**
     * Hint: you will need random access to the file in order to read and write pages at arbitrary offsets.
     * You should not call BufferPool instance methods when reading a page from disk.
     */
    public class MyIterator implements DbFileIterator {
        TransactionId tid;
        Permissions permissions;
        BufferPool bufferPool = Database.getBufferPool();
        Iterator<Tuple> iterator;
        int num = 0;

        public MyIterator(TransactionId tid, Permissions permissions){
            this.tid = tid;
            this.permissions = permissions;
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            num = 0;
            HeapPageId heapPageId = new HeapPageId(getId(), num);
            HeapPage heapPage = (HeapPage) bufferPool.getPage(tid, heapPageId, permissions);
            if (heapPage == null){
                throw new DbException("null");
            } else {
                iterator = heapPage.iterator();
            }
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if(iterator == null){
                return false;
            }
            if(iterator.hasNext()){
                return true;
            } else {
                return nextPage();
            }
        }

        private boolean nextPage() throws DbException, TransactionAbortedException {
            while(true){
                num++;
                if (num >= numPages()){
                    return false;
                }
                HeapPageId heapPageId = new HeapPageId(getId(), num);
                HeapPage heapPage = (HeapPage) bufferPool.getPage(tid, heapPageId, permissions);
                if (heapPage == null){
                    continue;
                }
                iterator = heapPage.iterator();
                if (iterator.hasNext()) {
                    return true;
                }
            }
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if (iterator == null){
                throw new NoSuchElementException();
            }
            return iterator.next();
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            open();
        }

        @Override
        public void close() {
            iterator = null;
        }
    }


    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f the file that stores the on-disk backing store for this heap
     *          file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
        file = f;
        tupleDesc = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        return file.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return tupleDesc;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
        FileInputStream fileInputStream = null;
        HeapPage heapPage = null;
        int size = BufferPool.getPageSize();
        byte[] buff = new byte[size];
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek((long) pid.getPageNumber() * size);
            if (randomAccessFile.read(buff) == -1){
                return null;
            }
            heapPage = new HeapPage((HeapPageId) pid, buff);
            randomAccessFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return heapPage;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
        HeapPageId id = (HeapPageId) page.getId();
        int size = BufferPool.getPageSize();
        int pageNumber = id.getPageNumber();
        byte[] data = page.getPageData();
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.seek((long) pageNumber * size);
            randomAccessFile.write(data);
            randomAccessFile.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        return (int) (file.length() / BufferPool.getPageSize());
    }

    // see DbFile.java for javadocs
    public List<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        if(!getFile().canRead() || !getFile().canWrite()){
            throw new IOException();
        }
        List<Page> res = new ArrayList<>();
        HeapPage page;
        HeapPageId heapPageId;
        int i;
        for (i=0; i<numPages(); i++){
            heapPageId = new HeapPageId(getId(), i);
            page = (HeapPage) Database.getBufferPool().getPage(tid, heapPageId, Permissions.READ_ONLY);
            if (page == null || page.getNumUnusedSlots() == 0){
                Database.getBufferPool().getLockManager().releaseLock(tid, heapPageId);
                continue;
            }
            page.insertTuple(t);
            page.markDirty(true, tid);
            res.add(page);
            return res;
        }
        heapPageId = new HeapPageId(getId(), i);
        page = new HeapPage(heapPageId, HeapPage.createEmptyPageData());
        page.insertTuple(t);
        writePage(page);
        res.add(page);
        return res;
    }

    // see DbFile.java for javadocs
    public List<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        List<Page> res = new ArrayList<>();
        HeapPageId heapPageId = (HeapPageId) t.getRecordId().getPageId();
        HeapPage page = (HeapPage) Database.getBufferPool().getPage(tid, heapPageId, Permissions.READ_WRITE);
        if (page == null){
            throw new DbException("No target page");
        }
        page.deleteTuple(t);
        res.add(page);
        return res;
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new MyIterator(tid, Permissions.READ_ONLY);
    }

}

