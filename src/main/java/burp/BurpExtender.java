package burp;

import UI.Tags;
import func.vulscan;
import utils.BurpAnalyzedRequest;
import utils.DomainNameRepeat;
import utils.UrlRepeat;
import yaml.YamlUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class BurpExtender implements IBurpExtender, IScannerCheck,IContextMenuFactory {

    public static String Yaml_Path = System.getProperty("user.dir") + "/" +"Config_yaml.yaml";
    public IBurpExtenderCallbacks call;
    private DomainNameRepeat DomainName;
    public IExtensionHelpers help;
    public Tags tags;
    private UrlRepeat urlC;
    public Collection<String> history_url = new LinkedList<String>();
    public static String EXPAND_NAME = "Route Vulnerable Scanning";
    public View view_class;
    public List<View.LogEntry> log;
    public Config Config_l;
    public ExecutorService ThreadPool;

    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks) {
        this.call = callbacks;
        this.help = call.getHelpers();
        this.view_class = new View();
        this.DomainName = new DomainNameRepeat();
        this.urlC = new UrlRepeat();
        this.log = this.view_class.log;
        this.Config_l = new Config(view_class,log);
        this.tags = new Tags(callbacks,Config_l);
        if (!new File(Yaml_Path).exists()){
            YamlUtil.init_Yaml(Yaml_Path);
        }
        Bfunc.show_yaml(view_class,log,Yaml_Path);
        call.printOutput("Indo@Loading RouteVulScan success");
        call.printOutput("From@Code by F6JO");
        call.printOutput("Github@https://github.com/F6JO/RouteVulScan");
        call.setExtensionName(EXPAND_NAME);
        call.registerScannerCheck(this);
        call.registerContextMenuFactory(this);

    }

    public List<IScanIssue> doPassiveScan(IHttpRequestResponse baseRequestResponse) {
        this.ThreadPool = Executors.newFixedThreadPool((Integer) Config_l.spinner1.getValue());
        ArrayList<IScanIssue> IssueList = new ArrayList();
        IHttpService Http_Service = baseRequestResponse.getHttpService();
        String Root_Url = Http_Service.getProtocol() + "://" + Http_Service.getHost() + ":" + String.valueOf(Http_Service.getPort());
        try {
            URL url = new URL(Root_Url + this.help.analyzeRequest(baseRequestResponse).getUrl().getPath());
            BurpAnalyzedRequest Root_Request = new BurpAnalyzedRequest(this.call, baseRequestResponse);
            String Root_Method = this.help.analyzeRequest(baseRequestResponse.getRequest()).getMethod();
            String New_Url = this.urlC.RemoveUrlParameterValue(url.toString());
            if (this.urlC.check(Root_Method, New_Url)) {
                return null;
            }
            new vulscan(this,Root_Request);
            this.urlC.addMethodAndUrl(Root_Method, New_Url);
            try {
                this.DomainName.add(Root_Url);
                return IssueList;
            } catch (Throwable th) {
                return IssueList;
            }
        } catch (MalformedURLException e3) {
            throw new RuntimeException(e3);
        }
    }

    public List<IScanIssue> doActiveScan(IHttpRequestResponse baseRequestResponse, IScannerInsertionPoint insertionPoint) {
        return null;
    }

    public int consolidateDuplicateIssues(IScanIssue existingIssue, IScanIssue newIssue) {
        return 0;
    }

    @Override
    public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        List<JMenuItem> menu = new ArrayList<JMenuItem>();
        JMenuItem one_menu = new JMenuItem("Send To RouteVulScan");
        one_menu.addActionListener(new Right_click_monitor(invocation,this));
        menu.add(one_menu);

        return menu;
    }
}


class Right_click_monitor implements ActionListener{
    private IContextMenuInvocation invocation;
    private BurpExtender burp;
    public Right_click_monitor(IContextMenuInvocation invocation,BurpExtender burp){
        this.invocation = invocation;
        this.burp = burp;
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        burp.ThreadPool = Executors.newFixedThreadPool((Integer) burp.Config_l.spinner1.getValue());
        IHttpRequestResponse[] RequestResponses = invocation.getSelectedMessages();
        for (IHttpRequestResponse i : RequestResponses){
            try {
//                IHttpService HttpService = i.getHttpService();
//                IRequestInfo RequestInfo = burp.help.analyzeRequest(HttpService,i.getRequest());

                IHttpService Http_Service = i.getHttpService();
                String Root_Url = Http_Service.getProtocol() + "://" + Http_Service.getHost() + ":" + String.valueOf(Http_Service.getPort());
                URL url = new URL(Root_Url + burp.help.analyzeRequest(i).getUrl().getPath());
                BurpAnalyzedRequest Root_Request = new BurpAnalyzedRequest(burp.call, i);
                start_send send = new start_send(burp, Root_Request);
                send.start();
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        }
//        burp.call.printOutput(String.valueOf(RequestResponses[0]));
    }
}

class start_send extends Thread {
    private BurpExtender burp;
    private BurpAnalyzedRequest Root_Request;
    public start_send(BurpExtender burp,BurpAnalyzedRequest Root_Request){
        this.burp = burp;
        this.Root_Request = Root_Request;
    }
    public void run(){
        new vulscan(this.burp,this.Root_Request);
    }

}
