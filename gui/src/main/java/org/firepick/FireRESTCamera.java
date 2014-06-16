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

import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.core.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.net.URL;


/**
 * A FireREST Camera.
 * http://github.com/firepick1/FireREST
 */
public class FireRESTCamera extends ReferenceCamera implements Runnable {
  private final static Logger logger = LoggerFactory.getLogger(FireRESTCamera.class);
  private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  @Element
  private String sourceUri;
  @Attribute(required = false)
  private int fps = 2;
  private URL sourceUrl;
  private FireREST firerest = new FireREST();
  private Thread thread;

  public FireRESTCamera() {
    unitsPerPixel = new Location(LengthUnit.Inches, 0.031, 0.031, 0, 0);
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
        logger.error("Unexpected exception: {}", e.getMessage());
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
    return "FireRESTCamera does not use a cache";
  }

  public synchronized void clearCache() throws IOException {
    //FileUtils.cleanDirectory(cacheDirectory);
    pcs.firePropertyChange("cacheSizeDescription", null, getCacheSizeDescription());
  }

  public void run() {

    while (!Thread.interrupted()) {
      BufferedImage frame = capture();
      broadcastCapture(frame);
      try {
        Thread.sleep(1000 / fps);
      }
      catch (Throwable e) {
        logger.warn("FireRESTCamera thread END: {}", e.getMessage());
        return;
      }
    }
  }

  @Override
  public BufferedImage capture() {
    BufferedImage result = firerest.getImage(sourceUrl);
    return result;
  }

  private synchronized void initialize() throws Exception {
    stop();
    sourceUrl = new URL(sourceUri);

    if (listeners.size() > 0) {
      start();
    }
  }

  @Override
  public Wizard getConfigurationWizard() {
    return new FireRESTCameraWizard(this);
  }

}
