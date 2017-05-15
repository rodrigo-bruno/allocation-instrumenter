import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Date;

public class Test3 {

    public static void main(String[] args) throws Exception {
        List<int[]> list = new LinkedList();
        for (int i = 0; i < 1024; i++) { list.add(new int[1024*512]); }
    }
}
