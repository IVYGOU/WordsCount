package com.gh.WordsCount;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//大文件处理类
public class DealFileText 
{
    // 要处理的文件
    private File file = null;
    
    // 线程数
    private int threadNum;
     
    // 线程表
    private Vector<CountWordsThread> listCountWordsThreads = null;
    
    // 文件分割大小
    private long splitSize;
    
    // 当前处理的文件位置
    private long currentPos;
     
    public DealFileText(File file, int threadNum, long splitSize)
    {
       	//确定线程数最小是1个
    	if (threadNum < 1)
    		threadNum = 1;
    	//确定线程数最大是10个，防止内存不够用
    	if (threadNum > 10)
    		threadNum = 10;
    	//分割最小为1M大小文件
    	if (splitSize < 1024*1024)
    		splitSize = 1024*1024;
    	//分割最大为10M大小文件
    	if (splitSize > 1024 * 1024 * 10)
    		splitSize = 1024 * 1024 * 10;
    	
        this.file       = file;
        this.threadNum  = threadNum;
        this.splitSize  = splitSize;
        this.currentPos = 0;
        this.listCountWordsThreads = new Vector<CountWordsThread>();
    }
    
    public void doFile() throws IOException, InterruptedException
    {
    	ExecutorService pool = Executors.newFixedThreadPool(threadNum);//创建一个可重用固定线程数的线程池
        
    	while (currentPos < this.file.length())
    	{
			CountWordsThread countWordsThread = null;
			
			if (currentPos + splitSize < file.length())
			{
				RandomAccessFile raf = new RandomAccessFile(file,"r");
				raf.seek(currentPos + splitSize);
				
				int offset = 0;				
				
				while(true)
				{
					char ch = (char)raf.read();
					
					//是否到文件末尾，到了跳出
					if (-1 == ch)
						break;
					//是否是字母和'，都不是跳出（防止单词被截断）
					if(false == Character.isLetter(ch) && '\'' != ch)
						break;
					
					offset++;
				}				
				
				countWordsThread = new CountWordsThread(file, currentPos, splitSize + offset);
				currentPos += splitSize + offset;
				
				raf.close();
			}
			else
			{
				countWordsThread = new CountWordsThread(file, currentPos, file.length() - currentPos);
				currentPos = file.length();
			}
			
			Thread thread = new Thread(countWordsThread);
			pool.execute(thread);//将线程放入池中进行执行
			listCountWordsThreads.add(countWordsThread);
    	}
    	
    	pool.shutdown(); //关闭线程池
    	
        //等待关闭线程池，每次等待的超时时间为1秒
        while(!pool.isTerminated())
        	pool.awaitTermination(1,TimeUnit.SECONDS); 
        
       // 开始总统计数目，并使用TreeMap保证输出结果有序（按首字母排序）
        TreeMap<String, Integer> tMap = new TreeMap<String, Integer>();
    	
    	for (int loop = 0; loop < listCountWordsThreads.size(); loop++)
    	{
    		Map<String, Integer> hMap = listCountWordsThreads.get(loop).getResultMap();
            
            Set<String> keys = hMap.keySet();
            Iterator<String> iterator = keys.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                
                if(key.equals(""))
					continue;			
			    if (tMap.get(key) == null)
                {
			    	tMap.put(key, hMap.get(key));
                }
			    else
                {
			    	tMap.put(key, tMap.get(key) + hMap.get(key));
                }
            }
    	}
    	
    	Set<String> keys = tMap.keySet();
    	Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            System.out.println("单词:" + key + " 出现次数:" + tMap.get(key));
        }
    }
}