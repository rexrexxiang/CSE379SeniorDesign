import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

public class GUI {

    static boolean twittervalue = false;
    static boolean weibovalue = false;
    static boolean wechatvalue = false;
    static PrintStream rlog = null;
    static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] argv) {
        init();
    }

    private static void init() {
        // Frame
        JFrame MainFrame = new JFrame();

        // Panel
        JPanel MainPanel = new JPanel(new BorderLayout());
        JPanel ControlPanel = new JPanel();
        JPanel first = new JPanel();
        JPanel second = new JPanel();
        JPanel third = new JPanel();
        JPanel fourth = new JPanel();
        JPanel fifth = new JPanel();

        ControlPanel.setLayout(new GridLayout(5, 1));
        first.setLayout(new FlowLayout(FlowLayout.LEFT));
        second.setLayout(new FlowLayout(FlowLayout.LEFT));
        third.setLayout(new FlowLayout(FlowLayout.LEFT));
        fourth.setLayout(new FlowLayout(FlowLayout.CENTER));
        fifth.setLayout(new FlowLayout(FlowLayout.CENTER));

        // Output
        JTextArea Output = new JTextArea();
        ((DefaultCaret)Output.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scrollPane = new JScrollPane(Output);

        // Checkbox
        JCheckBox TwitterBox = new JCheckBox("Twitter: ");
        JCheckBox WeiboButton = new JCheckBox("Weibo:   ");
        JCheckBox WeChatButton = new JCheckBox("WeChat:  ");

        // Button
        JButton CollectButton = new JButton("Data Collection");
        JButton AnalysisButton = new JButton("Analysis");
        JButton TranslationButton = new JButton("Translation");

        JButton ClearButton = new JButton("Clear Screen");

        // Text
        JTextArea TwitterText = new JTextArea(1, 20);
        JTextArea WeiboText = new JTextArea(1, 20);
        JTextArea WeChatText = new JTextArea(1, 20);

        // Format
        // get the screen size as a java dimension
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // get 2/3 of the height, and 2/3 of the width
        int height = screenSize.height * 2 / 3;
        int width = screenSize.width * 2 / 3;

        // set the jframe height and width
        MainFrame.setPreferredSize(new Dimension(width, height));

        first.add(TwitterBox);
        first.add(TwitterText);
        second.add(WeiboButton);
        second.add(WeiboText);
        third.add(WeChatButton);
        third.add(WeChatText);
        fourth.add(CollectButton);
        fourth.add(TranslationButton);
        fourth.add(AnalysisButton);
        fifth.add(ClearButton);

        fourth.setAlignmentY(1000);

        ControlPanel.add(first);
        ControlPanel.add(second);
        ControlPanel.add(third);
        ControlPanel.add(fourth);
        ControlPanel.add(fifth);

        MainPanel.add(scrollPane, BorderLayout.CENTER);
        MainPanel.add(ControlPanel, BorderLayout.WEST);

        MainFrame.add(MainPanel);
        MainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        MainFrame.pack();
        MainFrame.setLocationRelativeTo(null);
        MainFrame.setTitle("Microblog Util");

        // Property
        MainFrame.setVisible(true);
        Output.setEditable(false);

        // Redirect Output
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
            }
        };

        class JTextFieldPrintStream extends PrintStream {

            public JTextFieldPrintStream(OutputStream out) {
                super(out);
            }

            @Override
            public void println(String x) {
                Output.append("[" + sdf.format(new Date()) + "] " + x + '\n');
            }
            
            @Override
            public void print(String x) {
                Output.append("[" + sdf.format(new Date()) + "] " + x);
            }
        };

        JTextFieldPrintStream print = new JTextFieldPrintStream(out);
        System.setOut(print);
        
        
        //Log file for R
    	File log = new File("R.log");
    	try {
    		if(!log.exists()) {
    			log.createNewFile();
    		}
    		rlog = new PrintStream(log);
		} catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        // Event Handler
        CollectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (twittervalue) {
                    try {
                        String file = TwitterText.getText();
                        File f = new File(file);

                        if (f.exists() && !f.isDirectory()) {
                            Process p = Runtime.getRuntime().exec("python src/Twitter.py " + file);
                            StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
                            StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream());
                            errorGobbler.start();
                            outputGobbler.start();
                            //p.waitFor();
                        } else {
                            throw new Exception();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                if (weibovalue) {
                    try {
                        String file = WeiboText.getText();
                        File f = new File(file);

                        if (f.exists() && !f.isDirectory()) {
                            Process p = Runtime.getRuntime().exec("python src/Weibo.py " + file);
                            StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
                            StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream());
                            errorGobbler.start();
                            outputGobbler.start();
                            //p.waitFor();
                        } else {
                            throw new Exception();
                        }
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }

                if (wechatvalue) {
                    new Thread(() -> { try {
                    	
                        String file = WeChatText.getText();
                        File f = new File(file);

                        if (f.exists() && !f.isDirectory()) {
                        	Helper.collectWeChatData(f);
                        } else {
                            throw new Exception();
                        }
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }}).start();
                }
                try {
                	Thread.sleep(500);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        });

        TranslationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Thread(() -> { try {
                	if (weibovalue) {
                		String file = WeiboText.getText();
                		Helper.translateWeiboData(file);
                	}
                	if (wechatvalue) {
                		String file = WeChatText.getText();
                		Helper.translateWeChatData(file);
                	}

                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }}).start();
                try {
                	Thread.sleep(500);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        });

        AnalysisButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try{
                    ArrayList<String> twitter_results = new ArrayList<String>();
                    ArrayList<String> weibo_results = new ArrayList<String>();
                    ArrayList<String> wechat_results = new ArrayList<String>();

                    String filename;
                    String []tmp;
                    
                    File[] files;
                    File dir;
                    dir = new File("./Twitter");
                    if(dir.exists() && dir.isDirectory()) {
                    	files = dir.listFiles();
	
	                    for (File file : files) {
	                        if (file.isFile()) {
	                            filename = file.getName();
	                            tmp = filename.split("\\.");
	                            if(!tmp[0].equals("User_info") && tmp[tmp.length - 1].equals("csv")){
	                                twitter_results.add(tmp[0]);
	                            }
	                        }
	                    }
                    }
                    filename = null;
                    tmp = null;
                    	
                    dir = new File("./WeiboT");
                    if(dir.exists() && dir.isDirectory()) {
                    	files = dir.listFiles();
                        for (File file : files) {
                            if (file.isFile()) {
                                filename = file.getName();
                                tmp = filename.split("BT_");
                                tmp = tmp[1].split("\\.");
                                if(!tmp[0].equals("User_info") && tmp[tmp.length - 1].equals("csv")){
                                    weibo_results.add(tmp[0]);
                                }
                            }
                        }
                    }
                    filename = null;
                    tmp = null;

                    dir = new File("./WeChatT");
                    if(dir.exists() && dir.isDirectory()) {
                    	files = dir.listFiles();
	                    for (File file : files) {
	                        if (file.isFile()) {
	                            filename = file.getName();
	                            tmp = filename.split("CT_");
	                            tmp = tmp[1].split("\\.");
	                            if(tmp[tmp.length - 1].equals("csv")){
	                                wechat_results.add(tmp[0]);
	                            }
	                        }
	                    }
                    }
                    
                    dir = new File("TwitterA");
                    if(!dir.exists()) {dir.mkdir();}
                    dir = new File("WeiboA");
                    if(!dir.exists()) {dir.mkdir();}
                    dir = new File("WeChatA");
                    if(!dir.exists()) {dir.mkdir();}
                    
                    new Thread(()-> { try {	
	                    for(int i = 0; i < twitter_results.size(); i++) {
	                    	System.out.println("Analyzing " + (i+1) + "/" + twitter_results.size() + " twitter accounts...");
	                        String cmd = "Rscript src/twitter.R " + twitter_results.get(i) + " > TwitterA/TA_" + twitter_results.get(i) + ".txt";
	                        Process p =  Runtime.getRuntime().exec(cmd);
	                        StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
	                        StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), rlog);
	                        errorGobbler.start();
	                        outputGobbler.start();
	                        p.waitFor();
	                    }
						if(twitter_results.size() > 0) {
							System.out.println("Twitter analysis complete.");
						}
	                    
	                    for(int i = 0; i < weibo_results.size(); i++) {
	                    	System.out.println("Analyzing " + (i+1) + "/" + weibo_results.size() + " weibo accounts...");
	                        String cmd = "Rscript src/weibo.R " + weibo_results.get(i) + " > WeiboA/BA_" + weibo_results.get(i) + ".txt";
	                        Process p =  Runtime.getRuntime().exec(cmd);
	                        StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
	                        StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), rlog);
	                        errorGobbler.start();
	                        outputGobbler.start();
	                        p.waitFor();
	                    }
						if(weibo_results.size() > 0) {
							System.out.println("Weibo analysis complete.");
						}
	                   
	                    for(int i = 0; i < wechat_results.size(); i++) {
	                    	System.out.println("Analyzing " + (i+1) + "/" + wechat_results.size() + " wechat accounts...");
	                        String cmd = "Rscript src/wechat.R " + wechat_results.get(i) + " > WeChatA/CA_" + wechat_results.get(i) + ".txt";
	                        Process p =  Runtime.getRuntime().exec(cmd);
	                        StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream());
	                        StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), rlog);
	                        errorGobbler.start();
	                        outputGobbler.start();
	                        p.waitFor();
	                    }
						if(wechat_results.size() > 0) {
							System.out.println("WeChat analysis complete.");
						}
                    }catch (Exception ex) {
                        ex.printStackTrace();
                    }}).start();
                    Thread.sleep(500);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        ClearButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Output.setText("");
            }
        });

        TwitterBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                twittervalue = !twittervalue;
            }
        });

        WeiboButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                weibovalue = !weibovalue;
            }
        });

        WeChatButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                wechatvalue = !wechatvalue;
            }
        });
    }
}

class StreamGobbler extends Thread {
    InputStream is;
    PrintStream out;
    
    // reads everything from is until empty. 
    StreamGobbler(InputStream is) {
        this.is = is;
        this.out = System.out;
    }
    
    StreamGobbler(InputStream is, PrintStream out) {
        this.is = is;
        this.out = out;
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
                out.println(line); 
           out.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }
}
