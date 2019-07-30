package ssh;

import java.io.InputStream;
import java.nio.charset.Charset;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**   java ssh��¼linux�Ժ��һЩ������ʽ
* 
*/
public class SSHHelper{
     private String charset = Charset.defaultCharset().toString();
     private Session session;

     public SSHHelper(String host, Integer port, String user, String password) throws JSchException {
         connect(host, port, user, password);
     }

     /**
         * ����sftp������
         * @param host Զ������ip��ַ
         * @param port sftp���Ӷ˿ڣ�null ʱΪĬ�϶˿�
         * @param user �û���
         * @param password ����
         * @return
         * @throws JSchException 
         */
        private Session connect(String host, Integer port, String user, String password) throws JSchException{
            try {
                JSch jsch = new JSch();
                if(port != null){
                    session = jsch.getSession(user, host, port.intValue());
                }else{
                    session = jsch.getSession(user, host);
                }
                session.setPassword(password);
                //���õ�һ�ε�½��ʱ����ʾ����ѡֵ:(ask | yes | no)
                session.setConfig("StrictHostKeyChecking", "no");
                //30�����ӳ�ʱ
                session.connect(5000);
            } catch (JSchException e) {
                e.printStackTrace();
                System.out.println("SFTPUitl ��ȡ���ӷ�������");
                throw e;
            }
            return session;
        }

        public SSHResInfo sendCmd(String command) throws Exception{
            return sendCmd(command, 200);
        }
        /*
        * ִ���������ִ�н��
        * @param command ����
        * @param delay ����shell����ִ��ʱ��
        * @return String ִ�������ķ���
        * @throws IOException
        * @throws JSchException
        */
        public SSHResInfo sendCmd(String command,int delay) throws Exception{
            if(delay <50){
                delay = 50;
            }
            SSHResInfo result = null;
            byte[] tmp = new byte[1024]; //�����ݻ���
            StringBuffer strBuffer = new StringBuffer();  //ִ��SSH���صĽ��
            StringBuffer errResult=new StringBuffer();

            Channel channel = session.openChannel("exec");
            ChannelExec ssh = (ChannelExec) channel;
            //���صĽ�������Ǳ�׼��Ϣ,Ҳ�����Ǵ�����Ϣ,�������������Ҫ��ȡ
            //һ�������ֻ����һ�����.
            //��������˵������Ϣ����ִ������������Ϣ,����Զ��java JDK�汾����
            //ErrStream�����.
            InputStream stdStream = ssh.getInputStream();
            InputStream errStream = ssh.getErrStream();  
            ssh.setCommand(command);
            ssh.connect();

            try {


                //��ʼ���SSH����Ľ��
                while(true){
                //��ô������
                    while(errStream.available() > 0){
                        int i = errStream.read(tmp, 0, 1024);
                        if(i < 0) break;
                        errResult.append(new String(tmp, 0, i));
                    }

                   //��ñ�׼���
                    while(stdStream.available() > 0){
                        int i = stdStream.read(tmp, 0, 1024);
                        if(i < 0) break;
                        strBuffer.append(new String(tmp, 0, i));
                    }
                    if(ssh.isClosed()){
                        int code = ssh.getExitStatus();
//                        logger.info("exit-status: " + code);
                        result = new SSHResInfo(code, strBuffer.toString(), errResult.toString());
                        break;
                    }
                    try
                    {
                        Thread.sleep(delay);
                    }
                    catch(Exception ee)
                    {
                        ee.printStackTrace();
                    }
                }
            } finally {
                channel.disconnect();
            }

            return result;
        }

        /**
         * @param in
         * @param charset
         * @return
         * @throws IOException
         * @throws UnsupportedEncodingException
         */
        private String processStream(InputStream in, String charset) throws Exception {
            byte[] buf = new byte[1024];
            StringBuilder sb = new StringBuilder();
            while (in.read(buf) != -1) {
                sb.append(new String(buf, charset));
            }
            return sb.toString();
        }

        public boolean deleteRemoteFIleOrDir(String remoteFile){
            ChannelSftp channel=null;  
             try {  
                 channel=(ChannelSftp) session.openChannel("sftp");  
                 channel.connect();  
                 SftpATTRS sftpATTRS= channel.lstat(remoteFile);  
                 if(sftpATTRS.isDir()){  
                     //Ŀ¼  
//                     logger.debug("remote File:dir");  
                     channel.rmdir(remoteFile);
                     return true;  
                 }else if(!sftpATTRS.isDir()){  
                     //�ļ�  
//                     logger.debug("remote File:file");  
                     channel.rm(remoteFile);
                     return true;  
                 }else{  
//                     logger.debug("remote File:unkown");  
                     return false;  
                 }  
             }catch (JSchException e) {  
                 if(channel!=null){  
                     channel.disconnect();  
                     session.disconnect();  
                 }
//                 logger.error("error",e);  
                return  false;  
             } catch (SftpException e) { 
//                 logger.info("meg"+e.getMessage());
//                 logger.error("SftpException",e);  
                 return false;
             }  

        }

        /** 
         * �ж�linux�� ĳ�ļ��Ƿ���� 
         */  
     public boolean detectedFileExist(String remoteFile) {  

     ChannelSftp channel=null;  
     try {  
         channel=(ChannelSftp) session.openChannel("sftp");  
         channel.connect();  
         SftpATTRS sftpATTRS= channel.lstat(remoteFile);  
         if(sftpATTRS.isDir()||!sftpATTRS.isDir()){  
             //Ŀ¼ ���ļ�  
//             logger.info("remote File:dir");  
             return true;  
         }else{  
//             logger.info("remote File:unkown");  
             return false;  
         }  
     }catch (JSchException e) {  
         if(channel!=null){  
             channel.disconnect();  
             session.disconnect();  
     }  
        return  false;  
     } catch (SftpException e) {  
//         logger.error(e.getMessage());  
     }  
     return false;  
     } 



     /**����ǵùرգ���������һֱ���ڣ����򲻻��˳�
     * 
     */
    public void close(){
        if(session.isConnected())
        session.disconnect();
     }


}