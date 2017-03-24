package main.java.com.pulldocuments;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemOptions;
import org.apache.commons.vfs.provider.sftp.SftpClientFactory;
import org.apache.commons.vfs.provider.sftp.SftpFileSystemConfigBuilder;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SFTPClient{

	private ChannelSftp command;

	private Session session;

	public SFTPClient() {
		command = null;
	}

	public boolean connect(String host, String login, String password, int port) throws JSchException {

		//If the client is already connected, disconnect
		if (command != null) {
			disconnect();
		}
		FileSystemOptions fso = new FileSystemOptions();
		try {
			SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fso, "no");
			session = SftpClientFactory.createConnection(host, port, login.toCharArray(), password.toCharArray(), fso);
			Channel channel = session.openChannel("sftp");
			channel.connect();
			command = (ChannelSftp) channel;

		} catch (FileSystemException e) { 
			e.printStackTrace();
			return false;
		}
		System.out.println(command);
		return command != null;
		//return command.isConnected();
	}

	public void disconnect() {
		if (command != null) {
			command.exit();
		}
		if (session != null) {
			session.disconnect();
		}
		command = null;
	}


	protected boolean uploadFile(InputStream is, String remotePath) throws IOException {
		try {
			command.put(is, remotePath);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (is != null) {
				is.close();
			}
		}
		return true;
	}

	public boolean changeDir(String remotePath) throws Exception {
		try {
			command.cd(remotePath);
		} catch (SftpException e) {
			return false;
		}
		return true;
	}

	public boolean isARemoteDirectory(String path) {
		try {
			return command.stat(path).isDir();
		} catch (SftpException e) {
			//e.printStackTrace();
		}
		return false;
	}

	public String getWorkingDirectory() {
		try {
			return command.pwd();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
