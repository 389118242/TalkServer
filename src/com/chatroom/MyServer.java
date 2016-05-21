package com.chatroom;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MyServer {

	Map<String, ObjectOutputStream> userOOS = new HashMap<>();// ���ڴ洢���������û����û����Ͷ�Ӧ�Ķ��������
	ServerSocket server = null;

	public MyServer() {
		try {
			server = new ServerSocket(3927);
			while (true) {// ��������ServerSocket��socket
				Socket socket = server.accept();// ��ȡsocket
				new Talk(socket).start();// �����Ի��߳�
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class Talk extends Thread {// �ڲ��� �̳ж��߳� ʵ��ͬʱ�Ի�
		private Socket socket;
		private String userName;

		Talk(Socket socket) {
			this.socket = socket;
			System.out.println(socket.getInetAddress());
		}

		@Override
		public void run() {
			try {
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());//// ��Socket���ֽ���������������������
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());//// ��Socket���ֽ�������������������
				while (true) {
					Message mess = (Message) ois.readObject();// ����Socket���͵�Message����
					if ("login".equals(mess.getType())) {// mess������֤�û����Ƿ����
						Message mess1 = new Message();// ���ݾ���������ڷ�����Message����
						if (userOOS.keySet().contains(mess.getName())) {// �û�������
							mess1.setType("login");
							mess1.setContent("faile");
							oos.writeObject(mess1);// �����жϽ��
							oos.flush();
							continue;
						} else {// �û�������
							mess1.setType("login");
							mess1.setContent("ok");
							oos.writeObject(mess1);// �����жϽ��
							oos.flush();
						}
						userName = mess.getName();
						userOOS.put(userName, oos);// ���û�����������������浽Map����
						Message mess3 = new Message();
						mess3.setType("list");
						mess3.setList(new ArrayList<>(userOOS.keySet()));
						oos.writeObject(mess3);
						Message mess2 = new Message();// ֪ͨ�����������������³�Ա����
						mess2.setName("[����]");
						mess2.setType("postLogin");
						mess2.setContent("��ӭ��" + mess.getName() + "������������");
						say(mess2);
					} else if (!"say".equals(mess.getType())) {
						ObjectOutputStream oois = userOOS.get(mess.getType());
						if (null == oois) {
							Message messE = new Message();
							messE.setType("ERROR");
							messE.setName(mess.getType());
							messE.setContent("����ʧ��,�û���" + mess.getType() + "�����˳�������");
							oos.writeObject(messE);
						}else{
							oois.writeObject(mess);
						}
					} else {// mess������Ϣ
						say(mess);
					}
				}
			} catch (IOException e) {// �û��˳��ͻ���
				e.printStackTrace();
				userOOS.remove(userName);// ��Map�������Ƴ�
				if (null != userName) {// ��ʾ�û��˳������ң�δ������������û����˳��ģ����͸���Ϣ
					Message mess3 = new Message();
					mess3.setName("[����]");
					mess3.setType("postLogout");
					mess3.setContent("�û���" + userName + "���˳�������");
					say(mess3);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}

		/**
		 * Ⱥ����Ϣ
		 * 
		 * @param mess
		 *            ��Ϣ����
		 */
		public void say(Message mess) {
			for (String name : userOOS.keySet()) {
				if (mess.getName().equals("[����]") || !name.equals(userName)) // ���淢�������ˣ���ͨ��Ϣ���͸������������������
					try {
						ObjectOutputStream oos = userOOS.get(name);
						oos.writeObject(mess);
						oos.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}

	}

	public static void main(String[] args) {
		new MyServer();
	}

}
