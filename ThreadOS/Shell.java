/**
 * @author !@#$%%$#@!!@#$%%$#@!      PUT YOUR NAME HERE !@#$%%$#@!!@#$%%$#@!!@#$%%$#@!
 *
 * There's a separate folder with the ThreadOS .class files
 *
 */

import java.util.ArrayList;
import java.util.Arrays;

public class Shell extends Thread
{
    // the buffer for the command line input from the user
    private StringBuffer cmdLine;

    // a counter for how many times the shell has run commands
    private int runs;

    public Shell()
    {
        cmdLine = new StringBuffer();
        runs = 1;
    }

    public void run()
    {
        while(true)
        {
            displayPrompt();

            SysLib.cin(cmdLine);

            String[] args = SysLib.stringToArgs(cmdLine.toString());

            cmdLine.setLength(0);

            if(args.length == 0)
                continue;

            if(args[0].equals("exit"))
                break;

            for(int start = 0, end = 0; end < args.length; end++)
            {
                if(args[end].equals(";"))
                {
                    //stuff

                    //double check for length here
                    String[] pArgs = Arrays.copyOfRange(args, start, end - 1);
                    start = end;
                    int pid = SysLib.exec(pArgs);

                    while(pid != SysLib.join());

                }
                else if(args[end].equals("&"))
                {
                    //differenter stuff

                    String[] pArgs = Arrays.copyOfRange(args, start, end - 1);
                    start = end;
                    int pid = SysLib.exec(pArgs);
                }
            }

        }

        SysLib.exit();
    }

    private void displayPrompt()
    {
        SysLib.cout("shell["+runs+"]% ");
        runs++;
    }
}