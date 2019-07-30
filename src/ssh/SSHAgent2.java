package ssh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.jcraft.jsch.JSchException;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;  
  
/** 
 * 模拟交互式终端 
 *  
 * @author doctor 
 * 
 * @time 2015年8月6日   
 * 
 * @see http://www.programcreek.com/java-api-examples/index.php?api=ch.ethz.ssh2.SCPClient 
 * 
 */  
public class SSHAgent2 {  
    private Connection connection;  
    private Session session;  
    private BufferedReader stdout;  
    private PrintWriter printWriter;  
    private BufferedReader stderr;  
    private ExecutorService service = Executors.newFixedThreadPool(3);  
    private Scanner scanner = new Scanner(System.in);
    String nextLine="";
    CountDownLatch latch=new CountDownLatch(1);
  
    public void initSession(String hostName, String userName, String passwd) throws IOException {  
        connection = new Connection(hostName);  
        connection.connect();  
        scanner = new Scanner(System.in);
        scanner.useDelimiter("\n");
        boolean authenticateWithPassword = connection.authenticateWithPassword(userName, passwd);  
        if (!authenticateWithPassword) {  
            throw new RuntimeException("Authentication failed. Please check hostName, userName and passwd");  
        }  
        session = connection.openSession();  
        session.requestDumbPTY();  
        session.startShell();  
        stdout = new BufferedReader(new InputStreamReader(new StreamGobbler(session.getStdout()), StandardCharsets.UTF_8));  
        stderr = new BufferedReader(new InputStreamReader(new StreamGobbler(session.getStderr()), StandardCharsets.UTF_8));  
        printWriter = new PrintWriter(session.getStdin());
    }  
  
