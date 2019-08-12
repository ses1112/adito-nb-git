package de.adito.git.nbm.icon;

import de.adito.git.gui.icon.IIconLoader;
import org.jetbrains.annotations.*;
import org.openide.util.ImageUtilities;

import javax.swing.*;
import java.util.HashMap;

/**
 * IconLoader Implementation backed up by NetBeans ImageUtilities.
 *
 * @author m.haertel, 06.08.2019
 */
public class NBIconLoader implements IIconLoader
{
  private final static HashMap<String, ImageIcon> iconCache = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public ImageIcon getIcon(String pIconBase)
  {
    if (iconCache.containsKey(pIconBase))
    {
      return iconCache.get(pIconBase);
    }
    else
    {
      ImageIcon icon = ImageUtilities.loadImageIcon(pIconBase, true);
      iconCache.put(pIconBase, icon);
      return icon;
    }
  }
}
