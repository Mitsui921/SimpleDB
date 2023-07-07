package simpledb.execution;

import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.storage.Field;
import simpledb.storage.IntField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;

import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * @ClassName AggregateIter
 * @Description AggregateIter
 * @Author jtao
 * @Date 2023/7/7 11:34 上午
 **/

public class AggregateIter implements OpIterator{

    private Iterator<Tuple> tupleIterator;
    private List<Tuple> resultSet;
    private Map<Field, List<Field>> group;
    private Aggregator.Op what;
    private Type gbfieldType;
    private TupleDesc tupleDesc;
    private int gbfield;

    public AggregateIter(Map<Field, List<Field>> group, int gbfield, Type gbfieldType, Aggregator.Op what){
        this.group = group;
        this.gbfieldType = gbfieldType;
        this.what = what;
        if (gbfield != -1){
            Type[] types = new Type[2];
            types[0] = gbfieldType;
            types[1] = Type.INT_TYPE;
            tupleDesc = new TupleDesc(types);
        } else {
            Type[] types = new Type[1];
            types[0] = Type.INT_TYPE;
            tupleDesc = new TupleDesc(types);
        }
    }

    @Override
    public void open() throws DbException, TransactionAbortedException {
        resultSet = new ArrayList<>();
        if (what == Aggregator.Op.COUNT){
            for (Field field : group.keySet()){
                int count = 0;
                Tuple tuple = new Tuple(tupleDesc);
                if (field != null){
                    tuple.setField(0, field);
                    tuple.setField(1, new IntField(group.get(field).size()));
                } else {
                    tuple.setField(0, new IntField(group.get(field).size()));
                }
                resultSet.add(tuple);
            }
        } else if (what == Aggregator.Op.MIN){
            for (Field field : group.keySet()){
                int min = Integer.MAX_VALUE;
                Tuple tuple = new Tuple(tupleDesc);
                tuple.setField(0, field);
                for (int i=0; i<group.get(field).size(); i++){
                    IntField field1 = (IntField) group.get(field).get(i);
                    if (field1.getValue() < min){
                        min = field1.getValue();
                    }
                }
                if (field != null){
                    tuple.setField(1, new IntField(min));
                } else {
                    tuple.setField(0, new IntField(min));
                }
                resultSet.add(tuple);
            }
        } else if (what == Aggregator.Op.MAX){
            for (Field field : group.keySet()){
                int max = Integer.MIN_VALUE;
                Tuple tuple = new Tuple(tupleDesc);
                tuple.setField(0, field);
                for (int i=0; i<group.get(field).size(); i++){
                    IntField field1 = (IntField) group.get(field).get(i);
                    if (field1.getValue() > max){
                        max = field1.getValue();
                    }
                }
                if(field != null){
                    tuple.setField(1, new IntField(max));
                } else {
                    tuple.setField(0, new IntField(max));
                }
                resultSet.add(tuple);
            }
        } else if (what == Aggregator.Op.AVG){
            for (Field field : group.keySet()){
                int sum = 0;
                Tuple tuple = new Tuple(tupleDesc);
                tuple.setField(0, field);
                for (int i=0; i<group.get(field).size(); i++){
                    IntField field1 = (IntField) group.get(field).get(i);
                    sum += field1.getValue();
                }
                if(field != null){
                    tuple.setField(1, new IntField(sum / group.get(field).size()));
                } else {
                    tuple.setField(0, new IntField(sum / group.get(field).size()));
                }
                resultSet.add(tuple);
            }
        } else if (what == Aggregator.Op.SUM){
            for (Field field : group.keySet()){
                int sum = 0;
                Tuple tuple = new Tuple(tupleDesc);
                tuple.setField(0, field);
                for (int i=0; i<group.get(field).size(); i++){
                    IntField field1 = (IntField) group.get(field).get(i);
                    sum += field1.getValue();
                }
                if(field != null){
                    tuple.setField(1, new IntField(sum));
                } else {
                    tuple.setField(0, new IntField(sum));
                }
                resultSet.add(tuple);
            }
        }
        tupleIterator = resultSet.iterator();
    }

    @Override
    public boolean hasNext() throws DbException, TransactionAbortedException {
        if (tupleIterator == null){
            return false;
        }
        return tupleIterator.hasNext();
    }

    @Override
    public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
        return tupleIterator.next();
    }

    @Override
    public void rewind() throws DbException, TransactionAbortedException {
        if(resultSet != null){
            tupleIterator = resultSet.iterator();
        }
    }

    @Override
    public TupleDesc getTupleDesc() {
        return tupleDesc;
    }

    @Override
    public void close() {
        tupleIterator = null;
    }
}
