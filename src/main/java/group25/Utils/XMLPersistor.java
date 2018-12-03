package group25.Utils;

import com.thoughtworks.xstream.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;

import static group25.Utils.AnsiColors.RED;
import group25.Server.Common.*;
/**
 * Contains utils for creating XStream objects, writing java objects
 * to XML files and reading them from XML files.
 */

public class XMLPersistor {

    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private final String DATA_FILE_PATH = (System.getProperty("user.home")) + "/comp512/DistributedSystemsProject/Data";

    private XStream xstream;

    public XMLPersistor() {
        XStream xstream = new XStream();
        XStream.setupDefaultSecurity(xstream);
        xstream.allowTypes(new Class[] {
                HashMap.class,
                String.class,
                AbstractRMHashMapManager.class,
                Flight.class,
                Room.class,
                Car.class,
                Customer.class,
                ReservedItem.class,
        });
        this.xstream = xstream;
    }

    public boolean writeObject(Object obj, String filename) {
        FileOutputStream fos = null;
        File dataFile = new File(DATA_FILE_PATH+"/" + filename);
        try {
            dataFile.getParentFile().mkdir();
            dataFile.createNewFile();
            String objAsXML = this.xstream.toXML(obj);
            fos = new FileOutputStream(dataFile);
            fos.write("<?xml version=\"1.0\"?>\n".getBytes("UTF-8")); // XStream doesn't do this for you
            byte[] data = objAsXML.getBytes("UTF-8");
            fos.write(data);
            System.out.println("finished writing " + data);
        } catch (Exception e) {
            System.out.println(RED.colorString("XMLPersistor.writeObject() exception: ")+e.getMessage());
            System.exit(1);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    public <T> T readObject(String filepath) {
        String str = null;

        try {
            File file = new File(DATA_FILE_PATH+"/"+filepath);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];

            fis.read(data);
            str = new String(data, "UTF-8");
        } catch (Exception e) {
            return null; // file not found
        }

        return (T)this.xstream.fromXML(str);
    }
    
    public ObjectOutputStream getWriteAppendStream(String filepath) {
        File dataFile = new File(DATA_FILE_PATH+"/"+filepath);
        ObjectOutputStream oos = null;
        try {
            dataFile.getParentFile().mkdir();
            dataFile.createNewFile();
            oos = xstream.createObjectOutputStream(new FileOutputStream(dataFile));
            return oos;
        } catch (Exception e) {
            System.out.println(RED.colorString("XMLPersistor.getWriteAppendStream() exception: ")+e.getMessage());
            System.exit(1);
        }
        return null;  
    }

    public ObjectInputStream getReadAppendStream(String filepath) {
        File dataFile = new File(DATA_FILE_PATH+"/"+filepath);
        try {
            dataFile.getParentFile().mkdir();
            if (!dataFile.exists()) {
                System.out.println(RED.colorString("")+"");
                System.exit(1);
            }
            ObjectInputStream ois = xstream.createObjectInputStream(new FileInputStream(dataFile));
            return ois;
        } catch (Exception e) {
            System.out.println(RED.colorString("ERROR: ")+"XMLPersistor.getReadAppendStream() exception: "+e.getMessage());
            System.exit(1);
        }
        return null;
    }
}