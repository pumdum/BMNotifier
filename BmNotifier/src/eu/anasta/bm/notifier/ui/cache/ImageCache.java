package eu.anasta.bm.notifier.ui.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * Class for caching images
 * 
 * @author Emil
 */
public class ImageCache {

    private static HashMap<Object, Image> _ImageMap;
    public static String MAIL ="mail.png";
    public static String LITLLE_MAIL ="16/mail.png";
    public static String LITLLE_BM ="16/bm.png";
    public static String LITLLE_ERROR ="16/error.png";
    public static String LITLLE_WARN ="16/warn.png";
    public static String lITLLE_ONLINE ="16/online.png";
    public static String lITLLE_OFFLINE ="16/offline.png";
    public static String lITLLE_AWAY ="16/away.png";
    
    public static int BM_STATUS_DISCONECT = 	00000001;
    public static int BM_STATUS_WARN = 	00000010;
    public static int MAIL_UNREAD = 	00000100;
    public static int IM_ONLINE = 		00001000;
    public static int IM_OFFLINE = 		00010000;
    public static int IM_AWAY = 		00100000;

    // what path to get to the "icons" directory without actually including it
    private static final String           ICON_ROOT_PATH = "/";

    static {
        _ImageMap = new HashMap<Object, Image>();
    }
    

    
    /**
     * Returns an image that is also cached if it has to be created and does not already exist in the cache.
     * 
     * @param fileName Filename of image to fetch
     * @return Image null if it could not be found
     */
    public static Image getImage(String fileName) {
    	fileName = ICON_ROOT_PATH + fileName;
        Image image = _ImageMap.get(fileName);
        if (image == null) {
            image = createImage(fileName);
            _ImageMap.put(fileName, image);
        }
        return image;
    }
    
    public static Image getImage(int selection) {
    	Image image = _ImageMap.get(selection);
        if (image == null) {
        	image = new Image(Display.getDefault(), new Rectangle(0, 0, 32, 32));
        	GC gc =  new GC(image);
        	gc.drawImage(getImage(LITLLE_BM), 0, 0);
        	if ( (selection & MAIL_UNREAD) != 0){
        		gc.drawImage(getImage(LITLLE_MAIL), 16, -2);	
        	}
        	if ( (selection & BM_STATUS_DISCONECT) != 0){
        		gc.drawImage(getImage(LITLLE_ERROR), -2, 0);	
        	}
        	if ( (selection & BM_STATUS_WARN) != 0){
        		gc.drawImage(getImage(LITLLE_WARN), 0, 0);	
        	}
        	if ( (selection & IM_ONLINE) != 0){
        		gc.drawImage(getImage(lITLLE_ONLINE), 8, 9);	
        	}
        	if ( (selection & IM_OFFLINE) != 0){
        		gc.drawImage(getImage(lITLLE_OFFLINE), 8, 9);	
        	}
        	if ( (selection & IM_AWAY) != 0){
        		gc.drawImage(getImage(lITLLE_AWAY),	8, 9);	
        	}
            _ImageMap.put(selection, image);
            gc.dispose();
        }
        return image;
    }


    // creates the image, and tries really hard to do so
    private static Image createImage(String fileName) {
        ClassLoader classLoader = ImageCache.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream(fileName);
        if (is == null) {
            // the old way didn't have leading slash, so if we can't find the image stream,
            // let's see if the old way works.
            is = classLoader.getResourceAsStream(fileName.substring(1));

            if (is == null) {
                is = classLoader.getResourceAsStream(fileName);
                if (is == null) {
                    is = classLoader.getResourceAsStream(fileName.substring(1));
                    if (is == null) { return null; }
                }
            }
        }

        Image img = new Image(Display.getDefault(), is);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return img;
    }

    /**
     * Disposes ALL images that have been cached.
     */
    public static void dispose() {
        Iterator<Image> e = _ImageMap.values().iterator();
        while (e.hasNext())
            e.next().dispose();

    }
}
