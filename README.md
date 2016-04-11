WordsCount
===================================
本项目为一个基于Maven 3.3.9  的mvn工程

实现功能
-----------------------------------
* 根据一个英文文档小文件生成大文件；   

* 查询大文件中出现的不同单词；   

* 统计出这些单词出现的次数；   

* 按首字母A-Z顺序输出单词和对应出现次数。

调试环境
-----------------------------------
* JDK 1.8.0_77  

* Eclipse Mars.2 Release (4.5.2)   

* Maven 3.3.9  

代码思路
-----------------------------------
1.生成超过1G大小的英文文件  

2.将大文件分割为多个小文件   

3.产生多个子线程对每个小文件进行单词数目统计  

4.汇总每个子线程中的统计数目   

5.按首字母顺序输出单词和出现次数   

mvn命令行运行
-----------------------------------
项目pom.xml中已设置默认mainclass为com.gh.WordsCount.WordsCount ,cmd运行前需保证工程被compiler过。

在windows的cmd下(将cmd目录更改到工程存放目录)，可直接输入：
```
mvn exec:java
```
运行结果如下(以几个单词的统计为例)： 
![mvn-cmd](https://github.com/IVYGOU/pictures/blob/master/mvn-cmd.png "mvn命令运行结果")  
或者在不设置默认mainclass时输入：
```
mvn exec:java -Dexec.mainClass=com.gh.WordsCount.WordsCount
```
优化过程
-----------------------------------
###version 1.0
####第一个版本如下所示：(WordCount.java)
```Java
package com.test.WordCount;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class WordCount 
{
	private static String file_path = "D:\\test.txt";
	private static HashMap<String, Integer> result = new HashMap<String, Integer>();
	public static void main( String[] args )
	{
    		File file = new File(file_path);
    		String current_line = null;
    		String[] current_words = null;
    		BufferedReader br = null;
    		try 
    		{
    		br = new BufferedReader(new FileReader(file.getPath()));
		while((current_line = br.readLine()) != null)
		{
			current_words = current_line.split("[^a-zA-Z']+");
			for(int i=0; i<current_words.length; i++)
			{		
				if(current_words[i].equals(""))
					continue;			
				if (result.get(current_words[i].toLowerCase()) == null)
	                	{
				    	result.put(current_words[i].toLowerCase(), 1);
	        		}
				else
	                	{
	                	result.put(current_words[i].toLowerCase(), result.get(current_words[i])+1);
	                	}
			}
		}
    	}
    	catch (IOException e) 
    	{
            e.printStackTrace();
        }
    	
    	for (HashMap.Entry<String, Integer> entry : result.entrySet()) 
    	{
    		System.out.println( entry.getKey() + " : " + entry.getValue());
    	}
    	
    	try 
    	{
		br.close();
	} 
    	catch (IOException e) 
    	{
		e.printStackTrace();
	}
    }
}
```
这个算法思路如下：    

1.定义一个BufferedReader对象，读取文本文件内容  

2.逐行读取文本内容，使用spilt()方法分割字符串（使用正则表达式"[^a-zA-Z']+",将除英文字母和'外其他字符设为分隔符）   
 
3.对分割后的字符串数组进行数目统计，将结果存在HashMap中    

4.输出单词和对应个数

测试的结果如下：  

test数据：   

![test](https://github.com/IVYGOU/pictures/blob/master/test.png "test.txt")  

结果：   
![test-jieguo](https://github.com/IVYGOU/pictures/blob/master/test-jieguo.png "test.txt统计结果")  

该算法能对文本中的单词进行准确计数，排除其他符号和大小写干扰，基本实现功能。
但是在大容量文件中执行速度会很慢，于是考虑多线程处理文件的方式。

###version 2.0
在version1.0的基础上添加大文件的产生和多线程处理。
####大文件产生(WordsCount.java)
大于1G的英文文件很难下载到，即使下载到也无法正确知道文档里英文单词的数量，因此考虑自己生成一个大文件，可清楚知道里面各个单词的数量。
```Java
	File file = new File("1.txt");
	FileChannel fileChannel = new RandomAccessFile(file, "rw").getChannel();
	FileLock lock = fileChannel.lock(0, file.length(), false);
	MappedByteBuffer mbBuf = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
	String str = Charset.forName("UTF-8").decode(mbBuf).toString() + "\r\n";
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
```
将1.txt（1389KB）复制1000次可得2.txt（约1.4G），并且明确知道每个单词的出现频率，满足输入数据要求。
![test1 2](https://github.com/IVYGOU/pictures/blob/master/test1%202.png "由1.txt生成的2.txt")  

####分成多个子线程统计每个英文单词出现的次数(DealFileText.java)
```Java
	for (int num = 0; num < threadNum; num++)
	{
	    	if (currentPos < file.length())
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
	    			thread.start();
	    			listCountWordsThreads.add(countWordsThread);
	    			listThread.add(thread);
	    	}
	}
	    	//判断线程是否执行完成
	    	while(true) 
            {
            	boolean threadsDone = true;
            	
            	for (int loop = 0; loop < listThread.size(); loop++)
            	{
            		if (listThread.get(loop).getState() != Thread.State.TERMINATED)
            		{
            			threadsDone = false;
            			break;
            		}
            	}
            	if (true == threadsDone)
            		break;
            }
```
分割文件大小为splitSize，产生threadNum个子线程去统计每一个分割文件中单词出现次数。   

刚开始分割文件时未考虑单词被截断的状况，后面考虑到这种情况加入if(false == Character.isLetter(ch) && '\'' != ch)判断，若分割位置下一个byte是字母或‘，则不在此处分割继续往下找分割位置，防止单词截断。
####子线程处理函数(CountWordsThread.java)

```Java
 //重写run()方法
    @Override
    public void run() 
    {
        String str = Charset.forName("UTF-8").decode(mbBuf).toString();
        str = str.toLowerCase();
        String[] strArray = str.split("[^a-zA-Z']+");
        for(int i = 0; i<strArray.length; i++)
	{		
		if(strArray[i].equals(""))
			continue;
		if (hashMap.get(strArray[i]) == null)
        	{
			hashMap.put(strArray[i], 1);
		}
		else
            	{
		  	hashMap.put(strArray[i], hashMap.get(strArray[i]) + 1);
            	}
	}
    }
``` 

子线程run()方法里对字符串分割并统计单词出现次数，与version1.0里处理方式相同。
####统计总数目线程(DealFileText.java)

```Java
    //当分别统计的线程结束后，开始统计总数目的线程
    	new Thread( () ->
    	{
                // 使用TreeMap保证结果有序（按首字母排序）
                TreeMap<String, Integer> tMap = new TreeMap<String, Integer>();
                for (int loop = 0; loop < listCountWordsThreads.size(); loop++)
                {
                	Map<String, Integer> hMap = listCountWordsThreads.get(loop).getResultMap();
                        Set<String> keys = hMap.keySet();
                        Iterator<String> iterator = keys.iterator();
                        while (iterator.hasNext()) 
                        {
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
                for (int loop = 0; loop < listThread.size(); loop++)
                {
                	listThread.get(loop).interrupt();
                }
                Set<String> keys = tMap.keySet();
                Iterator<String> iterator = keys.iterator();
                while (iterator.hasNext()) 
                {
                	String key = (String) iterator.next();
                        System.out.println("单词:" + key + " 出现次数:" + tMap.get(key));
                }       	
                return;
    	}).start();
```
当所有统计单词子线程的程序执行完成后，开始此线程。将子线程的统计结果存入hashmap中。后来考虑到单词数目很多的情况下不方便查看，所以改为存在TreeMap中，按首字母（A~Z）的顺序输出单词和对应出现次数。   

经2.txt数据输入测试，能正确输出结果。

### version 2.1
version 2.0能对大文件进行处理，但是并未考虑具体电脑内存大小，线程数目，分割文件大小等，在哪一种情况下文件处理时间最短。      

经过不断测试得出：(计算机配置：64位，32g内存)   

1.当线程数超过10个时，程序会运行的非常慢，因为线程调度耗时和资源竞争等原因。   

2.分割文件大小过大会造成内存不够，过小则浪费时间，将分割文件大小设置为1M~10M大小之间。     

所以在大文件处理时对线程大小和分割文件大小做了限制(DealFileText.java)：
```Java
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
    	
```
在限制范围内进行多次试验，得出当线程数在2~4个，分割文件大小为10M时处理速度较快。   

在主类WordsCount的main()中设置：
```
DealFileText dft = new DealFileText(file2, 4, 1024 * 1024 * 10); // 文件，线程数，文件分割大小
```
PS: 此处因为电脑配置原因无法做更多线程，更大分割大小测试，有条件情况下可以做更大范围测试。

### version 2.2
在version2.1中采用显式线程对大文件进行处理，有可能造成系统创建大量线程而导致消耗完系统内存以及"过度切换"等问题，所以考虑使用线程池，它可以有效减少在创建和销毁线程上所花的时间以及系统资源的开销。  
 
除此之外，对字符串的分割也尝试用Java8的新features中的stream和lambda表达式实现。

####线程池的使用(DealFileText.java)   

创建：
```Java
ExecutorService pool = Executors.newFixedThreadPool(threadNum);//创建一个可重用固定线程数的线程池
```
执行：
```Java
Thread thread = new Thread(countWordsThread);
pool.execute(thread);//将线程放入池中进行执行
```
关闭：
```Java
pool.shutdown(); //关闭线程池
//等待关闭线程池，每次等待的超时时间为1秒
while(!pool.isTerminated())
        pool.awaitTermination(1,TimeUnit.SECONDS); 
```
####字符串分割(CountWordsThread.java)：   
```Java
        String str = Charset.forName("UTF-8").decode(mbBuf).toString();
        Stream<String> stream = Stream.of(str.toLowerCase());
        //分割单词并将结果存储在hashmap中
        hashMap=stream.flatMap(s -> Stream.of(s.split("[^a-zA-Z']+")))
        	.filter(word -> word.length() > 0)//保留长度不为 0 的单词
                .collect(Collectors.toMap(s -> s, s -> 1, Integer::sum)); 
```
首先将字符串全变为小写，并用flatMap整理到新的Stream，然后保留长度不为 0 的单词，将单词和出现次数存储于hashmap中。
