package com.gh.WordsCount;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;

//程序主类
public class WordsCount
{
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException, InterruptedException
	{
		File file = new File("1.txt");
	    FileChannel fileChannel = new RandomAccessFile(file, "rw").getChannel();
	    FileLock lock = fileChannel.lock(0, file.length(), false);
	    MappedByteBuffer mbBuf = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
	    String str = Charset.forName("UTF-8").decode(mbBuf).toString() + "\r\n";
		
	    //根据1.txt文件生成2.txt（复制1000次1.txt里的内容）
		File file2 = new File("2.txt");
		if (file2.exists())
		{
			file2.delete();
		}	
        FileOutputStream outputFileStream = new FileOutputStream(file2 ,true);
        for (int i = 0; i < 1000; i++)
        {
        	outputFileStream.write(str.getBytes("UTF-8"));
        }      
        outputFileStream.close();
        lock.release();
        fileChannel.close();

        long start = System.currentTimeMillis();
		DealFileText dft = new DealFileText(file2, 4, 1024 * 1024 * 10); // 文件，线程数，文件分割大小
		dft.doFile();
		long end = System.currentTimeMillis();  
        System.out.println("处理文件花费：" + (end - start) / 1000.0 + "秒"); 
	}
}