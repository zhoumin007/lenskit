/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2013 Regents of the University of Minnesota and contributors
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.grouplens.lenskit.data.dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.grouplens.lenskit.cursors.Cursor;
import org.grouplens.lenskit.data.event.Event;

import javax.inject.Inject;
import java.util.List;

/**
 * Item event DAO that pre-loads all events from an event DAO.
 *
 * @since 2.0
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
public final class PrefetchingItemEventDAO implements ItemEventDAO {
    private final EventDAO eventDAO;
    private transient volatile Long2ObjectMap<List<Event>> itemEvents;

    @Inject
    public PrefetchingItemEventDAO(EventDAO dao) {
        eventDAO = dao;
    }

    private void loadEvents() {
        if (itemEvents != null) {
            return;
        }

        synchronized (this) {
            if (itemEvents != null) {
                return;
            }
            Long2ObjectMap<ImmutableList.Builder<Event>> table =
                    new Long2ObjectOpenHashMap<ImmutableList.Builder<Event>>();
            Cursor<Event> events = eventDAO.streamEvents();
            try {
                for (Event evt: events) {
                    final long iid = evt.getItemId();
                    ImmutableList.Builder<Event> list = table.get(iid);
                    if (list == null) {
                        list = new ImmutableList.Builder<Event>();
                        table.put(iid, list);
                    }
                    list.add(evt);
                }
            } finally {
                events.close();
            }
            Long2ObjectMap<List<Event>> result = new Long2ObjectOpenHashMap<List<Event>>(table.size());
            for (Long2ObjectMap.Entry<ImmutableList.Builder<Event>> evt: table.long2ObjectEntrySet()) {
                result.put(evt.getLongKey(), evt.getValue().build());
                evt.setValue(null);
            }
            itemEvents = result;
        }
    }

    @Override
    public List<Event> getEventsForItem(long item) {
        loadEvents();
        return itemEvents.get(item);
    }

    @Override
    public <E extends Event> List<E> getEventsForItem(long item, Class<E> type) {
        List<Event> events = getEventsForItem(item);
        if (events == null) {
            return null;
        } else {
            return ImmutableList.copyOf(Iterables.filter(events, type));
        }
    }

    @Override
    public LongSet getUsersForItem(long item) {
        List<Event> events = getEventsForItem(item);
        if (events == null) {
            return null;
        }

        LongSet users = new LongOpenHashSet();
        for (Event evt: events) {
            users.add(evt.getUserId());
        }
        return users;
    }
}