    public void execCommand() throws IOException {
        service.submit(new Runnable() {  
            @Override  
            public void run() {  
                String line="";
                try {  
                	if(!"out".equals(Thread.currentThread().getName())){
                		Thread.currentThread().setName("out");
                	}
                	
                	int i=1;
                	int count=1;
                    while ((line = stdout.readLine())!= null) {
                    	if(count==1){
                    		i++;
                    		count++;
                    	}
                    	if(i==1){
                    		i++;
                    		continue;
                    	}
                    	if(line.endsWith("# ")){
                    		System.out.print(line);
                    		i=1;
                    	}else{
                    		System.out.println(line);
                    		if(count==2){
                    			printWriter.write("\r\n");  
                                printWriter.flush();
                                count++;
                    		}
                    	}
                    }
                    
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
        });  
        service.submit(new Runnable() {  
            @Override  
            public void run() {  
            	if(!"in".equals(Thread.currentThread().getName())){
            		Thread.currentThread().setName("in");
            	}
                while (true) {  
                    try {  
                        TimeUnit.SECONDS.sleep(1);  
                    } catch (InterruptedException e) {  
                        e.printStackTrace();  
                    }  
                    nextLine = scanner.nextLine();
                    if(nextLine.equals("q")){
                    	close();
                    	latch.countDown();
                    }
                    printWriter.write(nextLine + "\r\n");  
                    printWriter.flush();  
                }  
            }  
        }); 
    }  
  
    public void close() {
    	try {
			if (stdout!= null)
				stdout=null;
			if (stderr != null)
				stderr=null;
			if (printWriter != null)
				printWriter=null;
			if (scanner != null)
				scanner=null;
			if (session != null)
				session=null;
			if (connection != null)
				connection=null;
		} catch (Exception e) {
		}
    }  
  
    /** 
     * @param args 
     * @throws IOException 
     * @throws JSchException 
     * @throws URISyntaxException 
     */  
    public static void main(String[] args) throws Exception {
    	SSHAgent2 sshAgent = new SSHAgent2();
    	Properties prop = new Properties();
    	System.out.println(new File(".").getAbsolutePath());
		File file=new File(".\\infos.properties");
		if(!file.exists()){
			file.createNewFile();
		}
    	System.out.println(file.getAbsolutePath());
    	prop.load(new FileInputStream(file));
    	boolean flag=true;
    	while(flag){
    	System.out.println("1.查看已保存连接");
    	System.out.println("2.新建连接");
		System.out.println("3.一键同步时间");
		System.out.println("4.同步默认集群");
		System.out.println("5.退出");
		System.out.println("\n---------------------------\n");
		System.out.print("选择要进行的操作:");
		switch (new Scanner(System.in).nextInt()) {
		case 1:
			if(prop.getProperty("links")!=null){
				String[] links=prop.getProperty("links").split(",");
				if(links.length>0){
					for (int i=0;i<links.length;i++) {
						System.out.println(i+1+"."+links[i]);
					}
					System.out.print("选择要操作的连接:");
					int link=new Scanner(System.in).nextInt();
					if(link>links.length){
						break;
					}
					String mlink=links[link-1];
					System.out.println("1.连接\n2.同步时间\n3.删除");
					System.out.print("选择要进行的操作:");
					switch (new Scanner(System.in).nextInt()) {
					case 1:
						sshAgent.initSession(mlink, prop.getProperty(mlink+".user"), prop.getProperty(mlink+".pwd"));  
				        sshAgent.execCommand();
				        sshAgent.latch.await();
				        sshAgent.latch=new CountDownLatch(1);
						break;
					case 2:
						syncTime(mlink, prop.getProperty(mlink+".user"), prop.getProperty(mlink+".pwd"));
						break;
					case 3:
						String value="";
						for (int i=0;i<links.length;i++) {
							if(mlink.equals(links[i])){
								continue;
							}
							value+=links[i]+",";
						}
						value=value.substring(0, value.length()-1);
						prop.setProperty("links", value);
						prop.store(new FileOutputStream(file),null);
						break;
					default:
						break;
					}
				}
			}
			break;
		case 2:
			String clink=null;
			String user=null;
			String pwd=null;
			System.out.print("输入主机名或IP地址:");
			clink=System.console().readLine();
//			clink=sshAgent.getScanner().next();
			System.out.print("输入用户名:");
			user=System.console().readLine();
//			user=sshAgent.getScanner().next();
			System.out.print("输入密码(密码加密不显示):");
			pwd=new String(System.console().readPassword());
//			pwd=sshAgent.getScanner().next();
			prop.setProperty("links", prop.getProperty("links")!=null?prop.getProperty("links")+","+clink:"");
			prop.setProperty(clink+".user", user);
			prop.setProperty(clink+".pwd", pwd);
			prop.store(new FileOutputStream(file),null);
			break;
		case 3:
			String[] links=prop.getProperty("links").split(",");
			if(links.length>0){
				for (int i=0;i<links.length;i++) {
					System.out.println(i+1+"."+links[i]);
				}
				System.out.print("选择要同步的主机(空格隔开):");
				String[] synclinks=System.console().readLine().split(" ");
				for (String string : synclinks) {
					int i=Integer.parseInt(string)-1;
					syncTime(links[i], prop.getProperty(links[i]+".user"), prop.getProperty(links[i]+".pwd"));
				}
			}
			break;
		case 4:
			SyncMyclients.testSSH();
			break;
		default:
			sshAgent.close();
			System.exit(0);
			break;
		}
    	}
    }

	public Scanner getScanner() {
		if(scanner==null){
			scanner = new Scanner(System.in);
		}
		return scanner;
	}

	public void setScanner(Scanner scanner) {
		this.scanner = scanner;
	}

	public String getNextLine() {
		return nextLine;
	}

	public void setNextLine(String nextLine) {
		this.nextLine = nextLine;
	}
    
	public static void syncTime(String mlink,String user,String pwd){
        try {
            SSHHelper helper1 = new SSHHelper(mlink, 22, user, pwd);
            String command1 = "date -s \""+new Date()+"\"";
            		SSHResInfo resInfo = helper1.sendCmd(command1);
            		if(resInfo.getExitStuts()==0){
            			 System.out.println(mlink+":时间同步成功.");
            		}
            	helper1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}  