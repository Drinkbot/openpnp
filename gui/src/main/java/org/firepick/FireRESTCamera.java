/*
   Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
   Copyright (C) 2014 Karl Lew <karl@firepick.org>
   
   This file is part of OpenPnP.
   
  OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
   
   For more information about OpenPnP visit http://openpnp.org
*/

package org.firepick;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.TableScannerCameraConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A FireREST Camera. 
 * http://github.com/firepick1/FireREST
 */
public class FireRESTCamera extends ReferenceCamera implements Runnable {
  private final static Logger logger = LoggerFactory.getLogger(FireRESTCamera.class);
  
  private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  
  @Element
  private String sourceUri;
  

  @Attribute(required=false)
  private int fps = 2;
  
  private URL sourceUrl;
  private CachedImage cachedImage;
  private BufferedImage buffer;
  private Thread thread;
  private File cacheDirectory;
  
  public FireRESTCamera() {
    unitsPerPixel = new Location(LengthUnit.Inches, 0.031, 0.031, 0, 0); 
    sourceUri = "http://firepick1.github.io/firerest/cv/1/camera.jpg";
  }
  
  @SuppressWarnings("unused")
  @Commit
  private void commit() throws Exception {
    setSourceUri(sourceUri);
  }
  
  @Override
  public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
    start();
    super.startContinuousCapture(listener, maximumFps);
  }
  
  @Override
  public synchronized void stopContinuousCapture(CameraListener listener) {
    super.stopContinuousCapture(listener);
    if (listeners.size() == 0) {
      stop();
    }
  }
  
  private synchronized void stop() {
    if (thread != null && thread.isAlive()) {
      thread.interrupt();
      try {
        thread.join();
      }
      catch (Exception e) {
        
      }
      thread = null;
    }
  }
  
  private synchronized void start() {
    if (thread == null) {
      thread = new Thread(this);
      thread.start();
    }
  }

  public String getSourceUri() {
    return sourceUri;
  }

  public void setSourceUri(String sourceUri) throws Exception {
    String oldValue = this.sourceUri;
    this.sourceUri = sourceUri;
    pcs.firePropertyChange("sourceUri", oldValue, sourceUri);
    // TODO: Move to start() so simply setting a property doesn't sometimes
    // blow up.
    initialize();
  }
  
  public String getCacheSizeDescription() {
    try {
      return FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(cacheDirectory));
    }
    catch (Exception e) {
      return "Not Initialized";
    }
  }
  
  public synchronized void clearCache() throws IOException {
    FileUtils.cleanDirectory(cacheDirectory);
    pcs.firePropertyChange("cacheSizeDescription", null, getCacheSizeDescription());
  }

  public void run() {
    while (!Thread.interrupted()) {
      BufferedImage frame = capture();
      broadcastCapture(frame);
      try {
        Thread.sleep(1000 / fps);
      }
      catch (InterruptedException e) {
        return;
      }
    }
  }
  
  @Override
  public BufferedImage capture() {
    if (buffer == null) {
      return null;
    }
    if (head == null) {
        // TODO: Render an error image saying that it must be attached
        // to a head.
      return null;
    }
    synchronized (buffer) {
      Location headXY = getLocation().convertToUnits(LengthUnit.Millimeters);
      
      renderBuffer();
      
      int width = cachedImage.getWidth();
      int height = cachedImage.getHeight();
      BufferedImage frame = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB);
      
      Graphics2D g = (Graphics2D) frame.getGraphics();
      g.drawImage( buffer, 0, 0, width, height, 0, 0, width, height, null);
      g.dispose();
      
      return frame;
    }
  }
  
  private void renderBuffer() {
    Graphics2D g = (Graphics2D) buffer.getGraphics();
    g.setColor(Color.black);
    g.clearRect(0, 0, buffer.getWidth(), buffer.getHeight());
    g.setColor(Color.white);

    BufferedImage image = cachedImage.getImage();

    int w = image.getWidth();
    int h = image.getHeight();
    g.drawImage (image, 0, 0, w, h, w, h, 0, 0, null);
    
    g.dispose();
  }
  
  private synchronized void initialize() throws Exception {
    stop();
    sourceUrl = new URL(sourceUri);
    cacheDirectory = new File(Configuration.get().getResourceDirectory(getClass()), DigestUtils.shaHex(sourceUri));
    if (!cacheDirectory.exists()) {
      cacheDirectory.mkdirs();
    }

    File imageFile = new File(cacheDirectory, "firerest.png");
    cachedImage = new CachedImage(sourceUrl, imageFile);
    cachedImage.getImage(); // get an image for WxH
    buffer = new BufferedImage( cachedImage.getWidth(), cachedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
    
    if (listeners.size() > 0) {
      start();
    }
  }
  
  @Override
  public Wizard getConfigurationWizard() {
    return new FireRESTCameraWizard(this);
  }
  
  public static class CachedImage {
    private File file;
    private SoftReference<BufferedImage> image;
    private URL imageURL;
    private width;
    private height;
    
    public CachedImage(URL imageURL, File file) {
      if (file == null) {
        throw new RuntimeException("CachedImage file cannot be null");
      }
      this.file = file;
      if (imageURL == null) {
        throw new RuntimeException("FireRESTCamera <source-uri> must be specified");
      }
      this.imageURL = imageURL;
    }
    
    public synchronized BufferedImage getImage() {
      if (imageURL != null) {
	try {
	  FileUtils.copyURLToFile(imageURL, file);
	}
	catch (Exception e) {
	  logger.error("Could not download image: {}", imageURL);
	  e.printStackTrace();
	}
      }
      try {
	image = new SoftReference<BufferedImage>(ImageIO.read(file));
	BufferedImage result = image.get();
	width = result.getWidth();
	height = result.getHeight();
	return result;
      }
      catch (Exception e) {
	throw new RuntimeException("Could not load cached image: {}", file);
      }
    }

    public int getWidth() {return width;}
    public int getHeight() {return height;}
    
    public String toString() {
      return imageURL.toString();
    }

  }
}
