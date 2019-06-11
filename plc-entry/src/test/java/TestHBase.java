import com.tiza.air.cluster.HBaseUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.junit.Test;

/**
 * Description: TestHBase
 * Author: DIYILIU
 * Update: 2019-06-11 09:09
 */
public class TestHBase {

    @Test
    public void test(){
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", "");
        config.set("hbase.zookeeper.property.clientPort", "2181");

        HBaseUtil hbaseUtil = new HBaseUtil();
        hbaseUtil.setConfig(config);
        hbaseUtil.setTable("tstar:TrashRawdata");


    }
}
