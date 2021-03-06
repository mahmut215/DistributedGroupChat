package client;

import common.*;

import javax.sound.sampled.Line;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class CommandHandler {
    private Socket sock;
    private ObjectOutputStream sOutput;
    private ObjectInputStream sInput;
    private Random rand;

    private MessageHandler mh;
    public CommandHandler(MessageHandler mh) {
        this.mh = mh;
        rand = new Random();
    }

    public static void main(String[] args) throws Throwable {
        int port = Client.getTrackerPort();
        String serverAddress = Client.getTrackerAddr();
        Socket sock = new Socket(serverAddress, port);
        ObjectOutputStream sOutput = new ObjectOutputStream(sock.getOutputStream());
        ObjectInputStream sInput = new ObjectInputStream(sock.getInputStream());
        Scanner s = new Scanner(System.in);
        while (true) {
            String str = s.nextLine();
            if ("register".equals(str)) {
                sOutput.writeObject(new ControlMessage(ControlMessage.Type.Register, "192.168.1.1,80,alexm"));
                RegisterReply r = (RegisterReply) sInput.readObject();
                System.out.println(r.id);
                sOutput.writeObject(new ControlMessage(ControlMessage.Type.JoinGroup,"distrib"));
                JoinGroupReply zz = (JoinGroupReply) sInput.readObject();
                System.out.println(zz.users);
                sOutput.writeObject(new ControlMessage(ControlMessage.Type.ListGroups, ""));
                ListGroupsReply x = (ListGroupsReply) sInput.readObject();
                System.out.println(x.groups);
//                sOutput.writeObject(new ControlMessage(ControlMessage.Type.ListMembers,"distrib"));
//                ListMembersReply z;
//                z = (ListMembersReply) sInput.readObject();
//                System.out.println(z.users);

            } else {
                System.out.println("Hello world");
            }
        }
    }
    private void beginConnection()throws Throwable{
        int port = Client.getTrackerPort();
        String serverAddress = Client.getTrackerAddr();
        sock = new Socket(serverAddress, port);
        sOutput = new ObjectOutputStream(sock.getOutputStream());
        sInput = new ObjectInputStream(sock.getInputStream());
    }
    public void execute (String command)throws Throwable{


            if(command.equals("r")) { // register user to tracker.
                this.beginConnection();
                Client.setUsername("client-" + String.valueOf(rand.nextInt(100)));
                sOutput.writeObject(new ControlMessage(ControlMessage.Type.Register, mh.getLocalAddress().getHostAddress() +"," + String.valueOf(mh.getSocket().getLocalPort()) + "," + Client.getUsername() ,-1));
                RegisterReply r = (RegisterReply) sInput.readObject();
                sock.close();
                System.out.println(r.id);
                Client.setClientId(r.id);
            }
            else if(command.equals("lg")){
                this.beginConnection();
                sOutput.writeObject(new ControlMessage(ControlMessage.Type.ListGroups, "", (int)Client.getClientId()));
                ListGroupsReply x = (ListGroupsReply) sInput.readObject();
                sock.close();
                System.out.println(x.groups);
            }
            else if(command.charAt(0) == 'l' && command.charAt(1) == 'm') {
                System.out.println("Hello man!");
                this.beginConnection();
                sOutput.writeObject(new ControlMessage(ControlMessage.Type.ListMembers,command.substring(3), (int)Client.getClientId()));
                ListMembersReply z;
                z = (ListMembersReply) sInput.readObject();
                sock.close();
                System.out.println(z.users);
            }
            else if(command.charAt(0) == 'j'){
                this.beginConnection();
                sOutput.writeObject(new ControlMessage(ControlMessage.Type.JoinGroup,command.substring(2), (int)Client.getClientId()));
                JoinGroupReply zz = (JoinGroupReply) sInput.readObject();
                sock.close();
                String groupId = command.substring(2);
                // acquire lock
                InformationController.getLock().lock();
                try {
                    Group g = InformationController.getGroup(groupId);
                    if (g == null)
                        g = new Group(groupId,mh);
                    g.dropMembers();
                    for (UserInfo user : zz.users) {
                        Member m = new Member(user.id, InetAddress.getByName(user.ip), user.port, user.username);
                        g.addMember(m);
                        InformationController.addMember(m);
                    }
                    InformationController.addGroup(g);
                } finally {
                    InformationController.getLock().unlock();
                }
                // end of critical section
                System.out.println(zz.users);
            }
            else if(command.charAt(0) == 'e'){
                this.beginConnection();
                sOutput.writeObject(new ControlMessage(ControlMessage.Type.ExitGroup,command.substring(2), (int)Client.getClientId()));
                sock.close();
                InformationController.deleteGroup(command.substring(2));


            }
            else if(command.charAt(0)=='q'){
                this.beginConnection();
                sOutput.writeObject(new ControlMessage(ControlMessage.Type.Quit,"", (int)Client.getClientId()));
                ControlReply zz = (ControlReply) sInput.readObject();
                sock.close();
                System.exit(0);
            }
            else if(command.charAt(0) == 'w') {
                String groupname = command.substring(2);
                Group g = InformationController.getGroup(groupname);
                if (g == null) {
                    System.out.println("There is no such group");
                } else {
                    Client.setCurrentGroupId(groupname);
                }
            }
            else
                System.out.println("Unkown command. Type !h for the help menu");

    }

    public long registerClient() {
        return  1L;
    }

}

