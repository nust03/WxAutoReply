package com.thbyg.wxautoreply;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 *  Android Java代码执行adb Shell命令
 *  增加工具类
 */


public class ShellUtils {
    public static final String COMMAND_SU = "su";
    public static final String COMMAND_SH = "sh";
    public static final String COMMAND_EXIT = "exit\n";
    public static final String COMMAND_LINE_END = "\n";

    private ShellUtils() {
        throw new AssertionError();
    }
    /**
     * 查看是否有了root权限
     *
     * @return
     */
    public static boolean checkRootPermission() {
        return execCommand("echo root", true, false).result == 0;
    }
    /**
     * 执行shell命令，默认返回结果
     *
     * @param command
     *            command
     * @param 运行是否需要root权限
     * @return
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(String command, boolean isRoot) {
        return execCommand(new String[] { command }, isRoot, true);
    }
    /**
     * 执行shell命令，默认返回结果
     *
     * @param commands
     *            command list
     * @param 运行是否需要root权限
     * @return
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(List<String> commands,
                                            boolean isRoot) {
        return execCommand(
                commands == null ? null : commands.toArray(new String[] {}),
                isRoot, true);
    }
    /**
     * 执行shell命令，默认返回结果
     *
     * @param commands
     *            command array
     * @param 运行是否需要root权限
     * @return
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(String[] commands, boolean isRoot) {
        return execCommand(commands, isRoot, true);
    }
    /**
     * execute shell command
     *
     * @param command
     *            command
     * @param 运行是否需要root权限
     * @param isNeedResultMsg
     *            whether need result msg
     * @return
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(String command, boolean isRoot,
                                            boolean isNeedResultMsg) {
        return execCommand(new String[] { command }, isRoot, isNeedResultMsg);
    }
    /**
     * execute shell commands
     *
     * @param commands
     *            command list
     * @param 运行是否需要root权限
     * @param 是否需要返回运行结果
     * @return
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(List<String> commands,
                                            boolean isRoot, boolean isNeedResultMsg) {
        return execCommand(
                commands == null ? null : commands.toArray(new String[] {}),
                isRoot, isNeedResultMsg);
    }
    /**
     * execute shell commands
     *
     * @param commands
     *            command array
     * @param 运行是否需要root权限
     * @param 是否需要返回运行结果
     * @return <ul>
     *         <li>if isNeedResultMsg is false, {@link CommandResult#successMsg}
     *         is null and {@link CommandResult#errorMsg} is null.</li>
     *         <li>if {@link CommandResult#result} is -1, there maybe some
     *         excepiton.</li>
     *         </ul>
     */
    public static CommandResult execCommand(String[] commands, boolean isRoot,
                                            boolean isNeedResultMsg) {
        int result = -1;
        if (commands == null || commands.length == 0) {
            return new CommandResult(result, null, null);
        }


        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;
        String enabled_accessibility_services = null;
        String accessibility_enabled = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec(
                    isRoot ? COMMAND_SU : COMMAND_SH);
            os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (command == null) {
                    continue;
                }
                os.write(command.getBytes());
                os.writeBytes(COMMAND_LINE_END);
                os.flush();
            }
            os.close();
            //result = process.waitFor();
            // get command result
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                successResult = new BufferedReader(new InputStreamReader(
                        process.getInputStream()));
                errorResult = new BufferedReader(new InputStreamReader(
                        process.getErrorStream()));
                String s;
                result = 0;
                while ((s = successResult.readLine()) != null) {
                    successMsg.append(s + "\n");

                    if(s.trim().indexOf("enabled_accessibility_services") >= 0){
                        String[] tmp = s.trim().split("\\|");

                        //for(int i =0;i<tmp.length;i++) LogToFile.write(tmp[i]);
                        LogToFile.write("s=" + s.trim() + ",length=" + String.valueOf(tmp.length) + ",tmp=" + tmp[1]);
                        if(tmp[1].equalsIgnoreCase("enabled_accessibility_services")) {
                            if( tmp.length > 2) enabled_accessibility_services = tmp[2];
                            else enabled_accessibility_services = "";
                            result++;
                        }
                    }
                    if(s.trim().indexOf("accessibility_enabled") >= 0){
                        String[] tmp = s.trim().split("\\|");
                        LogToFile.write("s=" + s.trim() + ",length=" + String.valueOf(tmp.length) + ",tmp=" + tmp[1]);
                        if(tmp[1].equalsIgnoreCase("accessibility_enabled")){
                            if( tmp.length > 2) accessibility_enabled = tmp[2];
                            else accessibility_enabled = "0";
                            result++;
                        }
                    }
                }
                while ((s = errorResult.readLine()) != null) {
                    errorMsg.append(s + "\n");
                }
            }
        } catch (IOException e) {
            LogToFile.write("Fail!IOException=" + e.getMessage());
        } catch (Exception e) {
            LogToFile.write("Fail!Exception=" + e.getMessage());
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                LogToFile.write("Fail!IOException=" + e.getMessage());
            }

            if (process != null) {
                process.destroy();
            }
        }
        return new CommandResult(result,
                successMsg == null ? null : successMsg.toString(),
                errorMsg == null ? null : errorMsg.toString(),enabled_accessibility_services,accessibility_enabled
        );
    }
    /**
     * 运行结果
     * <ul>
     * <li>{@link CommandResult#result} means result of command, 0 means normal,
     * else means error, same to excute in linux shell</li>
     * <li>{@link CommandResult#successMsg} means success message of command
     * result</li>
     * <li>{@link CommandResult#errorMsg} means error message of command result</li>
     * </ul>
     *
     * @author <a href="http://www.trinea.cn" target="_blank">Trinea</a>
     *         2013-5-16
     */
    public static class CommandResult {
        /** 运行结果 **/
        public int result;
        /** 运行成功结果 **/
        public String successMsg;
        /** 运行失败结果 **/
        public String errorMsg;
        public String enabled_accessibility_services;
        public String accessibility_enabled;

        public CommandResult(int result) {
            this.result = result;
        }
        public CommandResult(int result, String successMsg, String errorMsg, String enabled_accessibility_services, String accessibility_enabled) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
            this.enabled_accessibility_services = enabled_accessibility_services;
            this.accessibility_enabled = accessibility_enabled;
        }

        public CommandResult(int result, String successMsg, String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }
    }
}