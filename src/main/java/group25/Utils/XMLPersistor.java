package group25.Utils;

import com.thoughtworks.xstream.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;

import static group25.Utils.AnsiColors.RED;

/**
 * Contains utils for creating XStream objects, writing java objects
 * to XML files and reading them from XML files.
 */

public class XMLPersistor {

    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private XStream xstream;

    public XMLPersistor(Class classToWrite) {
        XStream xstream = new XStream();
        XStream.setupDefaultSecurity(xstream);
        xstream.allowTypes(new Class[] {
                HashMap.class,
                String.class,
                classToWrite
        });
        this.xstream = xstream;
    }

    public boolean writeObject(Object obj, String filename) {
        String objAsXML = this.xstream.toXML(obj);

        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write("<?xml version=\"1.0\">\n".getBytes("UTF-8")); // XStream doesn't do this for you
            fos.write(objAsXML.getBytes("UTF-8"));
        } catch (Exception e) {
            System.out.println(RED.colorString("XMLPersistor.writeObject() exception: ")+e.getMessage());
            System.exit(1);
        }

        return true;
    }

    public <T> T readObject(String filename) {
        String str = null;

        try {
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];

            fis.read(data);
            str = new String(data, "UTF-8");
        } catch (Exception e) {
            System.out.println(RED.colorString("XMLPersistor.readObject() exception: ")+e.getMessage());
            System.exit(1);
        }

        return (T)this.xstream.fromXML(str);
    }
}