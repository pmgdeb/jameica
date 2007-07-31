/**********************************************************************
 * $Source: /cvsroot/jameica/jameica/src/de/willuhn/jameica/gui/input/SearchInput.java,v $
 * $Revision: 1.3 $
 * $Date: 2007/07/31 14:32:48 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/
package de.willuhn.jameica.gui.input;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.PopupList;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import de.willuhn.datasource.BeanUtil;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.util.Color;
import de.willuhn.jameica.gui.util.DelayedListener;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

/**
 * Erzeugt eine Such-Box, in der man Text eingaben kann.
 * Kann prima zur Erstellung eines Suchfeldes genutzt werden,
 * welches bei jeder Eingabe eines Zeichens eine Liste mit
 * Vorschlaegen anzeigen kann.
 * @author willuhn
 */
public class SearchInput extends AbstractInput
{
  // Fachdaten
  private String attribute    = null;
  private Object value        = null;
  
  // SWT-Daten
  private Text text           = null;
  private boolean enabled     = true;
  private String search       = null;


  /**
   * Erzeugt eine neue Such-Box.
   */
  public SearchInput()
  {
    super();
    this.search = Application.getI18n().tr("Suche...");
  }

  /**
   * Legt den Namen des Attributes fest, welches von den Objekten angezeigt werden
   * soll. Bei herkoemmlichen Beans wird also ein Getter mit diesem Namen aufgerufen. 
   * Wird kein Attribut angegeben, wird bei Objekten des Typs <code>GenericObject</code>
   * der Wert des Primaer-Attributes angezeigt, andernfalls der Wert von <code>toString()</code>.
   * @param name Name des anzuzeigenden Attributes (muss im GenericObject
   * via getAttribute(String) abrufbar sein).
   */
  public void setAttribute(String name)
	{
		if (name != null)
			this.attribute = name;
	}
  
  private boolean inSearch = false;

