/*
 * LoginFrame
 *
 * Version 1.0
 *
 * 2026-08-30
 *
 * Copyright (c) 2026 Vcampus Team
 */
package vcampus.client.view;

import vcampus.client.biz.IUserClientSrv;
import vcampus.client.biz.UserClientSrv;
import vcampus.common.constant.IConstant;
import vcampus.common.util.MD5Util;
import vcampus.common.vo.Message;
import vcampus.common.vo.User;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;

/**
 * 客户端登录/注册窗口。用户填写登录ID、密码、角色后，点击"登录"或"注册"
 * 按钮，通过 {@link UserClientSrv} 把请求发给服务器，并根据响应弹窗提示
 * 成功或失败。这是用户管理模块本周要跑通的完整链路的界面入口。
 */
public class LoginFrame extends JFrame {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 登录ID输入框。 */
    private final JTextField _uidField = new JTextField(14);

    /** 密码输入框（星号回显）。 */
    private final JPasswordField _pwdField = new JPasswordField(14);

    /** 角色选择框。 */
    private final JComboBox<String> _roleBox = new JComboBox<>(new String[]{"学生", "管理员"});

    /** 客户端用户业务服务，负责实际的 Socket 通信。 */
    private final IUserClientSrv _userClientSrv = new UserClientSrv();

    /**
     * 构造方法：创建窗口并搭建界面。
     */
    public LoginFrame() {
        super("Vcampus 登录");
        buildUi();
    }

    /**
     * 搭建窗口上的各个控件与布局。
     */
    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("登录ID："), gbc);
        gbc.gridx = 1;
        panel.add(_uidField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("密码："), gbc);
        gbc.gridx = 1;
        panel.add(_pwdField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("角色："), gbc);
        gbc.gridx = 1;
        panel.add(_roleBox, gbc);

        JButton loginButton = new JButton("登录");
        JButton registerButton = new JButton("注册");
        loginButton.addActionListener(e -> onLogin());
        registerButton.addActionListener(e -> onRegister());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        getContentPane().add(panel);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * 从界面收集输入并校验，密码在这里就完成 MD5 摘要（明文密码不会经网络传输）。
     *
     * @return 校验通过时返回封装好的 {@link User}；输入不合法时返回 {@code null}
     */
    private User collectInput() {
        String uid = _uidField.getText().trim();
        String pwd = new String(_pwdField.getPassword());
        String role = (String) _roleBox.getSelectedItem();

        if (uid.isEmpty() || pwd.isEmpty()) {
            JOptionPane.showMessageDialog(this, "登录ID和密码不能为空", "提示", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        User user = new User();
        user.setUId(uid);
        user.setUPwd(MD5Util.md5(pwd));
        user.setURole(role);
        return user;
    }

    /**
     * "登录"按钮的点击处理：发送登录请求并根据响应弹窗提示。
     */
    private void onLogin() {
        User loginUser = collectInput();
        if (loginUser == null) {
            return;
        }
        try {
            Message response = _userClientSrv.login(loginUser);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                User found = (User) response.getData();
                JOptionPane.showMessageDialog(this, "登录成功，欢迎 " + found.getUId() + "！",
                        "登录成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, String.valueOf(response.getData()),
                        "登录失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "无法连接服务器，请确认服务器已启动：" + e.getMessage(),
                    "连接失败", JOptionPane.ERROR_MESSAGE);
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "服务器返回的数据无法识别：" + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * "注册"按钮的点击处理：发送注册请求并根据响应弹窗提示。
     */
    private void onRegister() {
        User newUser = collectInput();
        if (newUser == null) {
            return;
        }
        try {
            Message response = _userClientSrv.register(newUser);
            if (IConstant.STATUS_SUCCESS.equals(response.getStatusCode())) {
                JOptionPane.showMessageDialog(this, String.valueOf(response.getData()),
                        "注册成功", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, String.valueOf(response.getData()),
                        "注册失败", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "无法连接服务器，请确认服务器已启动：" + e.getMessage(),
                    "连接失败", JOptionPane.ERROR_MESSAGE);
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "服务器返回的数据无法识别：" + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 客户端程序入口：在事件分派线程上创建并显示登录窗口。
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
