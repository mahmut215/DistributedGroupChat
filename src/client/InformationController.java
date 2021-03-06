package client;

import java.lang.Runnable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InformationController {
    private static Lock lock = new ReentrantLock();
    private static HashMap<String, Group> groups;
    private static HashMap<Long, Member> members;
    
    public InformationController() {
        groups = new HashMap<String, Group>();
        members = new HashMap<Long, Member>();
    }

    public static Lock getLock() {
        return lock;
    }

    public static String getGroupName(String groupId) {
        if (!groups.containsKey(groupId))
            return null;
        return groups.get(groupId).name;
    }
    public static void deleteGroup(String groupId){
        lock.lock();
        try{
            if(!groups.containsKey(groupId))
                return;
            groups.remove(groupId);
            return;
        }
        finally {
            lock.unlock();
        }
    }

    public static void addMember(Member m) {
        if (members.containsKey(m.getId()))
            return;

        members.put(m.getId(), m);
    }

    public static Group getGroup(String groupId) {
        return groups.get(groupId);
    }
    public static void addGroup(Group g) {
        if (!groups.containsKey(g.name));
            groups.put(g.name, g);
    }
    public static ArrayList<String> getAllGroups(){
        lock.lock();
        ArrayList<String> result = new ArrayList<String>();
        Iterator it = InformationController.groups.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry pair = (HashMap.Entry)it.next();
            result.add((String)pair.getKey());
            //System.out.println(pair.getKey() + " = " + pair.getValue());
            //it.remove(); // avoids a ConcurrentModificationException
        }
        lock.unlock();
        return result;

    }
    public static Iterator<Member> getGroupMembers(String groupId) {
        if (!groups.containsKey(groupId))
            return null;
        return groups.get(groupId).members.iterator();
    }

    public static int getNumberOfMembersInGroup(String groupId) {
        if (!groups.containsKey(groupId))
            return 0;
        return groups.get(groupId).members.size();
    }

    public static Member getMember(Long memberId) {
        return members.get(memberId);
    }

    public static PriorityQueue<Message> getGroupMessages(String groupId) {
        if (!groups.containsKey(groupId)) 
            return null;
        return groups.get(groupId).messages;
    }

    public static void addMessageToGroup(String groupId, Message m) {
        if (!groups.containsKey(groupId))
            return;
        groups.get(groupId).messages.add(m);
    }

    public static void setMessageTimer(String groupId, Message msg, MessageHandler mh) {
        if (!groups.containsKey(groupId))
            return;
        // if timer already exists return
        if (groups.get(groupId).timers.containsKey(String.valueOf(msg.getUserId()) + "," + String.valueOf(msg.getMessageId())))
            return;

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                // we shouldn't receive a message and handle a timer
                // at the same time
                InformationController.getLock().lock();
                try {
                    Member m = InformationController.getMember(msg.getUserId());
                    mh.deliverMessage(groupId);
                    if (m.getExpectedMessageId() < msg.getMessageId()) {
                        m.setExpectedMessageId(msg.getMessageId());
                        mh.deliverMessage(msg.getGroupId());
                    }
                } finally {
                    InformationController.getLock().unlock();
                }
            }
        },1000L);

        groups.get(groupId).timers.put(String.valueOf(msg.getUserId()) + "," + String.valueOf(msg.getMessageId()), t);
    }

    public static void cancelMessageTimer(String groupId, Message msg) {
        if (!groups.containsKey(groupId))
            return;
        
        // ignore if timer doesn't exist
        if (!groups.get(groupId).timers.containsKey(String.valueOf(msg.getUserId()) + "," + String.valueOf(msg.getMessageId())))
            return;
        
        Timer t = groups.get(groupId).timers.get(String.valueOf(msg.getUserId()) + "," + String.valueOf(msg.getMessageId()));
        t.cancel();
    }
    
}