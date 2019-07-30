package ssh;

import java.util.Date;

import com.jcraft.jsch.JSchException;

public class SyncMyclients {

	public static void testSSH() {
		try {
			// 使用目标服务器机上的用户名和密码登陆
			SSHHelper helper1 = new SSHHelper("192.168.10.21", 22, "root", "root");
			SSHHelper helper2 = new SSHHelper("192.168.10.22", 22, "root", "root");
			SSHHelper helper3 = new SSHHelper("192.168.10.23", 22, "root", "root");
			String command1 = "date -s \"" + new Date() + "\"";
			SSHResInfo resInfo = helper1.sendCmd(command1);
			System.out.println("zzq-hadoop1:" + resInfo.toString());
			resInfo = helper2.sendCmd(command1);
			System.out.println("zzq-hadoop2:" + resInfo.toString());
			resInfo = helper3.sendCmd(command1);
			System.out.println("zzq-hadoop3:" + resInfo.toString());
			helper1.close();
			helper2.close();
			helper3.close();
		} catch (JSchException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}