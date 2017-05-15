import java.nio.ByteBuffer;                                                     
import java.util.ArrayList;                                                     
import java.util.LinkedList;                                                    
import java.util.List;                                                          
import java.util.Random;                                                        
import java.util.Date;                                                          
                                                                                
public class Test1 {                                                            

   private static ByteBuffer allocate() { return ByteBuffer.allocate(1024); }
                                                                                
    public static ByteBuffer[] list = new ByteBuffer[32*32*1024];                        
    public static void main(String[] args) throws Exception {                   
        for (int i = 0; i < 32; i++) {                                          
            for (int j = 0; j < 32 * 1024; j++) {                                      
                list[i*32 + j] = allocate();                
            }                                                                   
            Thread.sleep(1000);                                                 
        }                                                                       
    }
}
