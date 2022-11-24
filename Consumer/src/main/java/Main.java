import Configurations.PropertiesCache;
import Threads.ConsumerThread;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;


public class Main {

  public static void main(String[] args) throws IOException, TimeoutException {

    // the number of threads on the consumer
    final int NUM_THREADS = 200;

    // name of the message queue
    final String QUEUE_NAME = "post_requests";

    // gets the connection to the rabbit mq
    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername(PropertiesCache.getInstance().getProperty("username"));
    factory.setPassword(PropertiesCache.getInstance().getProperty("password"));
    factory.setHost(PropertiesCache.getInstance().getProperty("hostname"));
    Connection connection = factory.newConnection();

    // get connection to redis on ec2 instance
    String hostname = PropertiesCache.getInstance().getProperty("redis-hostname");
    String password = PropertiesCache.getInstance().getProperty("redis-password");
    Integer port = Integer.valueOf(PropertiesCache.getInstance().getProperty("redis-port"));




    // concurrent hashmap - shared resource
    // TODO: Remove this from the consumer program because will be using an in memory database
    ConcurrentHashMap<Integer, ArrayList<JSONObject>> skiersStorage = new ConcurrentHashMap<>();

    // creating a thread pool of consumer threads
    ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);

    // creating each consumer thread
    // TODO: Remove skiersStorage
    for (int i = 0; i < NUM_THREADS; i++) {
      threadPool.execute(new ConsumerThread(connection, skiersStorage, QUEUE_NAME));
    }
    try {
      if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
        threadPool.shutdownNow();
        if (!threadPool.awaitTermination(60, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      threadPool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

}
