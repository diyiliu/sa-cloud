import com.diyiliu.plugin.util.CommonUtil;
import org.junit.Test;

/**
 * Description: TestMain
 * Author: DIYILIU
 * Update: 2019-05-07 17:00
 */
public class TestMain {

    @Test
    public void test(){
        String str = "3F07126F3F";
        byte[] bytes = CommonUtil.hexStringToBytes(str);

        int i = CommonUtil.byte2int(bytes);
        System.out.println(Float.intBitsToFloat(i));
    }


    @Test
    public void test1(){
        String str = "001F";
        byte[] bytes = CommonUtil.hexStringToBytes(str);
        System.out.println(CommonUtil.bytes2BinaryStr(bytes));
    }
}
