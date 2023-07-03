package simpledb.transaction;

import simpledb.common.Permissions;
import simpledb.storage.PageId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName LockManager
 * @Description LockManager
 * @Author jtao
 * @Date 2023/7/3 5:50 下午
 **/

public class LockManager {

    private Map<Integer, List<Lock>> map;

    public LockManager(){
        this.map = new ConcurrentHashMap<>();
    }

    public synchronized Boolean acquireLock(TransactionId tid, PageId pageId, Permissions permissions){
        Integer pid = pageId.getPageNumber();
        Lock lock = new Lock(permissions, tid);
        List<Lock> lockList = map.get(pid);
        if(lockList == null){
            lockList = new ArrayList<>();
            lockList.add(lock);
            map.put(pid, lockList);
            return true;
        }
        // 只有一个事务占用锁
        if(lockList.size() == 1){
            Lock firstLock = lockList.get(0);
            if(firstLock.getTransactionId().equals(tid)){
                if(firstLock.getPermissions().equals(Permissions.READ_ONLY) && lock.getPermissions().equals(Permissions.READ_WRITE)){
                    // 升级锁
                    firstLock.setPermissions(Permissions.READ_WRITE);
                }
                return true;
            } else {
                if(firstLock.getPermissions().equals(Permissions.READ_ONLY) && lock.getPermissions().equals(Permissions.READ_ONLY)){
                    lockList.add(lock);
                    return true;
                }
                return false;
            }
        }
        //有多个事务则说明lockList中均为共享锁
        if(lock.getPermissions().equals(Permissions.READ_WRITE)){
            return false;
        }
        //同一个事务重复获取读锁
        for (Lock lock1 : lockList){
            if(lock1.getTransactionId().equals(tid)){
                return true;
            }
        }
        lockList.add(lock);
        return true;
    }

    public synchronized void releaseLock(TransactionId tid, PageId pageId){
        List<Lock> lockList = map.get(pageId.getPageNumber());
        for (int i=0; i<lockList.size(); i++){
            Lock lock = lockList.get(i);
            if(lock.getTransactionId().equals(tid)){
                lockList.remove(lock);
                if(lockList.size() == 0){
                    map.remove(pageId.getPageNumber());
                }
                return;
            }
        }
    }

    public synchronized void releaseAllLock(TransactionId tid){
        for (Integer k : map.keySet()){
            List<Lock> lockList = map.get(k);
            for (int i=0; i<lockList.size(); i++){
                Lock lock = lockList.get(i);
                if(lock.getTransactionId().equals(tid)){
                    lockList.remove(lock);
                    if(lockList.size() == 0){
                        map.remove(k);
                    }
                    break;
                }
            }
        }
    }

    public synchronized Boolean holdLock(TransactionId tid, PageId pageId){
        List<Lock> lockList = map.get(pageId.getPageNumber());
        for (int i=0; i<lockList.size(); i++){
            Lock lock = lockList.get(i);
            if(lock.getTransactionId().equals(tid)){
                return true;
            }
        }
        return false;
    }

}
