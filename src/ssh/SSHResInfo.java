package ssh;

public class SSHResInfo {
    private int exitStuts;//����״̬�� ����linux�п���ͨ�� echo $? ��֪ÿ��ִ����ִ�е�״̬�룩  
    private String outRes;//��׼��ȷ���������  
    private String errRes;//��׼�������������  



    public SSHResInfo(int exitStuts, String outRes, String errRes) {
        super();
        this.exitStuts = exitStuts;
        this.outRes = outRes;
        this.errRes = errRes;
    }

    public SSHResInfo() {
        super();
    }

    public int getExitStuts() {
        return exitStuts;
    }

    public void setExitStuts(int exitStuts) {
        this.exitStuts = exitStuts;
    }

    public String getOutRes() {
        return outRes;
    }

    public void setOutRes(String outRes) {
        this.outRes = outRes;
    }

    public String getErrRes() {
        return errRes;
    }

    public void setErrRes(String errRes) {
        this.errRes = errRes;
    }

    /**��exitStuts=0 && errRes="" &&outREs=""����true
     * @return
     */
    public boolean isEmptySuccess(){
        if(this.getExitStuts()==0 && "".equals(this.getErrRes())&& "".equals(this.getOutRes())){
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "SSHResInfo [exitStuts=" + exitStuts + ", outRes=" + outRes + ", errRes=" + errRes + "]";
    }

    public void clear(){  
     exitStuts=0;  
     outRes=errRes=null;  
     } 
}