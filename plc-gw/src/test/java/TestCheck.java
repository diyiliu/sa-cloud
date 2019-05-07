import com.diyiliu.plugin.util.CommonUtil;
import org.junit.Test;

/**
 * Description: TestCheck
 * Author: DIYILIU
 * Update: 2019-05-06 17:22
 */
public class TestCheck {

    @Test
    public void test(){
        String str = "02030E0005000000050000000500000005";

        str = CommonUtil.bytesToStr(CommonUtil.checkCRC(CommonUtil.hexStringToBytes(str)));
        System.out.println(str);
    }


    @Test
    public void test1(){
        String str = "123|abc";

        System.out.println(str.split("\\|")[1]);
    }
}
