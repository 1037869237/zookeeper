package com.itcast.example;

import com.itcast.watcher.ZKConnectionWatcher;
import org.apache.zookeeper.*;

import java.util.concurrent.CountDownLatch;

/**
 * 分布式场景下：用zookeeper生成唯一id
 * 分布式唯一id：在过去的单库单表型系统中，通常可以使用数据库字段自带的auto_increment 属性来自动为每条记录生成一个唯一的ID。
 * 但是分库分表后，就无法在依靠数据库的 auto_increment属性来唯一标识一条记录了。此时我们就可以用zookeeper在分布式环 境
 * 下生成全局唯一ID。
 *
 * 用zookeeper生成分布式唯一id思想：
 * 1.连接zookeeper服务器
 * 2.制定路径下生成一个临时有序节点
 * 3.取序列号及为分布式环境下的唯一id
 */
public class GloballyUniqueld implements Watcher {

    //  zk的连接串
    String IP = "192.168.31.119:2181";
    //  计数器对象
    CountDownLatch countDownLatch = new CountDownLatch(1);
    //  用户生成序号的节点
    String defaultPath = "/uniqueId";
    //  连接对象
    ZooKeeper zooKeeper;
    @Override
    public void process(WatchedEvent event) {
        try {
            // 捕获事件状态
            if (event.getType() == Watcher.Event.EventType.None) {
                if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    System.out.println("连接成功");
                    countDownLatch.countDown();
                } else if (event.getState() == Watcher.Event.KeeperState.Disconnected) {
                    System.out.println("连接断开!");
                } else if (event.getState() == Watcher.Event.KeeperState.Expired) {
                    System.out.println("连接超时!");
                    // 超时后服务器端已经将连接释放，需要重新连接服务器端
                    zooKeeper = new ZooKeeper(IP, 6000,
                            new ZKConnectionWatcher());
                } else if (event.getState() == Watcher.Event.KeeperState.AuthFailed) {
                    System.out.println("验证失败!");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // 构造方法
    public GloballyUniqueld() {
        try {
            //打开连接
            zooKeeper = new ZooKeeper(IP, 5000, this);
            // 阻塞线程，等待连接的创建成功
            countDownLatch.await();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    // 生成id的方法
    public String getUniqueId() {
        String path = "";
        try {
            //创建临时有序节点
            path = zooKeeper.create(defaultPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        // /uniqueId0000000001
        return path.substring(9);
    }

    public static void main(String[] args) {
        GloballyUniqueld globallyUniqueId = new GloballyUniqueld();
        for (int i = 1; i <= 5; i++) {
            String id = globallyUniqueId.getUniqueId();
            System.out.println(id);
        }
    }

}
