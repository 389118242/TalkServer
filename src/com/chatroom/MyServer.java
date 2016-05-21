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

	Map<String, ObjectOutputStream> userOOS = new HashMap<>();// 用于存储聊天室内用户的用户名和对应的对象输出流
	ServerSocket server = null;

	public MyServer() {
		try {
			server = new ServerSocket(3927);
			while (true) {// 检测请求该ServerSocket的socket
				Socket socket = server.accept();// 获取socket
				new Talk(socket).start();// 创建对话线程
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class Talk extends Thread {// 内部类 继承多线程 实现同时对话
		private Socket socket;
		private String userName;

		Talk(Socket socket) {
			this.socket = socket;
			System.out.println(socket.getInetAddress());
		}

		@Override
		public void run() {
			try {
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());//// 用Socket的字节输入流创建对象输入流
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());//// 用Socket的字节输出流创建对象输出流
				while (true) {
					Message mess = (Message) ois.readObject();// 接受Socket发送的Message对象
					if ("login".equals(mess.getType())) {// mess用于验证用户名是否存在
						Message mess1 = new Message();// 根据具体情况用于反馈的Message对象
						if (userOOS.keySet().contains(mess.getName())) {// 用户名存在
							mess1.setType("login");
							mess1.setContent("faile");
							oos.writeObject(mess1);// 返回判断结果
							oos.flush();
							continue;
						} else {// 用户名可用
							mess1.setType("login");
							mess1.setContent("ok");
							oos.writeObject(mess1);// 返回判断结果
							oos.flush();
						}
						userName = mess.getName();
						userOOS.put(userName, oos);// 将用户名和输出对象流保存到Map集合
						Message mess3 = new Message();
						mess3.setType("list");
						mess3.setList(new ArrayList<>(userOOS.keySet()));
						oos.writeObject(mess3);
						Message mess2 = new Message();// 通知聊天室内其他人有新成员加入
						mess2.setName("[公告]");
						mess2.setType("postLogin");
						mess2.setContent("欢迎“" + mess.getName() + "”进入聊天室");
						say(mess2);
					} else if (!"say".equals(mess.getType())) {
						ObjectOutputStream oois = userOOS.get(mess.getType());
						if (null == oois) {
							Message messE = new Message();
							messE.setType("ERROR");
							messE.setName(mess.getType());
							messE.setContent("发送失败,用户“" + mess.getType() + "”已退出聊天室");
							oos.writeObject(messE);
						}else{
							oois.writeObject(mess);
						}
					} else {// mess发送消息
						say(mess);
					}
				}
			} catch (IOException e) {// 用户退出客户端
				e.printStackTrace();
				userOOS.remove(userName);// 在Map集合中移除
				if (null != userName) {// 提示用户退出聊天室，未完成重新输入用户名退出的，不送该消息
					Message mess3 = new Message();
					mess3.setName("[公告]");
					mess3.setType("postLogout");
					mess3.setContent("用户“" + userName + "”退出聊天室");
					say(mess3);
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		}

		/**
		 * 群发消息
		 * 
		 * @param mess
		 *            消息内容
		 */
		public void say(Message mess) {
			for (String name : userOOS.keySet()) {
				if (mess.getName().equals("[公告]") || !name.equals(userName)) // 公告发给所有人，普通消息发送给除发送者外的其他人
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