  /**
   * Ersetzt alle Elemente der Selectbox gegen die aus der uebergebenen Liste.
   * @param list
   */
  private void setList(List list)
  {
    if (inSearch || this.text == null || this.text.isDisposed())
      return;

    // Nichts gefunden
    if (list == null || list.size() == 0)
      return;

    boolean haveAttribute = this.attribute != null && this.attribute.length() > 0;

    try
    {
      // Liste von Strings fuer die Anzeige in der Popup-Box.

      ArrayList items  = new ArrayList();
      Hashtable values = new Hashtable();
      int size = list.size();
      for (int i=0;i<size;++i)
      {
        Object object = list.get(i);

        if (object == null)
          continue;

        // Anzuzeigenden Text ermitteln
        String text = null;
        if (haveAttribute)
        {
          Object value = BeanUtil.get(object,this.attribute);
          if (value == null)
            continue;
          
          text = value.toString();
          if (text == null)
            continue;
        }
        else
        {
          text = BeanUtil.toString(object);
        }
        items.add(text);
        values.put(text,object);
      }

      Point location = this.text.toDisplay(this.text.getLocation());
      Rectangle rect = this.text.getClientArea();

      PopupList popup = new PopupList(GUI.getShell());
      popup.setItems((String[])items.toArray(new String[items.size()]));
      String selected = popup.open(new Rectangle(location.x, rect.y + location.y + rect.height, rect.width, 0));

      // Jetzt muessen wir das zugehoerige Fachobjekt suchen
      // geht leider nicht anders, weil wir von der PopupList
      // keine Position kriegen sondern nur den Text.
      if (selected != null)
      {
        // Das "setText" loest eine erneute Suche aus. Daher
        // ueberpringen wir die naechste
        this.inSearch = true;
        this.value = values.get(selected);
        
        // Bei einem einfachen setText() landet der Cursor leider
        // am Anfang des Textes und kann auch nicht nach hinten
        // bewegt werden
        this.text.setText("");
        this.text.insert(selected);
      }
    }
    catch (RemoteException e)
    {
      Logger.error("unable to create combo box",e);
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(Application.getI18n().tr("Fehler beim Laden der Daten..."),StatusBarMessage.TYPE_ERROR));
    }
  }

  /**
   * Diese Funktion sollte ueberschrieben werden, wenn die Liste
   * der Vorschlaege bei Eingabe von Suchbegriffen aktualisiert werden soll.
   * Die Standardimplementierung macht schlicht keine Suche sondern
   * laesst alles, wie es ist.
   * @param text der momentan eingegebene Suchtext.
   * @return eine neue Liste mit den als Suchvorschlaegen anzuzeigenden Objekten.
   * Die Funktion kann sowohl null als auch eine leere Liste zurueckgeben,
   * wenn nichts gefunden wurde.
   */
  public List startSearch(String text)
  {
    return null;
  }
  
  /**
   * @see de.willuhn.jameica.gui.input.Input#getControl()
   */
  public Control getControl()
  {

    this.text = GUI.getStyleFactory().createText(getParent());
    this.text.setText(this.value == null ? search : toString(this.value));

    // "Suche..." grau einfaerben
    if (this.value == null || !enabled)
      this.text.setForeground(Color.COMMENT.getSWTColor());
    
    this.text.setEnabled(enabled);
    this.text.setEditable(enabled);

    this.text.addFocusListener(new FocusListener() {
    
      public void focusLost(FocusEvent e)
      {
        if (text == null || text.isDisposed())
          return;

        String s = text.getText();
        if (s == null || s.length() == 0)
        {
          text.setText(search);
          text.setForeground(Color.COMMENT.getSWTColor());
        }
      }
    
      public void focusGained(FocusEvent e)
      {
        if (text == null || text.isDisposed())
          return;

        String s = text.getText();
        if (s != null && s.equals(search))
        {
          text.setText("");
          text.setForeground(Color.WIDGET_FG.getSWTColor());
        }
      }
    
    });

    // Loest die Suche aus
    Listener listener = new Listener() {
      private String oldText = null;
      public void handleEvent(Event event)
      {
        if (text == null || text.isDisposed())
          return;

        String newText = text.getText();
        
        if (inSearch)
        {
          // TODO Das Updateverhalten ist noch nicht optimal.
          inSearch = false;
          return;
        }

        if (newText == null || newText.length() == 0)
          return; // Kein Suchbegriff - keine Suche

        if (newText.equals(search))
          return; // Nach "Suche..." suchen wir nicht

        if (newText == oldText || newText.equals(oldText))
          return; // Text wurde nicht geaendert

        text.setForeground(Color.WIDGET_FG.getSWTColor());

        oldText = newText;
        List newList = startSearch(newText);
        setList(newList);
      }
    
    };
    this.text.addListener(SWT.KeyUp, new DelayedListener(360,listener));

    return this.text;
  }

  /**
   * Liefert das aktuelle Objekt.
   * Das ist entweder das ausgewaehlte aus der letzten Suche oder das
   * initial uebergebene.
   * @see de.willuhn.jameica.gui.input.Input#getValue()
   */
  public Object getValue()
  {
    if (this.text == null || this.text.isDisposed())
      return this.value;
    String s = text.getText();
    if (s == null || s.length() == 0 || s.equals(this.search))
      return null;
    return value;
  }

	/**
	 * Liefert den derzeit angezeigten Text zurueck.
   * @return Text.
   */
  public String getText()
	{
    if (this.text == null || this.text.isDisposed())
      return null;
		return text.getText();
	}

  /**
   * @see de.willuhn.jameica.gui.input.Input#focus()
   */
  public void focus()
  {
    if (this.text == null || this.text.isDisposed())
      return;
    
    text.setFocus();
  }


  /**
   * @see de.willuhn.jameica.gui.input.Input#disable()
   */
  public void disable()
  {
    setEnabled(false);
  }

  /**
   * @see de.willuhn.jameica.gui.input.Input#enable()
   */
  public void enable()
  {
    setEnabled(true);
  }

  /**
   * @see de.willuhn.jameica.gui.input.Input#setEnabled(boolean)
   */
  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
    if (text != null && !text.isDisposed())
    {
      text.setEnabled(enabled);
      if (enabled)
        text.setForeground(Color.WIDGET_FG.getSWTColor());
      else
        text.setForeground(Color.COMMENT.getSWTColor());
    }
  }

  /**
   * @see de.willuhn.jameica.gui.input.Input#setValue(java.lang.Object)
   */
  public void setValue(Object o)
  {
    this.value = o;
    
    if (this.text != null && !this.text.isDisposed())
      this.text.setText(toString(this.value));
  }
  
  /**
   * Liefert eine String-Repraesentation des uebergebenen Objektes.
   * @param value Objekt.
   * @return String - nie null.
   */
  private String toString(Object value)
  {
    if (value == null)
      return "";

    try
    {
      if (attribute != null)
      {
        Object o = BeanUtil.get(value,attribute);
        return o == null ? "" : o.toString();
      }
      return BeanUtil.toString(value);
    }
    catch (Exception e)
    {
      Logger.error("unable to apply value",e);
    }
    return "";
  }

  /**
   * @see de.willuhn.jameica.gui.input.Input#isEnabled()
   */
  public boolean isEnabled()
  {
    return enabled;
  }
}

/*********************************************************************
 * $Log: SearchInput.java,v $
 * Revision 1.3  2007/07/31 14:32:48  willuhn
 * @N Neues Custom-Widget zum Suchen. Ist ein Textfeld, welches nach Eingabe einen Callback-Mechanismus fuer die Suche startet und die Ergebnisse dann in einer Dropdown-Box anbietet. Also ein Mix aus TextInput und dynamischem SelectInput
 *
 **********************************************************************/