package org.bobstuff.bobball.GameLogic;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class GameEventQueue implements Parcelable {
    public static final Creator<GameEventQueue> CREATOR = new Creator<GameEventQueue>() {
        @Override
        public GameEventQueue createFromParcel(Parcel in) {
            return new GameEventQueue(in);
        }

        @Override
        public GameEventQueue[] newArray(int size) {
            return new GameEventQueue[size];
        }
    };
    private ConcurrentNavigableMap<Integer, List<GameEvent>> queue;

    public GameEventQueue() {
        queue = new ConcurrentSkipListMap<>();
    }


    protected GameEventQueue(Parcel in) {
        this();
        ClassLoader classLoader = getClass().getClassLoader();
        ArrayList<GameEvent> list = in.readArrayList(classLoader);
        for (GameEvent ev : list) {
            addEvent(ev);
        }
    }

    public int getEarliestEvTime() {
        Integer t = queue.ceilingKey(0);
        if (t != null)
            return t;
        else
            return Integer.MAX_VALUE;
    }

    public GameEvent popEventAt(int time) {
        GameEvent ret = null;
        List<GameEvent> evlist = queue.get(time);

        if (evlist != null && evlist.size() > 0) {
            ret = evlist.remove(0);

            if (evlist.size() == 0)
                queue.remove(time);
        }
        return ret;
    }

    public boolean addEvent(GameEvent ev) {
        int time = ev.getTime();
        List<GameEvent> evlist = queue.get(time);
        if (evlist == null) {
            evlist = new ArrayList<>();
        }
        if (!evlist.contains(ev)) {
            evlist.add(ev);
            queue.put(time, evlist);
            return false;
        } else
            return true;
    }

    //get the oldest element newer than cutoff and remove it from the queue
    public GameEvent popOldestEventNewerThan(int cutoff) {
        GameEvent ret = null;
        Integer oldestTime = queue.ceilingKey(cutoff);

        if (oldestTime != null) {
            ret = popEventAt(oldestTime);
        }
        return ret;
    }

    public void purgeOlderThan(int cutoff) {
        for (Integer evtime : queue.descendingKeySet()) {
            if (evtime < cutoff)
                queue.remove(evtime);
            else
                break;
        }
    }

    public void clear() {
        queue.clear();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        List<GameEvent> l = new ArrayList<>();
        for (List<GameEvent> evlist : queue.values()) {
            l.addAll(evlist);
        }
        dest.writeList(l);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(this.getClass().getName() + ":\n");
        for (int time : queue.navigableKeySet()) {
            result.append("    [t=" + time + "] { ");
            for (GameEvent ev : queue.get(time))
                result.append("    " + ev.toString() + "; ");
            result.append("} \n");
        }
        result.append("}");

        return result.toString();
    }

}



