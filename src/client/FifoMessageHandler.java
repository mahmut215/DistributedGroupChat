package client;

import java.util.Iterator;
import java.util.PriorityQueue;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;


public class FifoMessageHandler extends MessageHandler {
    

    public void sendMessage(String message) {
        
        long clientId = Client.getClientId();
        long groupId = Client.getCurrentGroupId();
        long messageId = getNextMessageId();

        Message msg = new Message(clientId, groupId, messageId, message);

        Iterator<Member> recipient = 
            InformationController.getGroupMembers(Client.getCurrentGroupId());


        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        try {
            ObjectOutput oo = new ObjectOutputStream(bStream);
            oo.writeObject(msg);
            oo.close();

            byte[] serializedMessage = bStream.toByteArray();
            DatagramSocket socket = getSocket();

            DatagramPacket packet 
                = new DatagramPacket(serializedMessage, serializedMessage.length, getLocalAddress(), socket.getLocalPort());
            
            socket.send(packet);
            

            if (recipient == null) {
                // there is no group with such id
                return;
            }

            while (recipient.hasNext()) {
                Member m = recipient.next();
                
                packet = new DatagramPacket(serializedMessage, serializedMessage.length, m.getIp(), m.getPort());
                socket.send(packet);
            }

        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }
    
    
    public void receiveMessage(Message msg) {
        
        // add message to group heap 
        PriorityQueue<Message> messages = InformationController.getGroupMessages(msg.getGroupId());
        if (messages == null) {
            // group doesn't exist 
            return;
        }

        messages.add(msg);
        deliverMessage(msg.getGroupId());
    }

    public void deliverMessage(Long groupId) {
        // first we obtain the name of Group
        String groupName = InformationController.getGroupName(groupId);
        if (groupName == null) {
            // the group doesn't exist
            // this is clearly an error
            // in this case we can ignore the message
            return;
        }

        // then get group heap
        PriorityQueue<Message> messages = InformationController.getGroupMessages(groupId);
        if (messages == null) {
            // group doesn't exist 
            return;
        }

        while (true) {
            // get top message
            Message m = messages.peek();
            
            // then we obtain the Member that 
            // send this message.
            Member sender = InformationController.getMember(m.getUserId());
            if (sender == null) {
                // this sender isn't yet known to us
                // what should we do here?
                // FIX ME
            }

            if (m.getMessageId() != sender.getExpectedMessageId())
                break;

            if (m.getMessageId() < sender.getExpectedMessageId()) {
                // we should ignore these messages
                messages.remove();
                continue;
            }

            System.out.print("in " + groupName + sender.getUsername() + "says:: ");
            System.out.println(m.getMessage());

            //and increase the expectedMessageId
            sender.setExpectedMessageId(m.getMessageId()+1);

            // remove this message from messages
            messages.remove();

        }

        if (!messages.isEmpty()) {
            // we couldn't deliver a message
            // because it wan't expected
            // we need to add timer to 
            // wait for expected messages from this
            // user

            Message m = messages.peek();
            InformationController.setMessageTimer(groupId, m, this);

        }
    }
    
}