/*
 * This file is part of libbluray
 * Copyright (C) 2010  William Hahne
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library. If not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.videolan.media.content.playlist;

import java.awt.Component;
import java.util.LinkedList;

import javax.media.Control;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.media.MediaSelectListener;
import javax.tv.service.selection.InsufficientResourcesException;
import javax.tv.service.selection.InvalidServiceComponentException;

import org.bluray.media.AsynchronousPiPControl;
import org.bluray.net.BDLocator;
import org.dvb.media.DVBMediaSelectControl;
import org.videolan.PlaylistInfo;
import org.videolan.TIClip;

public class DVBMediaSelectControlImpl implements DVBMediaSelectControl{
    protected DVBMediaSelectControlImpl(Handler player) {
        this.player = player;
    }

    public void add(Locator component)
                throws InvalidLocatorException, InvalidServiceComponentException,
                InsufficientResourcesException, SecurityException {
        if (!checkLocator(component))
            throw new InvalidLocatorException(component);
        BDLocator locator = (BDLocator)component;
        if (locator.getSecondaryVideoStreamNumber() <= 0)
            throw new InvalidServiceComponentException(component);
        AsynchronousPiPControl control = (AsynchronousPiPControl)
            player.getControl("org.bluray.media.AsynchronousPiPControl");
        if (control.getCurrentStreamNumber() > 0)
            throw new InsufficientResourcesException();
        try {
            control.selectStreamNumber(locator.getSecondaryVideoStreamNumber());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void remove(Locator component)
                throws InvalidLocatorException, InvalidServiceComponentException,
                SecurityException {
        if (!checkLocator(component))
            throw new InvalidLocatorException(component);
        BDLocator locator = (BDLocator)component;
        if (locator.getSecondaryVideoStreamNumber() <= 0)
            throw new InvalidServiceComponentException(component);
        AsynchronousPiPControl control = (AsynchronousPiPControl)
            player.getControl("org.bluray.media.AsynchronousPiPControl");
        if (control.getCurrentStreamNumber() != locator.getSecondaryVideoStreamNumber())
            throw new InvalidLocatorException(component);
        control.stop();
    }

    public void select(Locator component)
                throws InvalidLocatorException, InvalidServiceComponentException,
            InsufficientResourcesException, SecurityException {
        if (!checkLocator(component))
            throw new InvalidLocatorException(component);
        Control control = getControl((BDLocator)component);
        if (control == null)
            throw new InvalidLocatorException(component);
        if (!(control instanceof StreamControl))
            return;
        int stream = getStream((BDLocator)component);
        if (stream <= 0)
            throw new InvalidLocatorException(component);
        try {
            ((StreamControl)control).selectStreamNumber(stream);
        } catch (Exception e) {
            throw new InvalidLocatorException(component);
        }
    }

    public void select(Locator[] components)
                throws InvalidLocatorException, InvalidServiceComponentException,
                InsufficientResourcesException, SecurityException {
        for (int i = 0; i < components.length; i++)
            select(components[i]);
    }

    public void replace(Locator fromComponent, Locator toComponent)
               throws InvalidLocatorException, InvalidServiceComponentException,
               InsufficientResourcesException, SecurityException {
        if (!checkLocator(fromComponent))
            throw new InvalidLocatorException(fromComponent);
        if (!checkLocator(toComponent))
            throw new InvalidLocatorException(toComponent);
        Control fromControl = getControl((BDLocator)fromComponent);
        if (fromControl == null)
            throw new InvalidLocatorException(fromComponent);
        Control toControl = getControl((BDLocator)toComponent);
        if (toControl == null)
            throw new InvalidLocatorException(toComponent);
        if (fromControl != toControl)
            throw new InvalidLocatorException(fromComponent);
        int fromStream = getStream((BDLocator)fromComponent);
        if ((fromStream <= 0) || (fromStream != ((StreamControl)fromControl).getCurrentStreamNumber()))
            throw new InvalidLocatorException(fromComponent);
        int toStream = getStream((BDLocator)toComponent);
        if (toStream <= 0)
            throw new InvalidLocatorException(toComponent);
        try {
            ((StreamControl)fromControl).selectStreamNumber(toStream);
        } catch (Exception e) {
            throw new InvalidLocatorException(toComponent);
        }
    }

    public void addMediaSelectListener(MediaSelectListener listener) {
        org.videolan.Logger.unimplemented("DVBMediaSelectControlImpl", "addMediaSelectListener");
        // callback is never triggered ?
        synchronized(listeners) {
            listeners.add(listener);
        }
    }

    public void removeMediaSelectListener(MediaSelectListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }

    public Locator[] getCurrentSelection() {
        BDLocator locator = player.getLocator();
        if (locator == null)
            return new Locator[0];
        String[] tags = locator.getComponentTags();
        if (tags.length <= 0)
            return new Locator[0];
        Locator[] selections = new Locator[tags.length];
        String[] tag = new String[1];
        for (int i = 0; i < tags.length; i++) {
            tag[0] = tags[i];
            try {
                selections[i] = new BDLocator(
                                              locator.getDiscId(),
                                              locator.getTitleNumber(),
                                              locator.getPlayListId(),
                                              locator.getPlayItemId(),
                                              locator.getMarkId(),
                                              tag);
            } catch (Exception e) {
                e.printStackTrace();
                return new Locator[0];
            }
        }
        return selections;
    }

    public void selectServiceMediaComponents(Locator locator)
            throws InvalidLocatorException, InvalidServiceComponentException,
            InsufficientResourcesException {
        if (!(locator instanceof BDLocator))
            throw new InvalidLocatorException(locator);
        PlaylistInfo pi = player.getPlaylistInfo();
        if (pi == null)
            throw new  InsufficientResourcesException();
        if (((BDLocator)locator).getPlayListId() != pi.getPlaylist())
            throw new InvalidLocatorException(locator);
        TIClip ci = player. getCurrentClipInfo();
        if (ci == null)
            throw new  InsufficientResourcesException();
        String[] tags = ((BDLocator)locator).getComponentTags();
        String[] tag = new String[1];
        for (int i = 0; i < tags.length; i++) {
            tag[0] = tags[i];
            try {
                select(new BDLocator(
                                     null,
                                     -1,
                                     pi.getPlaylist(),
                                     ci.getIndex(),
                                     -1,
                                     tag));
            } catch (org.davic.net.InvalidLocatorException e) {
                e.printStackTrace();
                throw new InvalidLocatorException(locator);
            } catch (SecurityException e) {
                e.printStackTrace();
                throw new InsufficientResourcesException();
            }
        }
    }

    public Component getControlComponent() {
        return null;
    }

    private boolean checkLocator(Locator locator) {
        if ((locator == null) || !(locator instanceof BDLocator))
            return false;
        PlaylistInfo pi = player.getPlaylistInfo();
        if (pi == null)
            return false;
        BDLocator bdLocator = (BDLocator)locator;
        if (bdLocator.getComponentTagsCount() != 1)
            return false;
        if (bdLocator.getPlayListId() != pi.getPlaylist())
            return false;
        TIClip ci = player. getCurrentClipInfo();
        if (ci == null)
            return false;
        return (bdLocator.getPlayItemId() == -1) || (bdLocator.getPlayItemId() == ci.getIndex());
    }

    private int getStream(BDLocator locator) {
        if (locator.getPrimaryVideoStreamNumber() > 0)
            return locator.getPrimaryVideoStreamNumber();
        else if (locator.getPrimaryAudioStreamNumber() > 0)
            return locator.getPrimaryAudioStreamNumber();
        else if (locator.getSecondaryVideoStreamNumber() > 0)
            return locator.getSecondaryVideoStreamNumber();
        else if (locator.getSecondaryAudioStreamNumber() > 0)
            return locator.getSecondaryAudioStreamNumber();
        else if (locator.getPGTextStreamNumber() > 0)
            return locator.getPGTextStreamNumber();
        return -1;
    }

    private Control getControl(BDLocator locator) {
        if (locator.getPrimaryVideoStreamNumber() > 0)
            return player.getControl("org.bluray.media.BackgroundVideoPresentationControl");
        else if (locator.getPrimaryAudioStreamNumber() > 0)
            return player.getControl("org.bluray.media.PrimaryAudioControl");
        else if (locator.getSecondaryVideoStreamNumber() > 0)
            return player.getControl("org.bluray.media.AsynchronousPiPControl");
        else if (locator.getSecondaryAudioStreamNumber() > 0)
            return player.getControl("org.bluray.media.SecondaryAudioControl");
        else if (locator.getPGTextStreamNumber() > 0)
            return player.getControl("org.bluray.media.SubtitlingControl");
        return null;
    }

    private LinkedList listeners = new LinkedList();
    private Handler player;
}
