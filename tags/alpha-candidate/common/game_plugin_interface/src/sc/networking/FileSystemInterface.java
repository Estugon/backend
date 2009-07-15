package sc.networking;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileSystemInterface implements INetworkInterface
{
	private final InputStream	inputStream;
	private final OutputStream	outputStream;

	public FileSystemInterface(File file) throws FileNotFoundException
	{
		this.inputStream = new FileInputStream(file);
		this.outputStream = new NullOutputStream(true);
	}

	public FileSystemInterface(InputStream in) throws FileNotFoundException
	{
		this.inputStream = in;
		this.outputStream = new NullOutputStream(true);
	}

	@Override
	public void close() throws IOException
	{
		this.inputStream.close();
		this.outputStream.close();
	}

	@Override
	public InputStream getInputStream() throws IOException
	{
		return this.inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException
	{
		return this.outputStream;
	}
}
