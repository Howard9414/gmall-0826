package utils

import java.io.InputStreamReader
import java.util.Properties

/**
 * @author Howard
 * @create 2020-02-19-6:40 下午
 */
object PropertiesUtil {
  def load(propertieName:String): Properties ={
    val prop=new Properties();
    prop.load(new InputStreamReader(Thread.currentThread().getContextClassLoader.getResourceAsStream(propertieName) , "UTF-8"))
    prop
  }
}
